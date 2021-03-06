package com.apps.xtrange.easyshare;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by Oscar on 9/1/2015.
 */
public class SimpleFileSender extends Service {

    public static final String EXTRA_PORT_USED = "exta-port-used";

    private static final String TAG = SimpleFileSender.class.getSimpleName();
    private static final int ALLOWED_RETRIES = 5;
    private int mSocketPort;
    private Uri mFileUri;

    private static final int PORT_START_RANGE = 49000;
    private static final int PORT_END_RANGE = 65000;

    private static final int BUFFER_SIZE = 65536;

    private LocalBinder mBinder = new LocalBinder();

    private Intent mIntent;

    private Thread mThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        this.mIntent = intent;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mThread != null) {
            mThread.interrupt();
        }
        return super.onUnbind(intent);
    }

    public void startFileSender() {
        final Context context = this;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mFileUri = (Uri) mIntent.
                        getParcelableExtra(Constants.EXTRA_FILE_URI);

                if (mFileUri == null) {
                    Util.LogDebug(TAG, "Uri is null");
                }

                InputStream fis = null;
                BufferedInputStream bufferedInputStream = null;
                DataOutputStream outputStream = null;
                ServerSocket serverSocket = null;
                Socket socket = null;

                try {
                    try {
                        mSocketPort = new Random().nextInt(PORT_END_RANGE - PORT_START_RANGE) + PORT_START_RANGE;
                        serverSocket = new ServerSocket(mSocketPort);
                    } catch (IOException e) {
                        //The port was used
                        for (int i = 0; i < ALLOWED_RETRIES; i++) {
                            mSocketPort = new Random().nextInt(PORT_END_RANGE - PORT_START_RANGE) + PORT_START_RANGE;
                            serverSocket = new ServerSocket(mSocketPort);
                        }
                    }

                    Intent startedIntent = new Intent(Constants.BROADCAST_SERVICE_STARTED);
                    startedIntent.putExtra(EXTRA_PORT_USED, mSocketPort);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(startedIntent);

                    byte[] bytes = new byte[BUFFER_SIZE];
                    while (true) {
                        Util.LogDebug(TAG, "Server waiting for connection...");
                        try {
                            socket = serverSocket.accept();
                            Util.LogDebug(TAG, "Accepted a connection");

                            Intent connectedIntent = new Intent(Constants.BROADCAST_CLIENT_CONNECTED);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(connectedIntent);

                            outputStream = new DataOutputStream(socket.getOutputStream());
                            //TODO: Support for multiple files
                            fis = getBaseContext().getContentResolver().openInputStream(mFileUri);
                            bufferedInputStream = new BufferedInputStream(fis);

                            int read = bufferedInputStream.read(bytes, 0, bytes.length);
                            int allRead = 0;
                            while (read != -1) {
                                allRead += read;
                                outputStream.write(bytes, 0, read);
                                read = bufferedInputStream.read(bytes, 0, bytes.length);
                            }

                            outputStream.flush();
                            Util.LogDebug(TAG, "Sent " + allRead + " bytes");

                        } finally {
                            if (bufferedInputStream != null) bufferedInputStream.close();
                            if (outputStream != null) outputStream.close();
                            if (socket != null) socket.close();
                            if (fis != null) fis.close();
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) try {
                        socket.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }

                    Intent startedIntent = new Intent(Constants.BROADCAST_SERVICE_FINISHED);
                    LocalBroadcastManager.getInstance(SimpleFileSender.this).sendBroadcast(startedIntent);
                }
            }
        });
        mThread.start();
    }


    public class LocalBinder extends Binder {
        public SimpleFileSender getLocalService () {
            return SimpleFileSender.this;
        }
    }
}

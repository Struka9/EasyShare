package com.apps.xtrange.easyshare;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
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
public class SimpleFileSender extends IntentService {

    public static final String EXTRA_URI = "extra-uri";
    public static final String EXTRA_PORT_USED = "exta-port-used";

    private static final String TAG = SimpleFileSender.class.getSimpleName();
    private static final int ALLOWED_RETRIES = 5;
    private int mSocketPort;
    private Uri mFileUri;

    private static final int PORT_START_RANGE = 49000;
    private static final int PORT_END_RANGE = 65000;

    private static final int BUFFER_SIZE = 65536;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SimpleFileSender(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mFileUri = intent.getParcelableExtra(EXTRA_URI);

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
            LocalBroadcastManager.getInstance(this).sendBroadcast(startedIntent);

            byte[] bytes = new byte[BUFFER_SIZE];
            while (true) {
                Util.LogDebug(TAG, "Server waiting for connection...");
                try {
                    socket = serverSocket.accept();
                    Util.LogDebug(TAG, "Accepted a connection");

                    Intent connectedIntent = new Intent(Constants.BROADCAST_CLIENT_CONNECTED);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);

                    outputStream = new DataOutputStream(socket.getOutputStream());
                    //TODO: Support for multiple files
                    fis = getBaseContext().getContentResolver().openInputStream(mFileUri);
                    bufferedInputStream = new BufferedInputStream(fis);

                    int read = bufferedInputStream.read(bytes, 0, bytes.length);

                    while (read != -1) {
                        outputStream.write(bytes, 0, read);
                        read = bufferedInputStream.read(bytes, 0, bytes.length);
                    }

                    outputStream.flush();
                    Util.LogDebug(TAG, "Done.");

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
            LocalBroadcastManager.getInstance(this).sendBroadcast(startedIntent);
        }
    }
}

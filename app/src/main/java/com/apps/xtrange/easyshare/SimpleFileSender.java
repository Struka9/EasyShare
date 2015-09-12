package com.apps.xtrange.easyshare;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

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
public class SimpleFileSender {
    private static final String TAG = SimpleFileSender.class.getSimpleName();
    private static final int ALLOWED_RETRIES = 5;
    private int mSocketPort;
    private Uri mFileUri;
    private Context mContext;
    private SenderEventsListener mListener;
    private Thread mServerThread;

    private static final int PORT_START_RANGE = 49000;
    private static final int PORT_END_RANGE = 65000;

    private static final int BUFFER_SIZE = 4096;

    public SimpleFileSender(Context context, Uri fileUri, SenderEventsListener listener) {
        mFileUri = fileUri;
        mContext = context;
        mListener = listener;
    }

    public void stopServer() {
        if (mServerThread != null) {
            mServerThread.interrupt();
            mServerThread = null;
        }
    }

    public void startServer() {
        mServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
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

                    if (mListener != null) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onServerStarted();
                            }
                        });
                    }
                    byte[] bytes = new byte[BUFFER_SIZE];
                    while (true) {
                        Util.LogDebug(TAG, "Server waiting for connection...");
                        try {
                            socket = serverSocket.accept();
                            Util.LogDebug(TAG, "Accepted a connection");

                            if (mListener != null) {
                                new Handler(mContext.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mListener.onClientConnected();
                                    }
                                });
                            }
                            outputStream = new DataOutputStream(socket.getOutputStream());
                            //TODO: Support for multiple files
                            fis = mContext.getContentResolver().openInputStream(mFileUri);
                            bufferedInputStream = new BufferedInputStream(fis);

                            int read = bufferedInputStream.read(bytes, 0, bytes.length);

                            while (read != -1) {
                                Util.LogDebug(TAG, "Sending " + mFileUri + "(" + bytes.length + " bytes)");
                                outputStream.write(bytes, 0, bytes.length);
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
                    if (mListener != null) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onServerError(e);
                            }
                        });
                    }

                } finally {
                    if (socket != null) try {
                        socket.close();
                    } catch (final IOException e) {
                        if (mListener != null) {
                            new Handler(mContext.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onServerError(e);
                                }
                            });
                        }
                    }
                }

                if (mListener != null) {
                    new Handler(mContext.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onCompleted();
                        }
                    });
                }
            }
        });

        mServerThread.start();
    }

    public int getPortUsed() {
        return mSocketPort;
    }

    public interface SenderEventsListener {
        public void onServerStarted();

        public void onCompleted();

        public void onClientConnected();

        public void onServerError(Exception e);
    }
}

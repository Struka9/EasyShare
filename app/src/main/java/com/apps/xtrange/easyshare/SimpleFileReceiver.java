package com.apps.xtrange.easyshare;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Oscar on 9/1/2015.
 */
public class SimpleFileReceiver {

    private static final String TAG = SimpleFileReceiver.class.getSimpleName();
    private int mPort;
    private String mIpAddress;
    private String mFileName;
    private ReceiverEventsListener mListener;

    public final static int BUFFER_SIZE = 6022386;

    private Context mContext;

    public SimpleFileReceiver(Context context, int port, String ipAddress, String fileName, ReceiverEventsListener listener) {
        mIpAddress = ipAddress;
        mPort = port;
        mContext = context;
        mFileName = fileName;
        mListener = listener;
    }

    public SimpleFileReceiver(Context context, int port, String ipAddress, String fileName) {
        this(context, port, ipAddress, fileName, null);
    }

    public void receiveFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File externalFilesDir = Environment.getExternalStorageDirectory();
                int bytesRead;
                int current = 0;
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                Socket sock = null;
                try {
                    Util.LogDebug(TAG, "Connecting...");
                    sock = new Socket(mIpAddress, mPort);

                    if (mListener != null) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onConnected();
                            }
                        });
                    }

                    // receive file
                    byte[] bytes = new byte[BUFFER_SIZE];
                    InputStream is = sock.getInputStream();

                    File outputFile = new File(externalFilesDir, mFileName);

                    //outputFile.mkdirs();

                    fos = new FileOutputStream(outputFile);
                    bos = new BufferedOutputStream(fos);
                    bytesRead = is.read(bytes, 0, bytes.length);
                    current = bytesRead;

                    while (bytesRead > -1) {

                        if (bytesRead >= 0) {
                            bos.write(bytes, 0, bytesRead);
                            current += bytesRead;
                        }

                        bytesRead =
                                is.read(bytes, 0, bytes.length);
                    }

                    //bos.write(bytes, 0, current);
                    bos.flush();
                    Util.LogDebug(TAG, "File " + outputFile.getName()
                            + " downloaded (" + current + " bytes read)");

                    if (mListener != null) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onConnected();
                            }
                        });
                    }
                } catch (final Exception e) {
                    if (mListener != null) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onError(e);
                            }
                        });
                    }
                } finally {
                    if (fos != null)
                        try {
                            fos.close();
                            if (bos != null) bos.close();
                            if (sock != null) sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
                if (mListener != null) {
                    new Handler(mContext.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onFinished(mFileName);
                        }
                    });
                }

            }
        }).start();
    }

    public interface ReceiverEventsListener {
        public void onConnected();
        public void onError(Exception e);
        public void onFinished(String fileName);
    }
}

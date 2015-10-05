package com.apps.xtrange.easyshare;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by Oscar on 9/1/2015.
 */
public class SimpleFileReceiver extends IntentService {


    private static final String TAG = SimpleFileReceiver.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0x01337;

    private int mPort;
    private String mIpAddress;
    private String mFileName;

    public final static int BUFFER_SIZE = 65536;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SimpleFileReceiver(String name) {
        super(name);
    }

    public SimpleFileReceiver() {
        super(SimpleFileReceiver.class.getSimpleName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        mIpAddress = intent.getStringExtra(Constants.EXTRA_IP_ADDRESS);
        mPort = intent.getIntExtra(SimpleFileSender.EXTRA_PORT_USED, -1);
        mFileName = intent.getStringExtra(Constants.EXTRA_FILENAME);

        File externalFilesDir = Environment.getExternalStorageDirectory();
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        try {
            Util.LogDebug(TAG, "Connecting...");
            sock = new Socket(mIpAddress, mPort);

            //The service connected to the sender service

            // receive file
            byte[] bytes = new byte[BUFFER_SIZE];
            InputStream is = sock.getInputStream();

            final File outputFile = new File(externalFilesDir, mFileName);

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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.success))
                    .setOngoing(false);

            Uri uri = Uri.fromFile(outputFile);

            Util.LogDebug(TAG, uri.toString());

            MimeTypeMap map = MimeTypeMap.getSingleton();
            String ext = getContentResolver().getType(uri);
            String type = map.getMimeTypeFromExtension(ext);

            if (type == null)
                type = "*/*";

            Intent resultIntent = new Intent(getApplicationContext(), ShareFilesActivity.class);
            resultIntent.setDataAndType(uri, type);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
            builder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());


            Intent broadCast = new Intent();
            broadCast.setAction(Constants.BROADCAST_SERVICE_FINISHED);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(broadCast);

        } catch (final Exception e) {
            e.printStackTrace();
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
    }
}

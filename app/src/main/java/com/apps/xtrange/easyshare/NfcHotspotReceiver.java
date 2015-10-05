package com.apps.xtrange.easyshare;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

/**
 * Created by oscarr on 10/5/15.
 */
public class NfcHotspotReceiver extends AppCompatActivity {
    private static final String TAG = NfcHotspotReceiver.class.getSimpleName();

    private BarcodeDetector mBarcodeDetector;

    // Incoming Intent
    private Intent mIntent;
    /*
     * Called from onNewIntent() for a SINGLE_TOP Activity
     * or onCreate() for a new Activity. For onNewIntent(),
     * remember to call setIntent() to store the most
     * current Intent
     *
     */
    private void handleViewIntent() {

        if (!mBarcodeDetector.isOperational()) {
            Toast.makeText(this, R.string.could_not_setup_detector, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get the Intent action
        mIntent = getIntent();
        String action = mIntent.getAction();
        /*
         * For ACTION_VIEW, the Activity is being asked to display data.
         * Get the URI.
         */
        if (TextUtils.equals(action, Intent.ACTION_VIEW)) {
            // Get the URI from the Intent
            Uri beamUri = mIntent.getData();
            /*
             * Test for the type of URI, by getting its scheme value
             */
            /*if (TextUtils.equals(beamUri.getScheme(), "file")) {
                mParentPath = handleFileUri(beamUri);
            } else*/ if (TextUtils.equals(
                    beamUri.getScheme(), "content")) {
                String filePath = handleContentUri(beamUri);

                if (filePath != null) {
                    Util.LogDebug(TAG, "Creating bitmap from file");
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                    Frame frame = new Frame.Builder()
                            .setBitmap(bitmap).build();

                    SparseArray<Barcode> barcodes = mBarcodeDetector.detect(frame);

                    if (barcodes.size() == 0) {
                        Toast.makeText(this, R.string.could_not_detect_barcode, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Barcode barcode = barcodes.get(0);

                    Matcher matcher = Constants.SHARE_HOTSPOT_PATTERN.matcher(barcode.rawValue);

                    if (!matcher.matches()) {
                        Toast.makeText(this, "It's not a valid Easy Share code.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //values[0] = ssid, values[1] = encryption, values[2] = password (ssid and password are encoded)
                    String[] values = barcode.rawValue.split(":");
                    try {
                        String ssid = URLDecoder.decode(values[0], "utf-8");
                        String encrytpion = values[1];
                        String password = URLDecoder.decode(values[2], "utf-8");

                        Util.LogDebug(TAG, "SSID: " + ssid + " and password: " + password + " with encryption: " + encrytpion);

                        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

                        new WifiConfigManager(wifiManager, null).execute(ssid, password, encrytpion);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        finish();
    }

    public String handleFileUri(Uri beamUri) {
        // Get the path part of the URI
        String fileName = beamUri.getPath();
        // Create a File object for this filename
        File copiedFile = new File(fileName);
        // Get a string containing the file's parent directory
        return copiedFile.getParent();
    }

    public String handleContentUri(Uri beamUri) {
        // Position of the filename in the query Cursor
        int filenameIndex;
        // The filename stored in MediaStore
        String fileName;
        // Test the authority of the URI
        if (!TextUtils.equals(beamUri.getAuthority(), MediaStore.AUTHORITY)) {
            /*
             * Handle content URIs for other content providers
             */
            // For a MediaStore content URI
        } else {
            // Get the column that contains the file name
            String[] projection = { MediaStore.MediaColumns.DATA };
            Cursor pathCursor =
                    getContentResolver().query(beamUri, projection,
                            null, null, null);
            // Check for a valid cursor
            if (pathCursor != null &&
                    pathCursor.moveToFirst()) {
                // Get the column index in the Cursor
                filenameIndex = pathCursor.getColumnIndex(
                        MediaStore.MediaColumns.DATA);
                // Get the full file name including path
                fileName = pathCursor.getString(filenameIndex);
                return fileName;
            } else {
                // The query didn't work; return null
                return null;
            }
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBarcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX|Barcode.QR_CODE)
                .build();
    }
}

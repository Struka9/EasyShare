package com.apps.xtrange.easyshare;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;

import com.google.zxing.common.BitMatrix;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

/**
 * Created by Oscar on 9/1/2015.
 */
public class Util {
    private static final boolean DEBUG = true;

    public static void LogDebug(String tag, String message) {
        if (DEBUG)
            Log.d(tag, message);
    }

    public static void LogError(String tag, String message) {
        if (DEBUG)
            Log.e(tag, message);
    }

    // And to convert the image URI to the direct file system path of the image file
    public static String getRealPathFromURI(Context context, Uri contentUri) {

        // can post image
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri,
                proj, // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    /**
     * Writes the given Matrix on a new Bitmap object.
     * @param matrix the matrix to write.
     * @return the new {@link Bitmap}-object.
     */
    public static Bitmap toBitmap(BitMatrix matrix){
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                bmp.setPixel(x, y, matrix.get(x,y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Converts the provided dp to pixels based on the device configuration */
    public static float dpToPixels(int dp, Context context) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }

    /** Returns the default NFC Adapter instance or null if it's not available*/
    public static NfcAdapter getNfcAdapter(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return null;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return null;
        } else {
            return NfcAdapter.getDefaultAdapter(context);
        }
    }

    public static String getEncryption(int encryptionType) {
        if (encryptionType == 0) {
            return Constants.ENCRYPTION_OPEN;
        } else if (encryptionType == 1) {
            return Constants.ENCRYPTION_WEP;
        } else {
            return Constants.ENCRYPTION_WPA;
        }
    }

    public static String generateCodeForShareHotspot(String ssid, String password, String encryption) throws UnsupportedEncodingException {
        return URLEncoder.encode(ssid, "utf-8") + ":" +
                encryption + ":" +
                URLEncoder.encode(password, "utf-8") + ":" +
                String.valueOf(Constants.MAGIC_NUMBER);
    }
}

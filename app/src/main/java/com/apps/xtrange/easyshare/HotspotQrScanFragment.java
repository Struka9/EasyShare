package com.apps.xtrange.easyshare;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.apps.xtrange.easyshare.vision.BarcodeTrackerFactory;
import com.apps.xtrange.easyshare.vision.ui.camera.CameraSourcePreview;
import com.apps.xtrange.easyshare.vision.ui.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

/**
 * Created by oscarr on 10/4/15.
 */
public class HotspotQrScanFragment extends Fragment {
    private static final String TAG = HotspotQrScanFragment.class.getSimpleName();
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private boolean mLocked;
    private WifiConfigManager.OnNetworkUpdateListener mNetworkUpdatedListener = new WifiConfigManager.OnNetworkUpdateListener() {
        @Override
        public void onNetworkUpdated() {
            mLocked = false;
        }
    };

    private BarcodeTrackerFactory.OnBarcodeReceivedListener mOnBarcodeReceivedListener = new BarcodeTrackerFactory.OnBarcodeReceivedListener() {
        @Override
        public void onBarcodeReceived(final Barcode barcode) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mLocked) return;

                    Matcher matcher = Constants.SHARE_HOTSPOT_PATTERN.matcher(barcode.rawValue);

                    if (!matcher.matches()) {
                        Toast.makeText(getActivity(), "It's not a valid Easy Share code.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mLocked = true;
                    //values[0] = ssid, values[1] = encryption, values[2] = password (ssid and password are encoded)
                    String[] values = barcode.rawValue.split(":");
                    try {
                        String ssid = URLDecoder.decode(values[0], "utf-8");
                        String encrytpion = values[1];
                        String password = URLDecoder.decode(values[2], "utf-8");

                        Util.LogDebug(TAG, "SSID: " + ssid + " and password: " + password + " with encryption: " + encrytpion);

                        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);

                        new WifiConfigManager(wifiManager, mNetworkUpdatedListener).execute(ssid, password, encrytpion);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.qr_code_reader_layout, container, false);

        mPreview = (CameraSourcePreview) layout.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) layout.findViewById(R.id.faceOverlay);

        createCameraSource();

        return layout;
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        Context context = getActivity().getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, mOnBarcodeReceivedListener);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        // A multi-detector groups the two detectors together as one detector.  All images received
        // by this detector from the camera will be sent to each of the underlying detectors, which
        // will each do face and barcode detection, respectively.  The detection results from each
        // are then sent to associated tracker instances which maintain per-item graphics on the
        // screen.
        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(barcodeDetector)
                .build();

        if (!multiDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(getActivity().getApplicationContext(), multiDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
                .build();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getActivity().getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(),
                            code,
                            Constants.RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }
}

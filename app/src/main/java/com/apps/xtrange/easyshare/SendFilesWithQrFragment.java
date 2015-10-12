package com.apps.xtrange.easyshare;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Created by Oscar on 9/12/2015.
 */
public class SendFilesWithQrFragment extends Fragment {

    public static Fragment newInstance(Uri fileUri) {
        Fragment f = new SendFilesWithQrFragment();

        Bundle args = new Bundle();
        args.putParcelable(Constants.EXTRA_FILE_URI, fileUri);

        f.setArguments(args);

        return f;
    }

    private boolean mBound = false;
    private SimpleFileSender mService;

    private static final String TAG = SendFilesWithQrFragment.class.getSimpleName();
    private ImageView mQrCodeImage;

    private Uri mFileUri;

    private FileSenderBroadcastReceiver mReceiver = new FileSenderBroadcastReceiver();

    private Bitmap mQrCodeBitmap;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SimpleFileSender.LocalBinder binder = (SimpleFileSender.LocalBinder)iBinder;
            mService = binder.getLocalService();
            mService.startFileSender();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_CLIENT_CONNECTED);
        intentFilter.addAction(Constants.BROADCAST_SERVICE_STARTED);
        intentFilter.addAction(Constants.BROADCAST_SERVICE_STARTED);

        Bundle args = getArguments();
        mFileUri = args.getParcelable(Constants.EXTRA_FILE_URI);

        Util.LogDebug(TAG, mFileUri.toString());

        LocalBroadcastManager.getInstance(activity).registerReceiver(mReceiver, intentFilter);

        if (!mBound)
            startSendFilesServer();
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);

        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }

        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.send_with_qr_layout, container, false);
        mQrCodeImage = (ImageView) layout.findViewById(R.id.qr_code);

        if (mQrCodeBitmap != null) {
            mQrCodeImage.setImageBitmap(mQrCodeBitmap);
        }

        return layout;
    }

    private void startSendFilesServer() {
        Intent senderServiceIntent = new Intent(getActivity(), SimpleFileSender.class);
        senderServiceIntent.putExtra(Constants.EXTRA_FILE_URI, mFileUri);
        getActivity().bindService(senderServiceIntent, mConnection, Service.BIND_AUTO_CREATE);

    }

    private void generateQrCode(int serverPort) {
        String mimeType = getActivity().getContentResolver().getType(mFileUri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        String ipAddress = Util.getIPAddress(true);

        Util.LogDebug(TAG, ipAddress);
        String barcodeContents = ipAddress + ":"
                + String.valueOf(serverPort)
                + ":" + String.valueOf(Constants.MAGIC_NUMBER)
                + ":" + extension;

        Util.LogDebug(TAG, barcodeContents);

        if (ipAddress == null || ipAddress.isEmpty()) {
            Toast.makeText(getActivity(), R.string.make_sure_same_network, Toast.LENGTH_LONG).show();
            return;
        }

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(barcodeContents, BarcodeFormat.QR_CODE,
                    (int) Util.dpToPixels(400, getActivity()),
                    (int) Util.dpToPixels(400, getActivity()));

            mQrCodeBitmap = Util.toBitmap(matrix);

            if (mQrCodeImage != null)
                mQrCodeImage.setImageBitmap(mQrCodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private class FileSenderBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case Constants.BROADCAST_SERVICE_STARTED:
                    int portUsed = intent.getIntExtra(SimpleFileSender.EXTRA_PORT_USED, -1);

                    if (portUsed == -1) {
                        Util.LogError(TAG, "Something went wrong, the port is -1");
                    }
                    generateQrCode(portUsed);
                    break;
                case Constants.BROADCAST_CLIENT_CONNECTED:
                    break;
                case Constants.BROADCAST_SERVICE_FINISHED:
                    Toast.makeText(getActivity(), R.string.file_transfer_complete, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                    break;
            }
        }
    }
}

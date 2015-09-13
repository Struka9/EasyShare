package com.apps.xtrange.easyshare;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
public class SendWithQrFragment extends Fragment implements SimpleFileSender.SenderEventsListener {

    public static Fragment newInstance(Uri fileUri) {
        Fragment f = new SendWithQrFragment();

        Bundle args = new Bundle();
        args.putParcelable(SendActivity.EXTRA_FILE_URI, fileUri);

        f.setArguments(args);

        return f;
    }

    private static final String TAG = SendWithQrFragment.class.getSimpleName();
    private ImageView mQrCodeImage;
    private SimpleFileSender mFileSenderServer;

    private Uri mFileUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mFileUri = args.getParcelable(SendActivity.EXTRA_FILE_URI);

        Util.LogDebug(TAG, mFileUri.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.send_with_qr_layout, container, false);
        mQrCodeImage = (ImageView) layout.findViewById(R.id.qr_code);

        startSendFilesServer();

        generateQrCode();

        return layout;
    }

    private void startSendFilesServer() {

        if (mFileSenderServer != null)
            mFileSenderServer.stopServer();

        mFileSenderServer = new SimpleFileSender(getActivity(), mFileUri, this);
        mFileSenderServer.startServer();
    }

    private void generateQrCode() {
        String mimeType = getActivity().getContentResolver().getType(mFileUri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        String ipAddress = Util.getIPAddress(true);
        int serverPort = mFileSenderServer.getPortUsed();

        Util.LogDebug(TAG, ipAddress);
        String barcodeContents = ipAddress + ":"
                + String.valueOf(serverPort)
                + ":" + String.valueOf(Constants.MAGIC_NUMBER)
                + ":" + extension;

        if (ipAddress == null || ipAddress.isEmpty()) {
            Toast.makeText(getActivity(), R.string.make_sure_same_network, Toast.LENGTH_LONG).show();
            return;
        }

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(barcodeContents, BarcodeFormat.QR_CODE,
                    (int) Util.dpToPixels(400, getActivity()),
                    (int) Util.dpToPixels(400, getActivity()));

            Bitmap qrCode = Util.toBitmap(matrix);
            mQrCodeImage.setImageBitmap(qrCode);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFileSenderServer != null)
            mFileSenderServer.stopServer();
    }

    @Override
    public void onServerStarted() {
        generateQrCode();
    }

    @Override
    public void onCompleted() {
        Toast.makeText(getActivity(), R.string.file_transfer_complete, Toast.LENGTH_LONG).show();
        getActivity().finish();
    }

    @Override
    public void onClientConnected() {
    }

    @Override
    public void onServerError(Exception e) {
        e.printStackTrace();
    }
}

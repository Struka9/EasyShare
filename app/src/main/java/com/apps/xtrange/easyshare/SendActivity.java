package com.apps.xtrange.easyshare;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Created by Oscar on 9/10/2015.
 */
public class SendActivity extends FragmentActivity implements SimpleFileSender.SenderEventsListener {
    public static final String EXTRA_FILE_URI = "extra-file-uri";
    private static final String TAG = SendActivity.class.getSimpleName();

    private SimpleFileSender mFileSenderServer;
    private Uri mFileUri;
    private ImageView mQrCodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_activity);
        mFileUri = getIntent().getParcelableExtra(EXTRA_FILE_URI);

        if (mFileUri == null) {
            Log.d(TAG, "Make sure to include the uri in the intent");
            finish();
        }

        mQrCodeImage = (ImageView) findViewById(R.id.qr_code);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        startSendFilesServer();
    }

    private void startSendFilesServer() {

        mFileSenderServer = new SimpleFileSender(this, mFileUri, this);
        mFileSenderServer.startServer();
    }

    private void generateQrCode() {
        String mimeType = getContentResolver().getType(mFileUri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        String ipAddress = Util.getIPAddress(true);
        int serverPort = mFileSenderServer.getPortUsed();

        Util.LogDebug(TAG, ipAddress);
        String barcodeContents = ipAddress + ":"
                + String.valueOf(serverPort)
                + ":" + String.valueOf(Constants.MAGIC_NUMBER)
                + ":" + extension;

        if (ipAddress == null || ipAddress.isEmpty()) {
            Toast.makeText(this, R.string.make_sure_same_network, Toast.LENGTH_LONG).show();
            return;
        }

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(barcodeContents, BarcodeFormat.QR_CODE,
                    (int) Util.dpToPixels(400, this),
                    (int) Util.dpToPixels(400, this));

            Bitmap qrCode = Util.toBitmap(matrix);
            mQrCodeImage.setImageBitmap(qrCode);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
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
        Toast.makeText(this, R.string.file_transfer_complete, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onClientConnected() {
    }

    @Override
    public void onServerError(Exception e) {
        e.printStackTrace();
    }
}

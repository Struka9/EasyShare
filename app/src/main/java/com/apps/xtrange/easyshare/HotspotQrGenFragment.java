package com.apps.xtrange.easyshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by oscarr on 10/3/15.
 */
public class HotspotQrGenFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = HotspotQrGenFragment.class.getSimpleName();
    private Button mGenerateBt;
    private Button mShareThroughNfc;
    private EditText mSsidEt;
    private EditText mPasswordEt;
    private Spinner mSecurityTypeSpinner;
    private ImageView mQrCodeImage;

    private Bitmap mQrCodeBitmap;
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = Util.getNfcAdapter(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.hotspot_qr_gen_layout, container, false);

        mGenerateBt = (Button)layout.findViewById(R.id.generate_qr_bt);
        mGenerateBt.setOnClickListener(this);

        mShareThroughNfc = (Button)layout.findViewById(R.id.send_through_nfc);
        mShareThroughNfc.setOnClickListener(this);

        mQrCodeImage = (ImageView)layout.findViewById(R.id.hotspot_share_qr_img);
        mSsidEt = (EditText)layout.findViewById(R.id.ssid_et);
        mPasswordEt = (EditText)layout.findViewById(R.id.password_et);
        mSecurityTypeSpinner = (Spinner)layout.findViewById(R.id.security_options_sp);
        mSecurityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = (String) mSecurityTypeSpinner.getSelectedItem();
                if (item.compareTo("Open") == 0) {
                    mPasswordEt.setVisibility(View.GONE);
                } else {
                    mPasswordEt.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        return layout;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.generate_qr_bt:

                String ssdid = mSsidEt.getText().toString().trim();

                if (ssdid.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.empty_ssid, Toast.LENGTH_SHORT).show();
                    return;
                }

                String password = mPasswordEt.getText().toString();
                int encryptionType = mSecurityTypeSpinner.getSelectedItemPosition();
                String encryption;
                if (encryptionType == 0) {
                    encryption = Constants.ENCRYPTION_OPEN;
                } else if (encryptionType == 1) {
                    encryption = Constants.ENCRYPTION_WEP;
                } else {
                    encryption = Constants.ENCRYPTION_WPA;
                }

                if (encryption.compareTo(Constants.ENCRYPTION_OPEN) != 0 && password.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.missing_password, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    generateQrImage(ssdid, encryption, password);
                    mShareThroughNfc.setEnabled(true);
                } catch (UnsupportedEncodingException e) {
                    Util.LogError(TAG, e.getMessage());
                }

                break;

            case R.id.send_through_nfc:
                Uri bitmapFileUri = saveQrCodeToExternalStorage();

                if (bitmapFileUri != null) {
                    mNfcAdapter.setBeamPushUris(new Uri[]{bitmapFileUri}, getActivity());
                    Toast.makeText(getActivity(), R.string.make_sure_devices_touch, Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    private void generateQrImage(String ssid, String encryption, String password) throws UnsupportedEncodingException {
        String barcodeContents = URLEncoder.encode(ssid, "utf-8") + ":" +
                encryption + ":" +
                URLEncoder.encode(password, "utf-8") + ":" +
                String.valueOf(Constants.MAGIC_NUMBER);

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(barcodeContents, BarcodeFormat.QR_CODE,
                    (int) Util.dpToPixels(200, getActivity()),
                    (int) Util.dpToPixels(200, getActivity()));

            mQrCodeBitmap = Util.toBitmap(matrix);

            if (mQrCodeImage != null)
                mQrCodeImage.setImageBitmap(mQrCodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Uri saveQrCodeToExternalStorage() {
        File sdcard = Environment.getExternalStorageDirectory();
        FileOutputStream out = null;
        File check = new File(sdcard, "/easyshare/");
        if (!(check.exists())) {
            boolean resu = check.mkdir();
            if(!resu){return null;}
        }

        try {
            File myFile = new File(sdcard, "/easyshare/" + mSsidEt.getText().toString() + ".png");
            out = new FileOutputStream(myFile);
            boolean success = mQrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            if (success) {
                myFile.setReadable(true, false);
                return Uri.fromFile(myFile);
            } else {
                Toast.makeText(getActivity(), getString(R.string.error_transferring), Toast.LENGTH_LONG).show();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
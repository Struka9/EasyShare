package com.apps.xtrange.easyshare;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

/**
 * Created by oscarr on 10/9/15.
 */
public class HotspotNfcShareActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
    private static final String TAG = HotspotNfcShareActivity.class.getSimpleName();
    private NfcAdapter mNfcAdapter;
    private WifiManager mWifiManager;

    private EditText mSsidEt;
    private EditText mPasswordEt;
    private Spinner mSecurityTypeSpinner;
    private Button mCreateApBt;

    private String mSsid;
    private String mPassword;
    private String mEncryption;

    private WifiConfigManager.OnNetworkUpdateListener mNetworkUpdatedListener = new WifiConfigManager.OnNetworkUpdateListener() {
        @Override
        public void onNetworkUpdated() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hotspot_share_nfc_layout);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mSsidEt = (EditText)findViewById(R.id.ssid_et);
        mPasswordEt = (EditText)findViewById(R.id.password_et);
        mSecurityTypeSpinner = (Spinner)findViewById(R.id.security_options_sp);
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

        mCreateApBt = (Button)findViewById(R.id.create_ap_bt);
        mCreateApBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSsid = mSsidEt.getText().toString().trim();
                mPassword = mPasswordEt.getText().toString().trim();

                int encryptionType = mSecurityTypeSpinner.getSelectedItemPosition();
                mEncryption = Util.getEncryption(encryptionType);

                new WifiConfigManager(mWifiManager, true, mNetworkUpdatedListener).execute(mSsid, mPassword, mEncryption);
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, R.string.no_nfc_detected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mNfcAdapter.setNdefPushMessageCallback(this, this);
        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        Util.LogDebug(TAG, "Creating NDEF message");
        try {
            String stringOut = Util.generateCodeForShareHotspot(mSsid, mPassword, mEncryption);
            byte[] bytesOut = stringOut.getBytes();

            NdefRecord ndefRecordOut = new NdefRecord(
                    NdefRecord.TNF_MIME_MEDIA,
                    "text/plain".getBytes(),
                    new byte[] {},
                    bytesOut);

             return new NdefMessage(ndefRecordOut);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {
        Util.LogDebug(TAG, "Something happened " + nfcEvent.toString());
    }
}

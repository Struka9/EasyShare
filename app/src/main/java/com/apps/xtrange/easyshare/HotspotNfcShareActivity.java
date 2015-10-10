package com.apps.xtrange.easyshare;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Created by oscarr on 10/9/15.
 */
public class HotspotNfcShareActivity extends AppCompatActivity {
    private static final String TAG = HotspotNfcShareActivity.class.getSimpleName();
    private NfcAdapter mNfcAdapter;
    private WifiManager mWifiManager;

    private EditText mSsidEt;
    private EditText mPasswordEt;
    private Spinner mSecurityTypeSpinner;
    private Button mCreateApBt;

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
                String ssid = mSsidEt.getText().toString();
                String password = mPasswordEt.getText().toString();

                int encryptionType = mSecurityTypeSpinner.getSelectedItemPosition();
                String encryption = Util.getEncryption(encryptionType);

                Log.d(TAG, encryption);
                new WifiConfigManager(mWifiManager, true, mNetworkUpdatedListener).execute(ssid, password, encryption);
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }
}

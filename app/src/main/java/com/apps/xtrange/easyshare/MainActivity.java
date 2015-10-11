package com.apps.xtrange.easyshare;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

/**
 * Created by oscarr on 10/3/15.
 */
public class MainActivity extends AppCompatActivity implements
        NfcAdapter.OnNdefPushCompleteCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private NfcAdapter mNfcAdapter;

    private WifiConfigManager.OnNetworkUpdateListener mNetworkUpdatedListener = new WifiConfigManager.OnNetworkUpdateListener() {
        @Override
        public void onNetworkUpdated() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Util.LogDebug("MainActivity", "This is it");
        }

        mNfcAdapter = Util.getNfcAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, R.string.no_nfc_detected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    public void onClick(View view) {
        Intent startActivityIntent = null;

        switch (view.getId()) {
            case R.id.share_files_bt:
                startActivityIntent = new Intent(this, ShareFilesActivity.class);
                break;
            case R.id.share_hotspot_nfc_bt:
                startActivityIntent = new Intent(this, HotspotNfcShareActivity.class);
                break;
            case R.id.share_hotspot_wifi_bt:
                startActivityIntent = new Intent(this, ShareHotspotWifiQrActivity.class);
                break;
            default:

                break;
        }

        startActivity(startActivityIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        Util.LogDebug(TAG, action);
        if(action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
            Parcelable[] parcelables =
                    intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage inNdefMessage = (NdefMessage)parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord NdefRecord_0 = inNdefRecords[0];

            String msg = new String(NdefRecord_0.getPayload());
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

            Matcher matcher = Constants.SHARE_HOTSPOT_PATTERN.matcher(msg);

            if (!matcher.matches()) {
                Toast.makeText(this, "It's not a valid Easy Share code.", Toast.LENGTH_SHORT).show();
                return;
            }

            //values[0] = ssid, values[1] = encryption, values[2] = password (ssid and password are encoded)
            String[] values = msg.split(":");
            try {
                String ssid = URLDecoder.decode(values[0], "utf-8");
                String encrytpion = values[1];
                String password = URLDecoder.decode(values[2], "utf-8");

                Util.LogDebug(TAG, "SSID: " + ssid + " and password: " + password + " with encryption: " + encrytpion);

                WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

                new WifiConfigManager(wifiManager, mNetworkUpdatedListener).execute(ssid, password, encrytpion);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Util.LogDebug(TAG, "OnNewIntent");
        setIntent(intent);
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {

    }
}

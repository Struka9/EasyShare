package com.apps.xtrange.easyshare;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by oscarr on 10/9/15.
 */
public class HotspotNfcShareActivity extends AppCompatActivity {
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_with_nfc_layout);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }
}

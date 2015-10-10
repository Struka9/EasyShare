package com.apps.xtrange.easyshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by oscarr on 10/3/15.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}

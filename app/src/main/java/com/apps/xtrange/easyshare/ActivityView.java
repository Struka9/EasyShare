package com.apps.xtrange.easyshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by Oscar on 9/27/2015.
 */
public class ActivityView extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri fileUri = getIntent().getData();

        String type = getIntent().getType();

        Util.LogDebug("ActivityView", fileUri.toString());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, type);
        startActivity(intent);

        finish();
    }
}

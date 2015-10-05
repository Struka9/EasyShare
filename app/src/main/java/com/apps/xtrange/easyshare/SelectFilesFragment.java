package com.apps.xtrange.easyshare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Oscar on 9/1/2015.
 */
public class SelectFilesFragment extends Fragment implements View.OnClickListener {

    private static final int PICK_FILES_REQ_CODE = 0x00001337;
    private static final String TAG = SelectFilesFragment.class.getSimpleName();
    private Button mSelectFilesBt;
    private ProgressDialog mProgress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.select_files_fragment, container, false);

        mSelectFilesBt = (Button)rootView.findViewById(R.id.select_files_button);
        mSelectFilesBt.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.select_files_button) {
            Intent selectFilesIntent = new Intent(Intent.ACTION_GET_CONTENT);
            selectFilesIntent.setType("*/*");

            Intent chooserIntent = Intent.createChooser(selectFilesIntent, "Choose");
            startActivityForResult(chooserIntent, PICK_FILES_REQ_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILES_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Intent intent = new Intent(getActivity(), SendActivity.class);
                intent.putExtra(Constants.EXTRA_FILE_URI, data.getData());
                startActivity(intent);
            }
        }
    }
}

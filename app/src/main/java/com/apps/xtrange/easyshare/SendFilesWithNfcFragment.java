package com.apps.xtrange.easyshare;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Oscar on 9/12/2015.
 */
public class SendFilesWithNfcFragment extends Fragment {

    private static final String TAG = SendFilesWithNfcFragment.class.getSimpleName();

    public static Fragment newInstance(Uri fileUri) {
        Fragment f = new SendFilesWithNfcFragment();

        Bundle args = new Bundle();
        args.putParcelable(Constants.EXTRA_FILE_URI, fileUri);

        f.setArguments(args);
        return f;
    }

    private Uri mFileUri;

    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileUri = getArguments().getParcelable(Constants.EXTRA_FILE_URI);

        Util.LogDebug(TAG, mFileUri.toString());

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mNfcAdapter = Util.getNfcAdapter(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.send_with_nfc_layout, container, false);

        if (mNfcAdapter == null) {
            rootView.findViewById(R.id.nfc_layout).setVisibility(View.GONE);
            rootView.findViewById(R.id.no_nfc_detected_tv).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.nfc_layout).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.no_nfc_detected_tv).setVisibility(View.GONE);

            mNfcAdapter.setBeamPushUrisCallback(new FileUriCallback(), getActivity());
        }
        return rootView;
    }

    private class FileUriCallback implements NfcAdapter.CreateBeamUrisCallback {

        @Override
        public Uri[] createBeamUris(NfcEvent event) {
            Log.d(TAG, "returning " + mFileUri.toString());
            return new Uri[]{mFileUri};
        }
    }
}

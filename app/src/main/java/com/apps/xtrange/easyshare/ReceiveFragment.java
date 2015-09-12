package com.apps.xtrange.easyshare;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

/**
 * Created by Oscar on 9/1/2015.
 */
public class ReceiveFragment extends Fragment {
    private ViewPager mViewPager;
    private TitlePageIndicator mPagerIndicator;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.receive_fragment_layout, container, false);

        mViewPager = (ViewPager)rootView.findViewById(R.id.receive_methods_pager);
        mViewPager.setAdapter(new ReceivePagerAdapter(getActivity(), getChildFragmentManager()));

        mPagerIndicator = (TitlePageIndicator)rootView.findViewById(R.id.titles);
        mPagerIndicator.setViewPager(mViewPager);

        return rootView;
    }

    private class ReceivePagerAdapter extends FragmentStatePagerAdapter {
        private Context mContext;
        private String[] mTitles;

        private QrReaderFragment mQrReaderFragment;
        private Fragment mNfcFragment;

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        public ReceivePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
            mTitles = mContext.getResources().getStringArray(R.array.receiving_options);
            mQrReaderFragment = new QrReaderFragment();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 1:
                case 0:
                    return mQrReaderFragment;

                    //return mNfcFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }
}

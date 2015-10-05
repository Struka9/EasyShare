package com.apps.xtrange.easyshare;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * Created by oscarr on 10/3/15.
 */
public class ShareHotspotWifiQrActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private FrameLayout mContentLayout;

    private String[] mDrawerTitles;
    private CharSequence mCurrentTitle;

    private Fragment[] mDrawerFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_drawer_layout);

        mDrawerTitles = getResources().getStringArray(R.array.share_hotspot_options);
        mDrawerFragments = new Fragment[mDrawerTitles.length];

        mDrawerFragments[0] = new HotspotQrGenFragment();
        mDrawerFragments[1] = new HotspotQrScanFragment();

        mContentLayout = (FrameLayout)findViewById(R.id.content_frame);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.string.open,
                R.string.close);

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, R.id.text, mDrawerTitles));
        mDrawerList.setOnItemClickListener(this);

        selectItem(0);
    }

    private void selectItem(int position) {
        Fragment f = mDrawerFragments[position];
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.content_frame, f, mDrawerTitles[position]);
        ft.commit();

        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        selectItem(i);
    }
}

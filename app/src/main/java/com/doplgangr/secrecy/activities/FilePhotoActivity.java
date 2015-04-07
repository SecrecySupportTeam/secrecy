package com.doplgangr.secrecy.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.adapters.PhotoPagerAdapter;
import com.doplgangr.secrecy.events.ImageLoadDoneEvent;
import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;

import de.greenrobot.event.EventBus;

public class FilePhotoActivity extends FragmentActivity {

    private static Activity context;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_view_pager);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        final PhotoPagerAdapter adapter = new PhotoPagerAdapter(getSupportFragmentManager(), context);

        Bundle extras = getIntent().getExtras();

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(adapter);
        Vault secret = VaultHolder.getInstance().retrieveVault();
        Vault.onFileFoundListener mListener = new Vault.onFileFoundListener() {
            @Override
            public void dothis(EncryptedFile file) {
                adapter.add(file);
            }
        };
        secret.iterateAllFiles(mListener);
        adapter.sort();

        int itemNo = extras.getInt(Config.gallery_item_extra);
        if (itemNo < (mViewPager.getAdapter().getCount())) { //check if requested item is in bound
            mViewPager.setCurrentItem(itemNo);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(ImageLoadDoneEvent event) {
        Util.log("Recieving imageview and bm");
        if (event.bitmap == null) {
            Util.alert(context,
                    context.getString(R.string.Error__out_of_memory),
                    context.getString(R.string.Error__out_of_memory_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            context.finish();
                        }
                    },
                    null);
            return;
        }
        try {
            event.imageView.setImageBitmap(event.bitmap);
        } catch (OutOfMemoryError e) {
            Util.alert(context,
                    context.getString(R.string.Error__out_of_memory),
                    context.getString(R.string.Error__out_of_memory_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            context.finish();
                        }
                    },
                    null);
        }
        event.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mViewPager.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }
}
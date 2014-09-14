package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.Jobs.ImageLoadJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.DummyViews.HackyViewPager;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import uk.co.senab.photoview.PhotoView;

@Fullscreen
@WindowFeature(Window.FEATURE_NO_TITLE)
@EActivity(R.layout.activity_view_pager)
public class FilePhotoFragment extends FragmentActivity {

    static Activity context;
    @Extra(Config.vault_extra)
    String vault;
    @Extra(Config.password_extra)
    String password;
    @Extra(Config.gallery_item_extra)
    Integer itemNo;
    @ViewById(R.id.view_pager)
    HackyViewPager mViewPager;

    @AfterViews
    void onCreate() {
        context = this;
        final SamplePagerAdapter adapter = new SamplePagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(adapter);
        Vault secret = new Vault(vault, password);
        Vault.onFileFoundListener mListener = new Vault.onFileFoundListener() {
            @Override
            public void dothis(File file) {
                adapter.add(file);
            }
        };
        secret.iterateAllFiles(mListener);
        if ((itemNo != null) && (itemNo < (mViewPager.getAdapter().getCount()))) //check if requested item is in bound
            mViewPager.setCurrentItem(itemNo);
    }

    static class SamplePagerAdapter extends FragmentPagerAdapter {

        private static ArrayList<File> files;

        public SamplePagerAdapter(FragmentManager fm) {
            super(fm);
            files = new ArrayList<File>();
        }

        public void add(File file) {
            String mimeType = FileUtils.getMimeType(file.getFile());
            if (mimeType != null)
                if (!mimeType.contains("image"))
                    return; //abort if not images.
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Fragment getItem(int position) {
            return PhotoFragment.newInstance(position);
        }

        public static class PhotoFragment extends Fragment {
            int mNum;

            static PhotoFragment newInstance(int num) {
                PhotoFragment f = new PhotoFragment();

                Bundle args = new Bundle();
                args.putInt(Config.gallery_item_extra, num);
                f.setArguments(args);

                return f;
            }


            public void onEventMainThread(ImageLoadJob.ImageLoadDoneEvent event) {
                Util.log("Recieving imageview and bm");
                try {
                    event.imageView.setImageBitmap(event.bitmap);
                } catch (OutOfMemoryError e) {
                    Util.alert(context,
                            context.getString(R.string.Error__out_of_memory),
                            context.getString(R.string.Error__out_of_memory_message),
                            Util.emptyClickListener,
                            null);
                    context.finish();
                }
                event.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                if (!EventBus.getDefault().isRegistered(this))
                    EventBus.getDefault().register(this);
                mNum = getArguments() != null ? getArguments().getInt(Config.gallery_item_extra) : 1;
            }

            /**
             * The Fragment's UI is just a simple text view showing its
             * instance number.
             */
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {

                final RelativeLayout relativeLayout = new RelativeLayout(container.getContext());
                final File file = files.get(mNum);
                final PhotoView photoView = new PhotoView(container.getContext());
                relativeLayout.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                photoView.setImageBitmap(file.getThumb());
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                final ProgressBar pBar = new ProgressBar(container.getContext());
                pBar.setIndeterminate(false);
                relativeLayout.addView(pBar, layoutParams);
                Util.log("video", FileUtils.getMimeType(file.getFile()));
                CustomApp.jobManager.addJobInBackground(new ImageLoadJob(file, photoView, pBar));
                return relativeLayout;
            }
        }

    }

}
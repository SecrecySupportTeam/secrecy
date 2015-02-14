package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Events.ImageLoadDoneEvent;
import com.doplgangr.secrecy.Exceptions.SecrecyFileException;
import com.doplgangr.secrecy.FileSystem.Encryption.VaultHolder;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.FileSystem.Encryption.Vault;
import com.doplgangr.secrecy.Jobs.ImageLoadJob;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

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

    private static Activity context;
    @Extra(Config.gallery_item_extra)
    Integer itemNo;
    @ViewById(R.id.view_pager)
    ViewPager mViewPager;

    @AfterViews
    void onCreate() {
        context = this;
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        final SamplePagerAdapter adapter = new SamplePagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(adapter);
        Vault secret = VaultHolder.getInstance().retrieveVault();
        Vault.onFileFoundListener mListener = new Vault.onFileFoundListener() {
            @Override
            public void dothis(EncryptedFile file) {
                adapter.add(file);
            }
        };
        secret.iterateAllFiles(mListener);
        if ((itemNo != null) && (itemNo < (mViewPager.getAdapter().getCount()))) //check if requested item is in bound
            mViewPager.setCurrentItem(itemNo);
    }

    @Override
    public void onDestroy(){
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

    static class SamplePagerAdapter extends FragmentStatePagerAdapter {

        private static ArrayList<EncryptedFile> encryptedFiles;

        public SamplePagerAdapter(FragmentManager fm) {
            super(fm);
            encryptedFiles = new ArrayList<EncryptedFile>();
        }

        public void add(EncryptedFile encryptedFile) {
            String mimeType = Util.getFileTypeFromExtension(encryptedFile.getFileExtension());
            if (mimeType != null)
                if (!mimeType.contains("image"))
                    return; //abort if not images.
            encryptedFiles.add(encryptedFile);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return encryptedFiles.size();
        }

        @Override
        public Fragment getItem(int position) {
            return PhotoFragment.newInstance(position);
        }

        public static class PhotoFragment extends Fragment {
            int mNum;
            private ImageLoadJob imageLoadJob = null;

            static PhotoFragment newInstance(int num) {
                PhotoFragment f = new PhotoFragment();

                Bundle args = new Bundle();
                args.putInt(Config.gallery_item_extra, num);
                f.setArguments(args);

                return f;
            }

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                mNum = getArguments() != null ? getArguments().getInt(Config.gallery_item_extra) : 1;
            }

            /**
             * The Fragment's UI is just a simple text view showing its
             * instance number.
             */
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
                Util.log("onCreateView!!");
                final RelativeLayout relativeLayout = new RelativeLayout(container.getContext());
                final EncryptedFile encryptedFile = encryptedFiles.get(mNum);
                final PhotoView photoView = new PhotoView(container.getContext());
                relativeLayout.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                try {
                    photoView.setImageBitmap(encryptedFile.getEncryptedThumbnail().getThumb(150));
                } catch (SecrecyFileException e) {
                    Util.log("No bitmap available!");
                }
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                final ProgressBar pBar = new ProgressBar(container.getContext());
                pBar.setIndeterminate(false);
                relativeLayout.addView(pBar, layoutParams);
                imageLoadJob = new ImageLoadJob(mNum, encryptedFile, photoView, pBar);
                CustomApp.jobManager.addJobInBackground(imageLoadJob);
                return relativeLayout;
            }

            @Override
            public void onPause(){
                super.onPause();
                if (imageLoadJob != null) {
                    imageLoadJob.setObsolet(true);
                }
            }

            @Override
            public void onDestroy(){
                super.onDestroy();
                if (imageLoadJob != null) {
                    imageLoadJob.setObsolet(true);
                }
            }

        }

    }

}
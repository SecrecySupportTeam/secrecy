package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Listeners;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.DummyViews.HackyViewPager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import java.util.ArrayList;

@Fullscreen
@WindowFeature(Window.FEATURE_NO_TITLE)
@EActivity(R.layout.activity_view_pager)
public class FilePhotoFragment extends Activity {

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
        final SamplePagerAdapter adapter = new SamplePagerAdapter(this);
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

    static class SamplePagerAdapter extends PagerAdapter {

        private static ArrayList<File> sDrawables = new ArrayList<File>();
        private static Activity context;

        public SamplePagerAdapter(Activity activity) {
            context = activity;
        }

        //Load a bitmap from a resource with a target size
        static Bitmap decodeSampledBitmapFromResource(java.io.File file, int reqWidth, int reqHeight) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        //Given the bitmap size and View size calculate a subsampling size (powers of 2)
        static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            int inSampleSize = 1;    //Default subsampling size
            // See if image raw height and width is bigger than that of required view
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                //bigger
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }

        public void add(File file) {
            if (file.hasThumbnail())
                sDrawables.add(file);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return sDrawables.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, final int position) {
            final SubsamplingScaleImageView photoView = new SubsamplingScaleImageView(container.getContext(),
                    null,
                    new SubsamplingScaleImageView.onFileFinishCalled() {
                        @Override
                        public void onFinish(String file) {
                            storage.purgeFile(new java.io.File(file));
                        }
                    },
                    new Listeners.EmptyListener() {

                        @Override
                        public void run() {
                            Util.alert(context,
                                    context.getString(R.string.Error__out_of_memory),
                                    context.getString(R.string.Error__out_of_memory_message),
                                    Util.emptyClickListener,
                                    null);
                        }
                    });

            // Now just add PhotoView to ViewPager and return it
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            class bindImage extends AsyncTask<Void, Void, java.io.File> {

                @Override
                protected java.io.File doInBackground(Void... voids) {

                    java.io.File imageFile =
                            sDrawables.get(position).readFile(new CryptStateListener() {
                                @Override
                                public void updateProgress(int progress) {

                                }

                                @Override
                                public void setMax(int max) {

                                }

                                @Override
                                public void onFailed(int statCode) {

                                }

                                @Override
                                public void Finished() {

                                }
                            });
                    return imageFile;
                }

                @Override
                public void onPostExecute(java.io.File file) {
                    try {
                        photoView.setImageFile(
                                file.getAbsolutePath()
                        );
                    } catch (OutOfMemoryError e) {
                        Util.alert(context,
                                context.getString(R.string.Error__out_of_memory),
                                context.getString(R.string.Error__out_of_memory_message),
                                Util.emptyClickListener,
                                null);
                        context.finish();
                    }
                    photoView.setTag(file);
                }

            }

            new bindImage().execute();

            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            java.io.File file = (java.io.File) ((View) object).getTag();
            storage.purgeFile(file);  //Just to be sure. Like when view is deleted before it finishes loading.
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
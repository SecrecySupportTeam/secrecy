/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Events.ThumbLoadDoneEvent;
import com.doplgangr.secrecy.Exceptions.SecrecyFileException;
import com.doplgangr.secrecy.FileSystem.Files.EncryptedFile;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;

import java.util.ArrayList;
import java.util.Comparator;

import de.greenrobot.event.EventBus;

class FilesListAdapter extends ArrayAdapter<EncryptedFile> {
    // store the context (as an inflated layout)
    private final LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private final int resource;
    private final ArrayList<ViewNIndex> checked = new ArrayList<ViewNIndex>();
    private boolean isGallery;
    // store (a reference to) the data
    private ArrayList<EncryptedFile> data = new ArrayList<EncryptedFile>();

    public FilesListAdapter(Context context, int layout) {
        super(context, layout, new ArrayList<EncryptedFile>());
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = layout;
        this.isGallery = (layout == R.layout.gallery_item);
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    /**
     * Add data to data set.
     */
    public void add(EncryptedFile encryptedFile) {
        if (encryptedFile == null)
            return;
        if (encryptedFile.getDecryptedFileName() == null)
            return;
        if (isGallery) {
            String mimeType = Util.getFileTypeFromExtension(encryptedFile.getFileExtension());
            if (mimeType != null)
                if (!mimeType.contains("image"))
                    return; //abort if not images.
        }
        if (!data.contains(encryptedFile))
            data.add(encryptedFile);
        notifyDataSetChanged();
    }

    /**
     * Add data to data set.
     */
    public void remove(int position) {
        data.remove(position);
        notifyDataSetChanged();
    }

    /**
     * Return the size of the data set.
     */
    public int getCount() {
        return this.data.size();
    }

    /**
     * Return if index is in data set.
     */
    public boolean hasIndex(int position) {
        return this.data.size() > position && position > -1;
    }

    /**
     * Return an object in the data set.
     */
    public EncryptedFile getItem(int position) {
        return this.data.get(position);
    }

    /**
     * Return the position provided.
     */
    public int getItemId(EncryptedFile encryptedFile) {
        return data.indexOf(encryptedFile);
    }

    /**
     * Return a generated view for a position.
     */
    public void update(ArrayList<EncryptedFile> data) {
        this.data = data;
        checked.clear();
    }

    public View getView(EncryptedFile encryptedFile, View convertView, ViewGroup parent) {
        int position = data.indexOf(encryptedFile);
        return getView(position, convertView, parent);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // reuse a given view, or inflate a new one from the xml
        View view;

        if (convertView == null) {
            view = this.inflater.inflate(resource, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.name);
            viewHolder.type = (TextView) view.findViewById(R.id.type);
            viewHolder.size = (TextView) view.findViewById(R.id.size);
            viewHolder.date = (TextView) view.findViewById(R.id.date);
            viewHolder.thumbnail = (ImageView) view.findViewById(R.id.thumbNail);
            viewHolder.frame = (FrameLayout) view.findViewById(R.id.frame);
            viewHolder.animator = (ViewAnimator) view.findViewById(R.id.viewAnimator);
            viewHolder.selected = false;
            viewHolder.page = 0;
            view.setTag(viewHolder);
        } else {
            view = convertView;
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.selected = false;
            for (ViewNIndex obj : checked)
                if (obj.index == position)
                    viewHolder.selected = true;
        }

        // bind the data to the view object
        return this.bindData(view, position);
    }

    /**
     * Bind the provided data to the view.
     * This is the only method not required by base adapter.
     */
    View bindData(final View view, int position) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        // make sure it's worth drawing the view
        if (position >= this.data.size())  // To prevent out of bound exception
            return view;
        if (this.data.get(position) == null)
            return view;

        // pull out the object
        final EncryptedFile encryptedFile = this.data.get(position);
        if (viewHolder.name != null)
            viewHolder.name.setText(encryptedFile.getDecryptedFileName());

        if (viewHolder.type != null)
            viewHolder.type.setText(encryptedFile.getType());

        if (viewHolder.size != null)
            viewHolder.size.setText(encryptedFile.getFileSize());

        if (viewHolder.date != null)
            viewHolder.date.setText(encryptedFile.getTimestamp());

        if (viewHolder.thumbnail != null) {
            viewHolder.thumbnail.setVisibility(View.GONE);
            viewHolder.thumbnail.setTag(encryptedFile.getDecryptedFileName());
        }
        if (viewHolder.frame != null)
            viewHolder.frame.setForeground(
                    viewHolder.selected ?
                            getContext().getResources().getDrawable(R.drawable.file_selector) :
                            null);
        if (viewHolder.animator != null)
            viewHolder.animator.setDisplayedChild(viewHolder.page);
        final int avatar_size = (int) CustomApp.context.getResources().getDimension(R.dimen.list_item_avatar_size);

        // This class is for binding thumbnail to UI
        class BindImageTask extends AsyncTask<EncryptedFile, Void, Bitmap> {
            protected Bitmap doInBackground(EncryptedFile... files) {
                if (isGallery) {
                    try {
                        return files[0].getEncryptedThumbnail().getThumb(100);
                    } catch (SecrecyFileException e) {
                        Util.log("No bitmap available!");
                    }
                }
                try {
                    return files[0].getEncryptedThumbnail().getThumb(avatar_size);     // async decrypt thumbnail
                } catch (SecrecyFileException e) {
                    Util.log("No bitmap available!");
                }
                return null;
            }

            protected void onPostExecute(Bitmap thumbnail) {
                String name = (String) viewHolder.thumbnail.getTag();
                if (name.equals(encryptedFile.getDecryptedFileName()) && (thumbnail != null) && (viewHolder.thumbnail != null)) {
                    viewHolder.thumbnail.setImageBitmap(thumbnail);   // bind thumbnail in UI thread
                    viewHolder.thumbnail.setVisibility(View.VISIBLE);
                }
            }
        }
        new BindImageTask().execute(encryptedFile);

        // return the final view object
        return view;
    }


    public void onEventMainThread(ThumbLoadDoneEvent event) {
        try {
            String name = (String) event.imageView.getTag();
            if (name.equals(event.encryptedFile.getDecryptedFileName()) && (event.bitmap != null) && (event.imageView != null)) {
                event.imageView.setImageBitmap(event.bitmap);   // bind thumbnail in UI thread
                event.imageView.setVisibility(View.VISIBLE);
            }
        } catch (OutOfMemoryError ignored) {
        }
    }

    public Boolean select(int position, View view) {
        ViewNIndex object = new ViewNIndex(position, view);
        for (ViewNIndex obj : checked)
            if (position == obj.index) {
                checked.remove(checked.indexOf(obj));
                return false;
            }
        checked.add(object);
        return true;
    }

    public ArrayList<ViewNIndex> getSelected() {
        return checked;
    }

    public void clearSelected() {
        checked.clear();
    }

    public void clear() {
        data.clear();
    }

    public void sort() {
        this.sort(new Comparator<EncryptedFile>() {
            @Override
            public int compare(EncryptedFile encryptedFile, EncryptedFile encryptedFile2) {
                return encryptedFile.getDecryptedFileName().compareTo(encryptedFile2.getDecryptedFileName());
            }
        });
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView name;
        public TextView type;
        public TextView size;
        public TextView date;
        public ImageView thumbnail;
        public FrameLayout frame;
        public Boolean selected;
        public ViewAnimator animator;
        public int page;
    }

    static class ViewNIndex {
        public Integer index;
        public View view;

        public ViewNIndex(Integer index, View view) {
            this.index = index;
            this.view = view;
        }

    }


}

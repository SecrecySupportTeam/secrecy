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

package com.doplgangr.secrecy.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.events.ThumbLoadDoneEvent;
import com.doplgangr.secrecy.exceptions.SecrecyFileException;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class FilesListAdapter extends RecyclerView.Adapter<FilesListAdapter.ViewHolder> {

    private final Context context;
    private boolean isGallery;
    private final int layout;
    // store (a reference to) the data
    private final List<EncryptedFile> data = new ArrayList<EncryptedFile>();
    private final Set<Integer> selectedItems = new HashSet<Integer>();
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onLongClickListener;

    public FilesListAdapter(Context context, boolean isGallery) {
        this.isGallery = false;
        this.context = context;
        this.isGallery = isGallery;
        layout = isGallery ? R.layout.gallery_item : R.layout.file_item;
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnLongClickListener(OnItemLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
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
                    return;
        }
        if (!data.contains(encryptedFile)) {
            data.add(encryptedFile);
        }
        notifyItemInserted(data.size() - 1);
    }

    public void remove(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    public void remove(List<Integer> selected) {
        // Remove items in reverse order to keep indices in order
        Collections.sort(selected, Collections.reverseOrder());
        for (Integer i : selected) {
            data.remove((int) i);
            notifyItemRemoved(i);
        }
    }

    /**
     * Return the size of the data set.
     */
    @Override
    public int getItemCount() {
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

    boolean isSelected(int position){
        return selectedItems.contains(position);
    }

    public boolean select(int position) {
        if (isSelected(position)){
            selectedItems.remove(position);
            return false;
        }
        selectedItems.add(position);
        return true;
    }

    public Set<Integer> getSelected() {
        return selectedItems;
    }

    public void clearSelected() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public void clear() {
        data.clear();
        selectedItems.clear();
    }

    public void sort() {
        Comparator<EncryptedFile> comparator;

        switch (PreferenceManager.getDefaultSharedPreferences(context).getString(
                Config.VAULT_SORT, Config.VAULT_SORT_ALPHABETIC)) {

            case Config.VAULT_SORT_ALPHABETIC:
                comparator = Config.COMPARATOR_ENCRYPTEDFILE_ALPHABETIC;
                break;
            case Config.VAULT_SORT_FILETYPE:
                comparator = Config.COMPARATOR_ENCRYPTEDFILE_FILETYPE;
                break;
            case Config.VAULT_SORT_LASTMODIFIED:
                comparator = Config.COMPARATOR_ENCRYPTEDFILE_LASTMODIFIED;
                break;
            default:
                comparator = null;
        }

        if (comparator != null) {
            Collections.sort(data, comparator);
            notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        final EncryptedFile encryptedFile = this.data.get(position);

        if (viewHolder.name != null){
            viewHolder.name.setText(encryptedFile.getDecryptedFileName());
        }
        if (viewHolder.type != null) {
            viewHolder.type.setText(encryptedFile.getType());
        }
        if (viewHolder.size != null) {
            viewHolder.size.setText(encryptedFile.getFileSize());
        }
        if (viewHolder.date != null) {
            viewHolder.date.setText(encryptedFile.getTimestamp());
        }
        if (viewHolder.thumbnail != null) {
            viewHolder.thumbnail.setVisibility(View.GONE);
            viewHolder.thumbnail.setTag(encryptedFile.getDecryptedFileName());
        }
        if (viewHolder.frame != null) {
            viewHolder.frame.setForeground(isSelected(position) ?
                            context.getResources().getDrawable(R.drawable.file_selector) :
                            null);
        }

        if (viewHolder.progressBar != null){
            encryptedFile.setProgressBar(viewHolder.progressBar);
            encryptedFile.getProgressBar().setMax((int) encryptedFile.getFile().length());
        }

        if (viewHolder.animator != null) {
            viewHolder.animator.setDisplayedChild(viewHolder.page);

            int viewIndex;
            if (data.get(position).getIsDecrypting()){
                viewIndex = 1;
            } else {
                viewHolder.animator.setInAnimation(null);
                viewIndex = 0;
            }
            viewHolder.animator.setDisplayedChild(viewIndex);
        }

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
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public final TextView name;
        public final TextView type;
        public final TextView size;
        public final TextView date;
        public final ImageView thumbnail;
        public final FrameLayout frame;
        public final ViewAnimator animator;
        public final ProgressBar progressBar;
        public int page;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            type = (TextView) itemView.findViewById(R.id.type);
            size = (TextView) itemView.findViewById(R.id.size);
            date = (TextView) itemView.findViewById(R.id.date);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbNail);
            frame = (FrameLayout) itemView.findViewById(R.id.frame);
            frame.setTag(this);
            animator = (ViewAnimator) itemView.findViewById(R.id.viewAnimator);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            frame.setOnClickListener(this);
            frame.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, getPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onLongClickListener != null){
                onLongClickListener.onItemLongClick(view, getPosition());
                return true;
            }
            return false;
        }
    }

    public interface OnItemClickListener {
        public void onItemClick(View view , int position);
    }
    public interface OnItemLongClickListener {
        public boolean onItemLongClick(View view , int position);
    }

}

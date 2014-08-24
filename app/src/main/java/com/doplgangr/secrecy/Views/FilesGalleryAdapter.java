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
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.R;

import java.util.ArrayList;

class FilesGalleryAdapter extends BaseAdapter {
    // store the context (as an inflated layout)
    private final LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private final int resource;
    private final ArrayList<Integer> checked = new ArrayList<Integer>();
    // store (a reference to) the data
    private ArrayList<File> data = new ArrayList<File>();

    public FilesGalleryAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = R.layout.gallery_item;
    }

    public void add(File file) {
        if (file.hasThumbnail())
            data.add(file);
        notifyDataSetChanged();
    }

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
     * Return an object in the data set.
     */
    public File getItem(int position) {
        return this.data.get(position);
    }

    /**
     * Return the position provided.
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Return a generated view for a position.
     */
    public void update(ArrayList<File> data) {
        this.data = data;
        checked.clear();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        // reuse a given view, or inflate a new one from the xml
        View row = convertView;
        ViewHolder holder = new ViewHolder();
        // pull out the object
        final File file = this.data.get(position);

        if (row == null) {
            row = this.inflater.inflate(resource, parent, false);
            View viewElement = row.findViewById(R.id.thumbNail);
            holder.image = (ImageView) viewElement;
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
        holder.image.setTag(file.getName());
        holder.image.setImageResource(R.drawable.ic_loading);


        // make sure it's worth drawing the view
        if (position >= this.data.size())  // To prevent out of bound exception
            return row;
        if (this.data.get(position) == null)
            return row;

        final ViewHolder finalHolder = holder;

        // This class is for binding thumbnail to UI
        class BindImageTask extends AsyncTask<Void, Void, Bitmap> {
            protected Bitmap doInBackground(Void... voids) {
                return file.getThumb();     // async decrypt thumbnail
            }

            protected void onPostExecute(Bitmap thumbnail) {
                String name = (String) finalHolder.image.getTag();
                if ((name.equals(file.getName())) && (thumbnail != null))
                    finalHolder.image.setImageBitmap(thumbnail);   // bind thumbnail in UI thread
            }
        }
        new BindImageTask().execute();

        // bind the data to the view object
        return row;
    }

    public Boolean select(int position) {
        if (checked.contains(position))
            checked.remove(checked.indexOf(position));
        else
            checked.add(position);
        return checked.contains(position);
    }

    public ArrayList<Integer> getSelected() {
        return checked;
    }

    public void clearSelected() {
        checked.clear();
    }

    public void clear() {
        data.clear();
    }

    public class ViewHolder {
        ImageView image;
    }
}

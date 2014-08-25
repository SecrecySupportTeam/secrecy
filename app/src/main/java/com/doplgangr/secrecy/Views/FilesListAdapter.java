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

import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.R;

import java.util.ArrayList;

class FilesListAdapter extends ArrayAdapter<File> {
    // store the context (as an inflated layout)
    private final LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private final int resource;
    private final ArrayList<Integer> checked = new ArrayList<Integer>();
    // store (a reference to) the data
    private ArrayList<File> data = new ArrayList<File>();

    public FilesListAdapter(Context context, int layout) {
        super(context, layout, new ArrayList<File>());
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = layout;
    }

    /**
     * Add data to data set.
     */
    public void add(File file) {
        data.add(file);
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

    public View getView(File file, View convertView, ViewGroup parent) {
        int position = data.indexOf(file);
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
            viewHolder.selected = false;
            view.setTag(viewHolder);
        } else {
            view = convertView;
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.selected = checked.contains(position);
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
        final File file = this.data.get(position);
        if (viewHolder.name != null)
            viewHolder.name.setText(file.getName());

        if (viewHolder.type != null)
            viewHolder.type.setText(file.getType());

        if (viewHolder.size != null)
            viewHolder.size.setText(file.getSize());

        if (viewHolder.date != null)
            viewHolder.date.setText(file.getTimestamp());

        if (viewHolder.thumbnail != null) {
            viewHolder.thumbnail.setVisibility(View.GONE);
            viewHolder.thumbnail.setTag(file.getName());
        }

        viewHolder.frame.setForeground(
                viewHolder.selected ?
                        getContext().getResources().getDrawable(R.drawable.file_selector) :
                        null);

        // This class is for binding thumbnail to UI
        class BindImageTask extends AsyncTask<File, Void, Bitmap> {
            protected Bitmap doInBackground(File... files) {
                return files[0].getThumb();     // async decrypt thumbnail
            }

            protected void onPostExecute(Bitmap thumbnail) {
                String name = (String) viewHolder.thumbnail.getTag();
                if (name.equals(file.getName()) && (thumbnail != null)) {
                    viewHolder.thumbnail.setImageBitmap(thumbnail);   // bind thumbnail in UI thread
                    viewHolder.thumbnail.setVisibility(View.VISIBLE);
                }
            }
        }
        new BindImageTask().execute(file);

        // return the final view object
        return view;
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

    static class ViewHolder {
        public TextView name;
        public TextView type;
        public TextView size;
        public TextView date;
        public ImageView thumbnail;
        public FrameLayout frame;
        public Boolean selected;
    }


}

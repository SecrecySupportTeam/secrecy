package com.doplgangr.secrecy.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.fragments.FileChooserFragment;
import com.doplgangr.secrecy.utils.Util;

import java.io.File;
import java.util.List;

public class FileChooserAdapter extends RecyclerView.Adapter<FileChooserAdapter.FileChooserViewHolder> {

    private Context context;
    /**
     * Inflater of the context;
     */
    private LayoutInflater inflater;
    /**
     * File object of the directory to be displayed
     */
    private File rootFile;
    /**
     * File object of the children to the directory to be displayed
     */
    private List<File> files;

    private FileChooserFragment.OnFileChosen mListener;


    public FileChooserAdapter(Activity context,
                              List<File> files, File rootFile, FileChooserFragment.OnFileChosen mListener) {
        this.context = context;
        this.inflater = context.getLayoutInflater();
        this.files = files;
        this.rootFile = rootFile;
        this.mListener = mListener;
    }

    @Override
    public FileChooserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FileChooserViewHolder(inflater.inflate(R.layout.listitem_single_line_text, parent, false));
    }

    @Override
    public void onBindViewHolder(FileChooserViewHolder holder, int position) {

        final File item = files.get(position);
        Boolean isParent = rootFile.getParentFile()!=null && rootFile.getParentFile().getAbsolutePath().equals(item.getAbsolutePath());
        Boolean isFile = item.isFile();
        Boolean isReadableDir = Util.canReadDir(item);

        if (holder.textView!=null){
            // TODO: create a more intuitive way to let user know this is "up"
            // If the rootFile has a parent, display as "up"
            if (isParent)
                holder.textView.setText("..");
            else {
                holder.textView.setText(item.getName());
            }
            holder.textView.setEnabled(isFile || isReadableDir);
            if (isSubDirectory(item, Storage.getRoot()) && !isParent)
                holder.textView.setTextColor(context.getResources().getColor(R.color.accent));
            else
                holder.textView.setTextColor(context.getResources().getColor(R.color.text_primary));
        }
        if (holder.iconView!=null){
            if (isFile)
                holder.iconView.setImageResource(R.drawable.ic_file);
            if (isReadableDir)
                holder.iconView.setImageResource(R.drawable.ic_action_folder);
            if (isSubDirectory(item, Storage.getRoot()) && !isParent)
                holder.iconView.setColorFilter(context.getResources().getColor(R.color.accent));
            else
                holder.iconView.setColorFilter(context.getResources().getColor(R.color.button));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    if (Util.canReadDir(item))
                        mListener.onFileSelected(item, false);
                    else
                        mListener.onFileSelected(item, true);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (files!=null)
            return files.size();
        return 0;
    }

    /**
     * Checks whether a directory is a subdirectory under certain root directory
     * @param directory root directory to be checked against
     * @param file file suspected of being a subdirectory
     * @return true if file is in directory, false if not.
     */
    public static boolean isSubDirectory(File directory, File file) {
        if (file == null)
            return false;
        if (file.equals(directory))
            return true;
        return isSubDirectory(directory, file.getParentFile());
    }

    public class FileChooserViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView iconView;
        public FileChooserViewHolder(View itemView) {
            super(itemView);
             textView =(TextView) itemView.findViewById(R.id.text1);
             iconView =(ImageView) itemView.findViewById(R.id.icon1);
        }
    }
}

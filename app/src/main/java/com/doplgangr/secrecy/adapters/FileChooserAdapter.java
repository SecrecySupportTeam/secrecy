package com.doplgangr.secrecy.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.fragments.FileChooserFragment;
import com.doplgangr.secrecy.utils.Util;

import java.io.File;
import java.util.List;

public class FileChooserAdapter extends ArrayAdapter<File> {

    private Context context;
    /**
     * File object of the directory to be displayed
     */
    private File rootFile;

    public FileChooserAdapter(Context context,
                              List<File> objects, File rootFile) {
        super(context, R.layout.listitem_single_line_text, R.id.text1, objects);
        this.context = context;
        this.rootFile = rootFile;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        File item = getItem(position);
        TextView textView =(TextView) view.findViewById(R.id.text1);
        ImageView iconView =(ImageView) view.findViewById(R.id.icon1);
        Boolean isParent = rootFile.getParentFile()!=null && rootFile.getParentFile().getAbsolutePath().equals(item.getAbsolutePath());

        // TODO: create a more intuitive way to let user know this is "up"
        // If the rootFile has a parent, display as "up"
        if (isParent)
            textView.setText("..");
        else {
            textView.setText(item.getName());
        }

        textView.setEnabled(Util.canReadDir(item));
        iconView.setImageResource(R.drawable.ic_action_folder);
        // Highlights the directory if it is the path to the current vault root
        if (isSubDirectory(item, Storage.getRoot()) && !isParent) {
            textView.setTextColor(context.getResources().getColor(R.color.accent));
            iconView.setColorFilter(context.getResources().getColor(R.color.accent));
        }else{
            iconView.setColorFilter(context.getResources().getColor(R.color.button));
        }

        return view;
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
}

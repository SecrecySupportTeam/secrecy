package com.doplgangr.secrecy.views.navdrawer;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.utils.Util;

import java.util.ArrayList;

/**
 * Created by matthew on 9/18/14.
 */
public class NavAdapter extends ArrayAdapter<NavItem> {

    private final Context mContext;
    private ArrayList<NavItem> mNavigationItems = new ArrayList<NavItem>();

    public NavAdapter(Context context, int resource,
                      ArrayList<NavItem> drawerItems) {

        super(context, resource, drawerItems);
        this.mContext = context;
        this.mNavigationItems = drawerItems;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;

        // Inflate layout.
        if (v == null)
            v = LayoutInflater.from(getContext()).inflate(R.layout.drawer_item, null);

        NavItem NavItem = mNavigationItems.get(position);

        TextView itemView = (TextView) v.findViewById(R.id.item_navigation_title);
        ImageView imgView = (ImageView) v.findViewById(R.id.item_navigation_img);

        itemView.setText(NavItem.getItem());
        imgView.setImageDrawable(getColouredDrawable(mContext.getResources().getDrawable(NavItem.getImg()), NavItem.getIsSelected()));
        imgView.setTag(NavItem.getItem());
        Util.log(imgView);

        itemView.setTextColor(NavItem.getIsSelected() ? mContext.getResources().getColor(R.color.primary) : mContext.getResources().getColor(R.color.text_secondary));

        return v;
    }

    private Drawable getColouredDrawable(Drawable _drawable, boolean _isSelected) {
        int iColor = _isSelected ? mContext.getResources().getColor(R.color.primary) : mContext.getResources().getColor(R.color.text_secondary);

        int red = (iColor & 0xFF0000) / 0xFFFF;
        int green = (iColor & 0xFF00) / 0xFF;
        int blue = iColor & 0xFF;

        float[] matrix = {0, 0, 0, 0, red
                , 0, 0, 0, 0, green
                , 0, 0, 0, 0, blue
                , 0, 0, 0, 1, 0};

        ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);

        _drawable.setColorFilter(new PorterDuffColorFilter(iColor, PorterDuff.Mode.SRC_IN));

        return _drawable;
    }
}

package com.doplgangr.secrecy.Views.DummyViews.NavDrawer;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.doplgangr.secrecy.R;

import java.util.ArrayList;

/**
 * Created by matthew on 9/18/14.
 */
public class NavListView extends ListView {

    private static int mSelectedColour;
    private NavAdapter mNavigationAdapter;
    private final Context mContext;
    private final ArrayList<NavItem> mNavigationItems = new ArrayList<NavItem>();
    private NavigationItemClickListener mCallbacks;

    public NavListView(Context _context) {
        this(_context, null, 0);
    }

    public NavListView(Context _context, AttributeSet _attrs) {
        this(_context, _attrs, 0);
    }

    public NavListView(Context _context, AttributeSet _attrs, int _defStyle) {
        super(_context, _attrs, _defStyle);
        mContext = _context;
        init(_attrs);
    }

    private void init(AttributeSet _attrs) {
        mNavigationAdapter = new NavAdapter(mContext, R.layout.drawer_item, mNavigationItems);
        mSelectedColour = getResources().getColor(R.color.primary);
        setAdapter(mNavigationAdapter);

        try {
            mCallbacks = (NavigationItemClickListener) mContext;
        } catch (ClassCastException e) {
            Log.w("L Navigation Drawer", mContext.getClass() + " should implement NavListView.NavigationItemClickListener");
        }

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mCallbacks != null) {
                    mCallbacks.onNavigationItemSelected(mNavigationItems.get(i).getItem(), mNavigationItems, i);
                }

                for (NavItem ni : mNavigationItems)
                    ni.setIsSelected(false);

                mNavigationItems.get(i).setIsSelected(true);

                mNavigationAdapter.notifyDataSetChanged();
            }
        });
    }

    public NavListView setNavigationItemClickListener(NavigationItemClickListener _navigationItemClickListener) {
        this.mCallbacks = _navigationItemClickListener;
        return this;
    }

    public NavListView setItems(String[] _items) {
        for (int i = 0; i < _items.length; i++) {
            try {
                mNavigationItems.get(i).setItem(_items[i]);
            } catch (Exception e) {
                mNavigationItems.add(new NavItem(_items[i]));
            }
        }

        if (mNavigationAdapter != null)
            mNavigationAdapter.notifyDataSetChanged();
        return this;
    }

    public NavListView setImages(int[] _res) {
        for (int i = 0; i < _res.length; i++) {
            try {
                mNavigationItems.get(i).setImg(_res[i]);
            } catch (Exception e) {
                mNavigationItems.add(new NavItem("", _res[i]));
            }
        }

        if (mNavigationAdapter != null)
            mNavigationAdapter.notifyDataSetChanged();
        return this;
    }

    public NavListView addNavigationItem(NavItem _navigationItem) {
        mNavigationItems.add(_navigationItem);
        mNavigationAdapter.notifyDataSetChanged();
        return this;
    }

    public NavListView addNavigationItem(String _title, int _res, boolean _isSelected) {
        mNavigationItems.add(new NavItem(_title, _res, _isSelected));
        mNavigationAdapter.notifyDataSetChanged();
        return this;
    }

    public NavListView setSelectedItem(int _position) {
        try {
            for (NavItem NavItem : mNavigationItems)
                NavItem.setIsSelected(false);
            mNavigationItems.get(_position).setIsSelected(true);
            mNavigationAdapter.notifyDataSetChanged();
        } catch (IndexOutOfBoundsException e) {
            Log.e("L Navigation Drawer", "Index specified in setSelectedItem doesn't exist in the list.");
        }
        return this;
    }

    public NavListView setSelectedColor(String _colour) {
        try {
            mSelectedColour = Color.parseColor(_colour);
        } catch (Exception e) {
            Log.e("L Navigation Drawer", "Invalid hex code " + _colour);
        }

        if (mNavigationAdapter != null)
            mNavigationAdapter.notifyDataSetChanged();
        return this;
    }

    public interface NavigationItemClickListener {
        public void onNavigationItemSelected(String item, ArrayList<NavItem> items, int position);
    }
}

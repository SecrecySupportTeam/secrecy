package com.doplgangr.secrecy.views.navdrawer;

/**
 * Created by matthew on 9/18/14.
 */
public class NavItem {
    private int mImg = 0;
    private String mItem = "";
    private boolean mIsSelected = false;

    public NavItem() {

    }

    public NavItem(String _item) {
        setItem(_item);
    }

    public NavItem(String _item, int _img) {
        this(_item);
        setImg(_img);
    }

    public NavItem(String _item, int _img, boolean _isSelected) {
        this(_item, _img);
        setIsSelected(_isSelected);
    }

    public String getItem() {
        return mItem;
    }

    public NavItem setItem(String _item) {
        mItem = _item;
        return this;
    }

    public int getImg() {
        return mImg;
    }

    public NavItem setImg(int _img) {
        mImg = _img;
        return this;
    }

    public boolean getIsSelected() {
        return mIsSelected;
    }

    public NavItem setIsSelected(boolean _isSelected) {
        mIsSelected = _isSelected;
        return this;
    }
}

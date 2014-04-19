package com.doplgangr.secrecy.UpdateManager;

import com.doplgangr.secrecy.R;

import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultRes;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by Matthew on 4/12/2014.
 */
@SharedPref
public interface AppVersion {
    @DefaultRes(R.string.alpha0_1)
    String name();

    @DefaultInt(1)
    int no();

}
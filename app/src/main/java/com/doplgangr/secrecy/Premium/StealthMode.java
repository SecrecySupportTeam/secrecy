package com.doplgangr.secrecy.Premium;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.doplgangr.secrecy.Views.MainActivity;

/**
 * Created by matthew on 8/28/14.
 */
public class StealthMode {

    //Hides the app here.
    //Set the counter to 1, prevent appearance of first time alert in the future
    //Set LauncherActivity to disabled, removing launcher icon.
    public static void hideApp(final Context context) {

        ComponentName componentToDisable =
                new ComponentName(context.getPackageName(),
                        MainActivity.class.getName());
        if (context.getPackageManager() != null)
            context.getPackageManager()
                    .setComponentEnabledSetting(componentToDisable,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
    }

    public static void showApp(final Context context) {

        ComponentName componentToDisable =
                new ComponentName(context.getPackageName(),
                        MainActivity.class.getName());
        if (context.getPackageManager() != null)
            context.getPackageManager()
                    .setComponentEnabledSetting(componentToDisable,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
    }
}

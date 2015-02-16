package com.doplgangr.secrecy.Jobs;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.FileSystem.OurFileProvider;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.FileViewer;
import com.doplgangr.secrecy.Views.FilesActivity;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SendFileJob extends Job {
    private static final int PRIORITY = 9;   //High. Lower than UI jobs
    private final Activity context;
    private final ArrayList<FileViewer.DecryptArgHolder> args;

    public SendFileJob(Activity context, ArrayList<FileViewer.DecryptArgHolder> args) {
        super(new Params(PRIORITY));
        this.args = args;
        this.context = context;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        Set<String> mimes = new HashSet<String>();
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        for (FileViewer.DecryptArgHolder arg : args) {
            File tempFile = arg.encryptedFile.readFile(arg.onFinish);
            //File specified is not invalid
            if (tempFile != null) {
                if (tempFile.getParentFile().equals(Storage.getTempFolder()))
                    tempFile = new java.io.File(Storage.getTempFolder(), tempFile.getName());
                uris.add(OurFileProvider.getUriForFile(context, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile));
                mimes.add(myMime.getMimeTypeFromExtension(arg.encryptedFile.getType()));
            }
        }
        if (uris.size() == 0 || mimes.size() == 0)
            return;
        Intent newIntent;
        if (uris.size() == 1) {
            newIntent = new Intent(Intent.ACTION_SEND);
            newIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            newIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            newIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        if (mimes.size() > 1)
            newIntent.setType("text/plain");                        //Mixed filetypes
        else
            newIntent.setType(new ArrayList<String>(mimes).get(0));
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooserIntent = generateCustomChooserIntent(newIntent, uris);
        try {
            context.startActivity(Intent.createChooser(chooserIntent, CustomApp.context.getString(R.string.Dialog__send_file)));
            FilesActivity.onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            Util.toast(context, CustomApp.context.getString(R.string.Error__no_activity_view), Toast.LENGTH_LONG);
            FilesActivity.onPauseDecision.finishActivity();
        }
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        throwable.printStackTrace();
        return false;   //Should Not Rerun
    }


    private Intent generateCustomChooserIntent(Intent prototype, ArrayList<Uri> uris) {
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
        Intent chooserIntent;

        Intent dummy = new Intent(prototype.getAction());
        dummy.setType(prototype.getType());
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(dummy, 0);

        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName.equalsIgnoreCase("com.doplgangr.secrecy"))
                    continue;

                HashMap<String, String> info = new HashMap<String, String>();
                info.put("packageName", resolveInfo.activityInfo.packageName);
                info.put("className", resolveInfo.activityInfo.name);
                info.put("simpleName", String.valueOf(resolveInfo.activityInfo.loadLabel(context.getPackageManager())));
                intentMetaInfo.add(info);
                for (Uri uri : uris)
                    context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (!intentMetaInfo.isEmpty()) {
                // sorting for nice readability
                Collections.sort(intentMetaInfo, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> map, HashMap<String, String> map2) {
                        return map.get("simpleName").compareTo(map2.get("simpleName"));
                    }
                });

                // create the custom intent list
                for (HashMap<String, String> metaInfo : intentMetaInfo) {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(metaInfo.get("packageName"));
                    targetedShareIntent.setClassName(metaInfo.get("packageName"), metaInfo.get("className"));
                    targetedShareIntents.add(targetedShareIntent);
                }
                chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), CustomApp.context.getString(R.string.Dialog__send_file));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[targetedShareIntents.size()]));
                return chooserIntent;
            }
        }

        return new Intent(Intent.ACTION_SEND);  //Unable to do anything. Duh.
    }
}
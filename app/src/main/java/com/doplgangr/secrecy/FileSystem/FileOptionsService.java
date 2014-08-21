package com.doplgangr.secrecy.FileSystem;

import android.app.IntentService;
import android.content.Intent;

import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;

/**
 * Created by matthew on 8/22/14.
 */
@EIntentService
public class FileOptionsService extends IntentService {

    public FileOptionsService() {
        super(FileOptionsService.class.getName());
    }

    @ServiceAction
    void delete(java.io.File file){
        storage.shredFile(file);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //ignored
    }
}

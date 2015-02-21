package com.doplgangr.secrecy.events;

import java.io.File;

/**
 * Created by matthew on 11/3/14.
 */
public class RestoreDoneEvent {
    public final File backupFile; //Path of backupFile

    public RestoreDoneEvent(File backupFile) {
        this.backupFile = backupFile;
    }
}

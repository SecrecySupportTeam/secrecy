package com.doplgangr.secrecy.events;

import java.io.File;

/**
 * Created by matthew on 11/2/14.
 */
public class BackUpDoneEvent {
    public final File backupPath;    //Path of the folder to backup
    public final File backupFile; //Path of backupFile

    public BackUpDoneEvent(File backupPath, File backupFile) {
        this.backupPath = backupPath;
        this.backupFile = backupFile;
    }
}
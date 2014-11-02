package com.doplgangr.secrecy.Events;

import java.io.File;

/**
 * Created by matthew on 11/2/14.
 */
public class BackUpDoneEvent {
    private File backupPath;    //Path of the folder to backup
    private File backupFile; //Path of backupFile

    public BackUpDoneEvent(File backupPath, File backupFile) {
        this.backupPath = backupPath;
        this.backupFile = backupFile;
    }
}
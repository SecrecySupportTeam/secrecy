package com.doplgangr.secrecy.events;

import java.io.File;

/**
 * Created by matthew on 11/3/14.
 */
public class RestoringFileEvent {
    public final File backupFile; //Path of backupFile
    public final File restoredFile; //Path of restored file

    public RestoringFileEvent(File backupFile, File restoredFile) {
        this.backupFile = backupFile;
        this.restoredFile = restoredFile;
    }
}

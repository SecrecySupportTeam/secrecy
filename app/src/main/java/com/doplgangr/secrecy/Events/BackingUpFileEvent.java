package com.doplgangr.secrecy.Events;

/**
 * Created by matthew on 11/2/14.
 */
public class BackingUpFileEvent {
    public String folderToBackup;
    public String fileInBackup;

    public BackingUpFileEvent(String folderToBackup, String fileInBackup) {
        this.folderToBackup = folderToBackup;
        this.fileInBackup = fileInBackup;
    }
}

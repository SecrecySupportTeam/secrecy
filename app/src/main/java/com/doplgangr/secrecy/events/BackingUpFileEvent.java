package com.doplgangr.secrecy.events;

/**
 * Created by matthew on 11/2/14.
 */
public class BackingUpFileEvent {
    private final String folderToBackup;
    private final String fileInBackup;

    public BackingUpFileEvent(String folderToBackup, String fileInBackup) {
        this.folderToBackup = folderToBackup;
        this.fileInBackup = fileInBackup;
    }
}

package com.doplgangr.secrecy.Events;

import com.doplgangr.secrecy.FileSystem.Encryption.Vault;

import java.io.File;

/**
 * Created by matthew on 11/2/14.
 */
public class AddingFileDoneEvent {
    public Vault vault;    //Path of the folder to backup
    public File addingFile; //Path of backupFile

    public AddingFileDoneEvent(Vault vault, File addingFile) {
        this.vault = vault;
        this.addingFile = addingFile;
    }
}
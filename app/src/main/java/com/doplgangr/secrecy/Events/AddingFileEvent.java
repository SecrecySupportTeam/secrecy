package com.doplgangr.secrecy.Events;

import com.doplgangr.secrecy.FileSystem.Encryption.Vault;

/**
 * Created by matthew on 11/2/14.
 */
public class AddingFileEvent {
    public final Vault vaultToAdd;
    public final String fileToAdd;

    public AddingFileEvent(Vault vaultToAdd, String fileToAdd) {
        this.vaultToAdd = vaultToAdd;
        this.fileToAdd = fileToAdd;
    }
}

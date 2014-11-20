package com.doplgangr.secrecy.Events;

import com.doplgangr.secrecy.FileSystem.Encryption.Vault;

/**
 * Created by matthew on 11/2/14.
 */
public class AddingFileDoneEvent {
    public final Vault vault;    //Path of the vault file is added

    public AddingFileDoneEvent(Vault vault) {
        this.vault = vault;
    }
}
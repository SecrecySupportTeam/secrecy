package com.doplgangr.secrecy.Events;


public class DecryptingFileDoneEvent {
    public final int index;

    public DecryptingFileDoneEvent(int index){
        this.index = index;
    }
}

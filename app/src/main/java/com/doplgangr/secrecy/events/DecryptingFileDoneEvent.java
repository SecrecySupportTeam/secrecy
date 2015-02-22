package com.doplgangr.secrecy.events;


public class DecryptingFileDoneEvent {
    public final int index;

    public DecryptingFileDoneEvent(int index){
        this.index = index;
    }
}

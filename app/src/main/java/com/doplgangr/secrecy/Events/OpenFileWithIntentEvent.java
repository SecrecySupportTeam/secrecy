package com.doplgangr.secrecy.Events;

import android.content.Intent;

/**
 * Created by matthew on 2/16/15.
 */
public class OpenFileWithIntentEvent {
    public final Intent originalIntent;
    public final Intent alternativeIntent;

    public OpenFileWithIntentEvent(Intent originalIntent, Intent alternativeIntent) {
        this.originalIntent = originalIntent;
        this.alternativeIntent = alternativeIntent;
    }
}

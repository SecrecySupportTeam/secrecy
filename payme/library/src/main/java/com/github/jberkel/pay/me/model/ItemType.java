package com.github.jberkel.pay.me.model;

import java.util.Locale;

public enum ItemType {
    /**
     * normal in app purchase
     */
    INAPP,
    /**
     * subscription
     */
    SUBS,
    /**
     * unknown type
     */
    UNKNOWN;

    public static ItemType fromString(String type) {
        for (ItemType t : values()) {
            if (t.toString().equals(type)) return t;
        }
        return UNKNOWN;
    }

    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}

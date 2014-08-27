package com.doplgangr.secrecy;

import java.io.File;

public class Listeners {

    public interface switchInterface {
        void showFiles();

        void showGallery();
    }

    public static interface EmptyListener {
        void run();
    }

    public static interface FileObserverEventListener {
        void add(File file);

        void remove(File file);
    }
}

package com.doplgangr.secrecy;

/**
 * Created by Matthew on 3/29/2014.
 */
public interface CryptStateListener {
    void updateProgress(int progress);

    void setMax(int max);

    void onFailed(int statCode);

    void Finished();
}

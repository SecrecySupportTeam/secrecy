package com.doplgangr.secrecy.jobs;

import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

public class InitializeVaultJob extends Job {
    private static final int PRIORITY = 10;
    private final String vault;
    private final String key;

    public InitializeVaultJob(String vault, String key) {
        super(new Params(PRIORITY));
        this.vault = vault;
        this.key = key;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        EventBus.getDefault().post(VaultHolder.getInstance().createAndRetrieveVault(vault, key));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        throwable.printStackTrace();
        return false;
    }

}
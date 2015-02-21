package com.doplgangr.secrecy.premium;

import android.app.Activity;

import com.doplgangr.secrecy.Util;
import com.github.jberkel.pay.me.IabHelper;
import com.github.jberkel.pay.me.IabResult;
import com.github.jberkel.pay.me.listener.OnIabSetupFinishedListener;
import com.github.jberkel.pay.me.listener.QueryInventoryFinishedListener;
import com.github.jberkel.pay.me.model.Inventory;
import com.github.jberkel.pay.me.model.Purchase;

public class PremiumStateHelper {
    //Test if user is premium
    private static final String SKU_PREMIUM = "donation.package.2";
    private final PremiumListener mPremiumListener;
    // The helper object
    private IabHelper mHelper;
    // Listener that's called when we finish querying the items and subscriptions we own
    private final QueryInventoryFinishedListener mGotInventoryListener = new QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Util.log("Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
                mPremiumListener.notPremium();
                return;
            }

            // Is it a failure?
            if (result.isFailure()) {
                mPremiumListener.notPremium();
                Util.log("Failed to query inventory: " + result);
                return;
            }

            Util.log("Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            boolean mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Util.log("User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

            if (mIsPremium)
                mPremiumListener.isPremium();
            else
                mPremiumListener.notPremium();
            destroy();
            Util.log("Initial inventory query finished; enabling main UI.");
        }
    };

    public PremiumStateHelper(Activity context, final PremiumListener mPremiumListener) {
        this.mPremiumListener = mPremiumListener;
        final String KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgbAMG82/KN7DaFV3lIVtQDepcEnI+N7MZJemXnus3kkSQ0vr+veE54l7w0Meq32alRaGBabgZuPZdjA7tsQJRa47IVF/ibHLzlBqAsefVNf+ulGEqvoeeU8oHJviIXZEdRRw3KfXrxepzKU75WLFXyMl1+ssQPWbhQaY6mLQebJz5cBivY67yd09zPjxz3SN844AFssj0+dh5D4YRIV1Qr5A0VgpNxWdbiGnDFk8WjLkfjbn3sdcJ2sCrB7pOUcjWbNRXp0jtFj0UQlmNisnbRPw9bPtrbXiWW7o745NmQfjMgg/35bJqRBlKOamU57LmJfbbpQwslpQVAQiv6dZWQIDAQAB";

        // Create the helper, passing it our context and the public key to verify signatures with
        Util.log("Creating IAB helper.");
        mHelper = new IabHelper(context, KEY);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Util.log("Starting setup.");
        mHelper.startSetup(new OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Util.log("Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Util.log("Problem setting up in-app billing: " + result);
                    mPremiumListener.notPremium();
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) {
                    mPremiumListener.notPremium();
                    return;
                }

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Util.log("Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // We're being destroyed. It's important to dispose of the helper here!
    void destroy() {
        // very important:
        Util.log("Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    public interface PremiumListener {
        void isPremium();

        void notPremium();
    }

}

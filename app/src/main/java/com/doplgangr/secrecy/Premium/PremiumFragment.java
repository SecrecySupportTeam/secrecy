/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.doplgangr.secrecy.Premium;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Util;
import com.github.jberkel.pay.me.IabHelper;
import com.github.jberkel.pay.me.IabResult;
import com.github.jberkel.pay.me.listener.OnIabPurchaseFinishedListener;
import com.github.jberkel.pay.me.listener.OnIabSetupFinishedListener;
import com.github.jberkel.pay.me.listener.QueryInventoryFinishedListener;
import com.github.jberkel.pay.me.model.Inventory;
import com.github.jberkel.pay.me.model.ItemType;
import com.github.jberkel.pay.me.model.Purchase;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.activity_premium)
public class PremiumFragment extends Fragment {
    // Debug tag, for logging
    private static final String TAG = "PremiumActivity";
    private static final String SKU_PREMIUM = "donation.package.2";
    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 19283;
    @ViewById(R.id.Premium__upgrade_button)
    Button mUpgradeButton;
    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    @ViewById(R.id.Premium__progress_bar)
    ProgressBar mProgressBar;
    private ActionBarActivity context;
    //static final String SKU_PREMIUM = "android.test.purchased";
    // Does the user have the premium upgrade?
    private boolean mIsPremium = false;
    // The helper object
    private IabHelper mHelper;
    // Callback for when a purchase is finished
    private final OnIabPurchaseFinishedListener mPurchaseFinishedListener = new OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Util.log("Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                alert(getString(R.string.Error__error_processing_purchase));
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Util.log("Purchase successful.");

            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Util.log("Purchase is premium upgrade. Congratulating user.");
                mIsPremium = true;
                updateUi();
                setWaitScreen(false);
            }
        }
    };
    // Listener that's called when we finish querying the items and subscriptions we own
    private final QueryInventoryFinishedListener mGotInventoryListener = new QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Util.log("Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                alert(getString(R.string.Error__check_play_store));
                complain("Failed to query inventory: " + result);
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
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Util.log("User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            updateUi();
            if (mIsPremium) {
                Util.alert(context,
                        getString(R.string.Dialog__purchased),
                        getString(R.string.Dialog__purchase_message),
                        Util.emptyClickListener,
                        null);
            }
            setWaitScreen(false);
            Util.log("Initial inventory query finished; enabling main UI.");
        }
    };

    @AfterViews
    void onCreate() {
        context = (ActionBarActivity) getActivity();
        context.getSupportActionBar().setTitle(R.string.Page_header__donation);

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
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
                    alert(getString(R.string.Error__cannot_set_up_IAP));
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Util.log("Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // User clicked the "Upgrade to Premium" button.
    @Click(R.id.Premium__upgrade_button)
    void onUpgradeAppButtonClicked() {
        Util.log("Upgrade button clicked; launching purchase flow for upgrade.");
        setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        mHelper.launchPurchaseFlow(context, SKU_PREMIUM, ItemType.INAPP, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Util.log("onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.log("onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        //  String payload = p.getDeveloperPayload();

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
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Util.log("Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    // updates UI to reflect model
    void updateUi() {
        if (!mIsPremium) {
            mUpgradeButton.setEnabled(true);
        }
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        mProgressBar.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setNeutralButton(R.string.OK, null);
        Util.log("Showing alert dialog: " + message);
        bld.create().show();
    }
}

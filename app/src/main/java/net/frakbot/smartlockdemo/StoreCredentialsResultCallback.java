package net.frakbot.smartlockdemo;

import android.content.IntentSender;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

class StoreCredentialsResultCallback implements ResultCallback<Status> {

    private final MainActivity activity;

    StoreCredentialsResultCallback(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onResult(Status status) {
        activity.hideProgress();
        if (status.isSuccess()) {
            activity.onCredentialsStored();
        } else if (hasUserCanceled(status)) {
            activity.onSmartLockCanceled();
        } else if (status.hasResolution()) {
            resolveResult(status, MainActivity.RC_SAVE);
        } else {
            // The user must create an account or sign in manually.
            Log.e("GMS stuff", "STATUS: Unsuccessful credential request had no resolution.");
            String message = activity.getString(R.string.error_store_failure, status.getStatusMessage());
            Snackbar.make(activity.getContentRoot(), message, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private static boolean hasUserCanceled(Status status) {
        return status.getStatusCode() == CommonStatusCodes.CANCELED;
    }

    public void resolveResult(Status status, int requestCode) {
        try {
            status.startResolutionForResult(activity, requestCode);
        } catch (IntentSender.SendIntentException e) {
            Log.e("GMS stuff", "STATUS: Failed to send resolution.", e);
            Snackbar.make(activity.getContentRoot(), R.string.error_store_failure_log, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }
}

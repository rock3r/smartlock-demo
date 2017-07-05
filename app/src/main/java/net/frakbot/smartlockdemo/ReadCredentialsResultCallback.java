package net.frakbot.smartlockdemo;

import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

class ReadCredentialsResultCallback implements ResultCallback<CredentialRequestResult> {

    private final MainActivity activity;
    private final boolean retryOnInternalError;

    ReadCredentialsResultCallback(MainActivity activity, boolean retryOnInternalError) {
        this.activity = activity;
        this.retryOnInternalError = retryOnInternalError;
    }

    @Override
    public void onResult(@NonNull CredentialRequestResult result) {
        activity.hideProgress();
        Status status = result.getStatus();
        if (status.isSuccess()) {
            activity.onCredentialsRetrieved(result.getCredential());
            Snackbar.make(activity.getContentRoot(), R.string.credentials_retrieved, Snackbar.LENGTH_SHORT)
                    .show();
        } else if (hasUserCanceled(status)) {
            activity.onSmartLockCanceled();
        } else if (needsSignIn(status)) {
            activity.startSigninHintFlow();
        } else if (status.hasResolution()) {
            startResolutionFor(status);
        } else if (shouldRetryFor(status)) {
            activity.retrieveCredentialsWithoutRetrying();
        } else {
            // Request has no resolution
            String message = activity.getString(R.string.error_retrieve_failure, status.getStatusMessage());
            Snackbar.make(activity.getContentRoot(), message, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private static boolean hasUserCanceled(Status status) {
        return status.getStatusCode() == CommonStatusCodes.CANCELED;
    }

    private static boolean needsSignIn(Status status) {
        return status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED;
    }

    private void startResolutionFor(Status status) {
        try {
            status.startResolutionForResult(activity, MainActivity.RC_READ);
        } catch (IntentSender.SendIntentException e) {
            Log.e("GMS stuff", "STATUS: Failed to send resolution.", e);
            Snackbar.make(activity.getContentRoot(), R.string.error_retrieve_failure_log, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private boolean shouldRetryFor(Status status) {
        return status.getStatusCode() == CommonStatusCodes.INTERNAL_ERROR && retryOnInternalError;
    }
}

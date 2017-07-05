package net.frakbot.smartlockdemo;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

class ForgetCredentialsResultCallback implements ResultCallback<Status> {

    private final MainActivity activity;

    ForgetCredentialsResultCallback(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onResult(@NonNull Status status) {
        activity.hideProgress();
        if (status.isSuccess()) {
            activity.onCredentialsForgotten();
        } else {
            Snackbar.make(activity.getContentRoot(), R.string.error_forget_failure, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }
}

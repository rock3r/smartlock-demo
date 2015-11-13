package net.frakbot.smartlockdemo;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

class UberGmsConnectionListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final MainActivity activity;

    public UberGmsConnectionListener(MainActivity hostActivity) {
        this.activity = hostActivity;
    }

    @Override
    public void onConnected(Bundle bundle) {
        activity.onGmsConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("GMS stuff", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        ViewGroup contentRoot = activity.getContentRoot();
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, MainActivity.RC_CONNECT);
            } catch (IntentSender.SendIntentException e) {
                Log.e("GMS Stuff", "Unable to resolve connection issue", e);
                Snackbar.make(contentRoot, "GMS result resolution failed, see log", Snackbar.LENGTH_LONG)
                        .show();
            }
        } else {
            Snackbar.make(contentRoot, "GMS connection failed: " + connectionResult.getErrorCode(), Snackbar.LENGTH_LONG)
                    .show();
        }
    }

}

package net.frakbot.smartlockdemo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.concurrent.TimeUnit;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;

public class MainActivity extends AppCompatActivity {

    static final int RC_CONNECT = R.id.request_code_connection;

    static final int RC_READ = R.id.request_code_read_credentials;
    static final int RC_SAVE = R.id.request_code_save_credentials;
    private static final int RC_SIGNIN_HINT = R.id.request_code_signin;

    private static final String TAG = "SmartLockDemo";

    private static final long FAKE_SIGNIN_DELAY_MS = TimeUnit.SECONDS.toMillis(2);

    private UserPreferences preferences;

    private GoogleApiClient credentialsClient;
    private CredentialRequest credentialsRequest;
    private ViewGroup contentRoot;
    private View progressView;
    private Credential credentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        contentRoot = (ViewGroup) findViewById(R.id.content_root);
        progressView = findViewById(R.id.progress);

        preferences = UserPreferences.with(this);

        setupGms();

        findViewById(R.id.store_credentials).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeCredentials();
            }
        });

        findViewById(R.id.forget_credentials).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgetCredentials();
            }
        });

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSigninForm();
            }
        });

        credentialsClient.connect();
    }

    private void setupGms() {
        if (!shouldUseSmartLock()) {
            showSmartLockDisabledMessage();
            return;
        }

        UberGmsConnectionListener gmsListener = new UberGmsConnectionListener(this);
        credentialsClient = createCredentialsClient(this, gmsListener);
        credentialsRequest = createCredentialsRequest();
    }

    private static GoogleApiClient createCredentialsClient(Context context, UberGmsConnectionListener listener) {
        return new Builder(context)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    private static CredentialRequest createCredentialsRequest() {
        return new CredentialRequest.Builder()
                .setSupportsPasswordLogin(true)
                .build();
    }

    void storeCredentials() {
        if (!shouldUseSmartLock()) {
            showSmartLockDisabledMessage();
            return;
        }

        credentials = prepareCredentials();
        if (credentials == null) {
            Snackbar.make(contentRoot, R.string.error_store_incomplete_credentials, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        showProgress();
        Auth.CredentialsApi
                .save(credentialsClient, credentials)
                .setResultCallback(new StoreCredentialsResultCallback(this));
    }

    private boolean shouldUseSmartLock() {
        return preferences.useSmartLock();
    }

    private void showSmartLockDisabledMessage() {
        Snackbar.make(contentRoot, R.string.error_smart_lock_disabled, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.reset, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        preferences.resetUseSmartLock();
                        restartApp();
                    }
                });
    }

    private Credential prepareCredentials() {
        String username = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return null;
        }
        return new Credential.Builder(username)
                .setPassword(password)
                .build();
    }

    private void forgetCredentials() {
        if (credentials == null) {
            Snackbar.make(contentRoot, R.string.error_forget_no_credentials, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        showProgress();
        Auth.CredentialsApi
                .delete(credentialsClient, credentials)
                .setResultCallback(new ForgetCredentialsResultCallback(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        credentialsClient.disconnect();
    }

    public void onGmsConnected() {
        findViewById(R.id.store_credentials).setEnabled(true);
        ReadCredentialsResultCallback callback = new ReadCredentialsResultCallback(this, true);
        requestStoredCredentials(credentialsClient, credentialsRequest, callback);
    }

    void retrieveCredentialsWithoutRetrying() {
        ReadCredentialsResultCallback callback = new ReadCredentialsResultCallback(this, false);
        requestStoredCredentials(credentialsClient, credentialsRequest, callback);
    }

    private void requestStoredCredentials(GoogleApiClient apiClient,
                                          CredentialRequest credentialRequest,
                                          ReadCredentialsResultCallback callback) {
        showProgress();
        Auth.CredentialsApi
                .request(apiClient, credentialRequest)
                .setResultCallback(callback);
    }

    void onCredentialsRetrieved(Credential credentials) {
        this.credentials = credentials;
        String userId = credentials.getId();
        ((EditText) findViewById(R.id.username)).setText(userId);
        EditText passwordView = (EditText) findViewById(R.id.password);
        passwordView.setText(credentials.getPassword());
        passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        findViewById(R.id.forget_credentials).setEnabled(true);
        Snackbar.make(contentRoot, R.string.signing_in_with_stored_credentials, Snackbar.LENGTH_SHORT).show();
        startFakeSigninProcessFor(userId);
    }

    private void startFakeSigninProcessFor(final String userId) {
        showProgress();
        contentRoot.postDelayed(new Runnable() {
            @Override
            public void run() {
                showSignedInStateFor(userId);
                hideProgress();
            }
        }, FAKE_SIGNIN_DELAY_MS);
    }

    private void showSignedInStateFor(String userId) {
        findViewById(R.id.form_container).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.signed_in_username)).setText(userId);
        findViewById(R.id.signed_in_container).setVisibility(View.VISIBLE);
    }

    private void showSigninForm() {
        findViewById(R.id.form_container).setVisibility(View.VISIBLE);
        findViewById(R.id.signed_in_container).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_READ) {
            handleCredentialsReadResult(resultCode, data);
        } else if (requestCode == RC_SAVE) {
            handleCredentialsStoreResult(resultCode);
        } else if (requestCode == RC_CONNECT) {
            handleGmsConnectionResult(resultCode);
        } else if (requestCode == RC_SIGNIN_HINT) {
            handleSigninHint(data);
        }
    }

    private void handleCredentialsReadResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Credential credentials = data.getParcelableExtra(Credential.EXTRA_KEY);
            onCredentialsRetrieved(credentials);
        } else {
            Log.e(TAG, "Credentials read failed");
            Snackbar.make(contentRoot, R.string.error_retrieve_fatal, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void handleCredentialsStoreResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            onCredentialsStored();
        } else {
            Log.e(TAG, "SAVE: Canceled by user");
        }
    }

    private void handleGmsConnectionResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            credentialsClient.connect();
        }
    }

    private void handleSigninHint(Intent data) {
        if (containsNoCredentials(data)) {
            return;
        }
        Credential credentials = data.getParcelableExtra(Credential.EXTRA_KEY);
        ((EditText) findViewById(R.id.username)).setText(credentials.getId());
        ((EditText) findViewById(R.id.password)).setText(credentials.getGeneratedPassword());
    }

    private static boolean containsNoCredentials(Intent data) {
        return !data.hasExtra(Credential.EXTRA_KEY);
    }

    public ViewGroup getContentRoot() {
        return contentRoot;
    }

    public void startSigninHintFlow() {
        HintRequest hintRequest = createHintRequest();
        try {
            PendingIntent pendingIntent = Auth.CredentialsApi
                    .getHintPickerIntent(credentialsClient, hintRequest);
            startIntentSenderForResult(pendingIntent.getIntentSender(), RC_SIGNIN_HINT, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Signin hint launch failed", e);
            Snackbar.make(contentRoot, R.string.error_signin_hint_failure, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private static HintRequest createHintRequest() {
        CredentialPickerConfig pickerConfig = new CredentialPickerConfig.Builder()
                .setShowAddAccountButton(true)
                .setShowCancelButton(true)
                .setForNewAccount(true)
                .build();
        return new HintRequest.Builder()
                .setEmailAddressIdentifierSupported(true)
                .setHintPickerConfig(pickerConfig)
                .build();
    }

    void onCredentialsForgotten() {
        credentials = null;
        ((EditText) findViewById(R.id.username)).setText(null);
        EditText passwordView = (EditText) findViewById(R.id.password);
        passwordView.setText(null);
        passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Snackbar.make(contentRoot, R.string.credentials_forgotten, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void showProgress() {
        contentRoot.setEnabled(false);
        progressView.setVisibility(View.VISIBLE);
    }

    void hideProgress() {
        contentRoot.setEnabled(true);
        progressView.setVisibility(View.GONE);
    }

    public void onCredentialsStored() {
        Snackbar.make(contentRoot, R.string.credentials_saved, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.restart_app, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        restartApp();
                    }
                })
                .show();
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void onSmartLockCanceled() {
        preferences.setUserRefusedSmartLock();
    }

}

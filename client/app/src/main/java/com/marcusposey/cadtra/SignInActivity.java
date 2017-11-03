package com.marcusposey.cadtra;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;

/**
 * Acquires a Google Id Token from the user
 *
 * This activity can invoke an explicit sign in process wherein the user
 * presses a sign in button. It also supports silent sign-in, i.e., if
 * credentials have previously been supplied, a request to Google is made
 * which should update the stored Id Token.
 */
public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    // Extra key which indicates the activity was started solely to refresh an id token
    public static final String REFRESH_REQUEST = "REFRESH_REQUEST";

    private static final int RC_SIGN_IN = 9001;
    private final int EXPLICIT_SIGN_IN = 0;
    private final int SILENT_SIGN_IN = 1;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.PROFILE))
                .requestScopes(new Scope(Scopes.EMAIL))
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setEnabled(false);

        signIn(SILENT_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result, EXPLICIT_SIGN_IN);
        }
    }

    private void handleSignInResult(GoogleSignInResult result, int context) {
        if (result != null && result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Log.v("SignInActivity", acct.getIdToken());

            RemoteService.getInstance().setIdToken(acct.getIdToken());
            // If they just wanted an updated token, don't send them to the main activity.
            if (getIntent().getBooleanExtra(REFRESH_REQUEST, false)) finish();

            RemoteService.getInstance().getOrCreateAccount(); // todo: store response.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else if (result != null && context == SILENT_SIGN_IN) {
            // prompt explicit sign in.
            Log.i("signin", "failed silent sign in");
            findViewById(R.id.sign_in_button).setEnabled(true);
        }
        else {
            Log.v("SignInActivity", "Sign in failed; err " + result.getStatus().getStatusCode());
            Toast.makeText(getApplicationContext(), "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn(EXPLICIT_SIGN_IN);
                break;
        }
    }

    /** Sign in using a google account */
    private void signIn(int context) {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (opr != null && context == SILENT_SIGN_IN) {
            handleGooglePendingResult(opr);
        }
        else { // Start an explicit sign in process.
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    private void handleGooglePendingResult(OptionalPendingResult<GoogleSignInResult> pendingResult) {
        if (pendingResult.isDone()) {
            // There's immediate result available.
            GoogleSignInResult signInResult = pendingResult.get();
            handleSignInResult(signInResult, SILENT_SIGN_IN);
        } else {
            // There's no immediate result ready,  waits for the async callback.
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult signInResult) {
                    handleSignInResult(signInResult, SILENT_SIGN_IN);
                }
            });
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();
    }
}

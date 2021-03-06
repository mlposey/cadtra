package com.marcusposey.cadtra.activity;

import android.app.Activity;
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
import com.marcusposey.cadtra.R;
import com.marcusposey.cadtra.model.Account;
import com.marcusposey.cadtra.net.RequestFactory;
import com.marcusposey.cadtra.net.TokenStore;

/**
 * Acquires a Google Id Token from the user
 *
 * This activity can invoke an explicit sign in process wherein the user
 * presses a sign in button. It also supports silent sign-in, i.e., if
 * credentials have previously been supplied, a request to Google is made
 * which should update the stored Id Token.
 */
public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener  {
    /** Denotes the method used to carry out the sign in process */
    private enum Method { SILENT, EXPLICIT }

    // Extra key which indicates the activity was started solely to refresh an id token
    public static final String REFRESH_REQUEST = "REFRESH_REQUEST";

    // The key for the intent extra that stores a Google Id token
    public static final String TOKEN_EXTRA = "TOKEN_EXTRA";

    // Request code for a Google Sign-In activity result
    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient googleApiClient;

    /** Requests necessary sign in scopes and starts a silent sign-in process */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        googleApiClient = createApiClient();
        signIn(Method.SILENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result, Method.EXPLICIT);
        }
    }

    private void handleSignInResult(GoogleSignInResult result, final Method context) {
        if (result != null && result.isSuccess()) {
            handleSignInSuccess(result.getSignInAccount());
        }
        else if (result != null && context == Method.SILENT) {
            Log.i("SignInActivity", "Failed silent sign in");

            SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
            signInButton.setOnClickListener((view) -> signIn(Method.EXPLICIT));
            signInButton.setSize(SignInButton.SIZE_STANDARD);
            signInButton.setVisibility(View.VISIBLE);
        }
        else {
            Log.v("SignInActivity", "Sign in failed; err " + result.getStatus().getStatusCode());
            Toast.makeText(getApplicationContext(), "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignInSuccess(GoogleSignInAccount acct) {
        Log.v("SignInActivity", acct.getIdToken());

        TokenStore.getInstance().setIdToken(acct.getIdToken());

        if (!getIntent().getBooleanExtra(REFRESH_REQUEST, false)) {
            // Maybe we'll need this later to display their picture
            // or name in the UI.
            Account userAccount = new Account.Factory(new RequestFactory(this))
                    .fromNetwork();
            if (userAccount == null) {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT)
                    .show();
                return;
            }
        }

        Intent intent = new Intent();
        intent.putExtra(TOKEN_EXTRA, acct.getIdToken());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /** Sign in using a google account */
    private void signIn(final Method context) {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (opr != null && context == Method.SILENT) {
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
            handleSignInResult(signInResult, Method.SILENT);
        } else {
            // There's no immediate result ready,  waits for the async callback.
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult signInResult) {
                    handleSignInResult(signInResult, Method.SILENT);
                }
            });
        }
    }

    /**
     * Creates a GoogleApiClient for the sign in process
     * The client will request the following scopes:
     *  - Profile
     *  - Email
     * It will include access to an id token made by Google.
     */
    private GoogleApiClient createApiClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.PROFILE))
                .requestScopes(new Scope(Scopes.EMAIL))
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();
        return new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    /** Called if googleApiClient cannot establish a connection */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();
    }
}

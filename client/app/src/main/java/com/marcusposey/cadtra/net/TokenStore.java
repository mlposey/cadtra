package com.marcusposey.cadtra.net;

import android.app.Activity;
import android.content.Intent;

import com.marcusposey.cadtra.SignInActivity;

/** Starts SignInActivity to refresh and store an Id Token */
public class TokenStore extends Activity {
    // Intent tag for extras that store the id token
    public static final String TOKEN_EXTRA = "TOKEN";

    private String idToken;

    /** Gets the last token retrieved or a new one if none exists */
    public String getIdToken() {
        if (idToken.isEmpty()) refresh();
        return idToken;
    }

    /** Attempts to refresh the id token token */
    public void refresh() {
        Intent silentSignIn = new Intent(this, SignInActivity.class);
        silentSignIn.putExtra(SignInActivity.REFRESH_REQUEST, true);
        startActivityForResult(silentSignIn, 0 /* code doesn't matter */);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        idToken = data.getStringExtra(TOKEN_EXTRA);
    }
}
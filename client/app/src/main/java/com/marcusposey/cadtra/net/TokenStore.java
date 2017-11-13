package com.marcusposey.cadtra.net;

import android.app.Activity;
import android.content.Intent;

import com.marcusposey.cadtra.SignInActivity;

/** Stores the user's id token */
public class TokenStore {
    private static TokenStore instance;

    // Intent tag for extras that store the id token
    public static final String TOKEN_EXTRA = "TOKEN";

    private String idToken;

    private TokenStore() {}

    /** Returns the single instance of the user's token */
    public static synchronized TokenStore getInstance() {
        if (instance == null) instance = new TokenStore();
        return instance;
    }

    /**
     * Gets the last token retrieved or a new one if none exists
     * @return null if TokenStore is not attached to an Activity
     */
    public synchronized String getIdToken() {
        return idToken;
    }

    /** Sets the id token */
    public synchronized void setIdToken(String token) {
        idToken = token;
    }

    /** Attempts to refresh the id token */
    public synchronized void refresh(Activity parent) {
        Intent silentSignIn = new Intent(parent, SignInActivity.class);
        silentSignIn.putExtra(SignInActivity.REFRESH_REQUEST, true);
        parent.startActivity(silentSignIn);
    }
}
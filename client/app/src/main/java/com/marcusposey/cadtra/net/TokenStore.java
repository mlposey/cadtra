package com.marcusposey.cadtra.net;

import android.app.Activity;
import android.content.Intent;

import com.marcusposey.cadtra.SignInActivity;

/** Stores the user's id token */
public class TokenStore {
    private static TokenStore instance;

    // The last time the id token was refreshed
    // Id Tokens last one hour before they need to be renewed.
    private long lastRefresh;
    private final int msInHour = 3600 * 1000;

    // Intent tag for extras that store the id token
    public static final String TOKEN_EXTRA = "TOKEN";

    private String idToken;

    private TokenStore() {
        lastRefresh = System.currentTimeMillis();
    }

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

    /** Refreshes the token if it is nearing expiration */
    public synchronized void refresh(Activity parent) {
        if ((System.currentTimeMillis() - lastRefresh) < msInHour / 2) {
            // Refresh only after 30 minutes, even though the token
            // lasts an hour.
            return;
        }
        Intent silentSignIn = new Intent(parent, SignInActivity.class);
        silentSignIn.putExtra(SignInActivity.REFRESH_REQUEST, true);
        parent.startActivity(silentSignIn);
        lastRefresh = System.currentTimeMillis();
    }
}
package com.marcusposey.cadtra.net;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.marcusposey.cadtra.RunLog;
import com.marcusposey.cadtra.SignInActivity;
import com.marcusposey.cadtra.WorkoutSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Provides access to the remote service that stores/retrieves application data */
public class RemoteService {
    // Tag used for Android logging.
    private final String LOG_TAG = "REMOTE_SERVICE";

    private static RemoteService instance = null;
    // Prevents concurrent stages by locking on stage-build and unlocking
    // on stage-submit.
    private final Lock stageLock = new ReentrantLock();

    // The user's id token
    private String idToken;

    private final Gson gson = new Gson();

    private final HttpClient httpClient = new DefaultHttpClient();
    private CompletableFuture<Pair<Integer, String>> httpRequest;

    // Activity used to start a silent sign in process and display toasts
    // may be null
    private Activity activity;

    private RemoteService() {}

    /** Returns an instance of the RemoteService */
    public synchronized static RemoteService getInstance() {
        if (instance == null) instance = new RemoteService();
        return instance;
    }

    /**
     * Returns an instance of the RemoteService
     * @param activity Used for silent sign-on and toasts
     */
    public synchronized static RemoteService getInstance(Activity activity) {
        RemoteService service = getInstance();
        service.activity = activity;
        return service;
    }

    /** Sets the id token to pass to the application server */
    public synchronized void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    /** Prepares a server request to submit a run log */
    public void uploadRunLog(final WorkoutSession session) {
        stageLock.lock();
        refreshIdToken();

        httpRequest = CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = getApiURI("/users/me/logs");
                if (uri == null) return null;

                HttpPost req = new HttpPost(uri);
                RunLog log = new RunLog(session.getStartTimestampTz(), session.getEndTimestampTz(),
                                        session.getPolylinePath(), session.getDistance(),
                                        session.getSplitInterval(), session.getSplits(),
                                        session.getComment());
                writeJSONBody(req, gson.toJson(log));
                req.setHeader("Authorization", "Bearer " + idToken);

                HttpResponse resp = httpClient.execute(req);
                return getResponse(resp);
            } catch (Exception e) {
                showConErrToast();
            }
            return null;
        });
    }

    /**
     * Gets a list of all runs completed by the user
     * @return the list of runs or null if no runs have been saved
     */
    public synchronized RunLog[] getRunLogs() {
        stageLock.lock();

        httpRequest = CompletableFuture.supplyAsync(() -> {
            URI uri = getApiURI("/users/me/logs");
            if (uri == null) return null;

            HttpGet req = new HttpGet(uri);
            req.setHeader("Authorization", "Bearer " + idToken);
            try {
                // todo: put this exception stuff in the getResponse. it will probably require
                // adding the client and request as parameters.
                return getResponse(httpClient.execute(req));
            } catch (IOException e) {
                showConErrToast();
                return null;
            }
        });

        Pair<Integer, String> resp = getResponse();
        Log.i(LOG_TAG, "Server response: " + resp.second);
        if (resp == null || resp.first != 200) return new RunLog[0];

        return gson.fromJson(resp.second, RunLog[].class);
    }

    /**
     * Requests the user profile from the server and creates a new account
     * if the server indicates the profile did not exist.
     * @return the response code and response body
     */
    public synchronized Pair<Integer, String> getOrCreateAccount() {
        getAccount();
        Pair<Integer, String> resp = getResponse();
        if (resp.first == 200) return resp;
        createAccount();
        return getResponse();
    }

    /** Returns the response code and body of the last request */
    public Pair<Integer, String> getResponse() {
        Pair<Integer, String> response = null;
        try { response = httpRequest.get(); }
        catch (Exception e) { e.printStackTrace(); }

        stageLock.unlock();
        return response;
    }

    /** Creates a user account using the Google id token */
    private void createAccount() {
        stageLock.lock();

        httpRequest = CompletableFuture.supplyAsync(() -> {
            URI uri = getApiURI("/users");
            if (uri == null) return null;

            HttpPost req = new HttpPost(uri);
            req.setHeader("Authorization", "Bearer " + idToken);
            try {
                return getResponse(httpClient.execute(req));
            } catch (IOException e) {
                showConErrToast();
                return null;
            }
        });
    }

    /** Uses a Google id token to get details about the user's application account */
    private void getAccount() {
        stageLock.lock();

        httpRequest = CompletableFuture.supplyAsync(() -> {
            URI uri = getApiURI("/users/me");
            if (uri == null) return null;

            HttpGet req = new HttpGet(uri);
            req.setHeader("Authorization", "Bearer " + idToken);
            try {
                return getResponse(httpClient.execute(req));
            } catch (Exception e) {
                showConErrToast();
            }
            return null;
        });
    }

    /** Displays a toast that signals a failure to connect to the server */
    private void showConErrToast() {
        if (activity == null) return;
        Context ctx = activity.getApplicationContext();
        Toast.makeText(ctx, "Could not connect to server", Toast.LENGTH_LONG).show();
    }

    private Pair<Integer, String> getResponse(HttpResponse response) {
        try {
            String respBody = EntityUtils.toString(response.getEntity());
            return new Pair<>(response.getStatusLine().getStatusCode(), respBody);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Constructs a URI to the newest version of the REST API
     * @param resourcePath a path to a REST resource (e.g., /users/me)
     * @return the connection or null if the resourcePath was invalid
     */
    private URI getApiURI(String resourcePath) {
        final String host = "srv.marcusposey.com";
        final int port = 8000;

        try {
            return new URL(String.format("http://%s:%d/api/v1%s", host, port, resourcePath)).toURI();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Writes content to the request body of req and sets the content type to application/json
     */
    private void writeJSONBody(HttpEntityEnclosingRequestBase req, String body) {
        try {
            Log.i(LOG_TAG, body);
            StringEntity respBody = new StringEntity(body);
            req.setEntity(respBody);
            req.setHeader("Content-Type", "application/json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reuses a cached id token or acquires a new one if it expired
     * Only works if an activity has been supplied
     */
    private synchronized void refreshIdToken() {
        if (activity == null) return;
        Intent silentSignIn = new Intent(activity, SignInActivity.class);
        silentSignIn.putExtra(SignInActivity.REFRESH_REQUEST, true);
        activity.startActivity(silentSignIn);
    }
}

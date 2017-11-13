package com.marcusposey.cadtra.net;

import android.app.Activity;
import android.util.Log;

import com.google.gson.Gson;
import com.marcusposey.cadtra.RunLog;
import com.marcusposey.cadtra.WorkoutSession;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URL;

/** Creates HTTP requests that can be sent to an ApiRequest */
public class RequestFactory {
    // Stores the user's id token
    private TokenStore tokenStore = TokenStore.getInstance();
    private final Gson gson = new Gson();

    public RequestFactory(Activity parent) {
        tokenStore.refresh(parent);
    }

    /**
     * Creates a POST request containing a run log
     * @return null if the process fails
     */
    public HttpUriRequest runLogPost(final WorkoutSession session) {
        HttpPost req = null;

        try {
            final URL resource = ServerInfo.getV1ResourceURL("/users/me/logs");
            req = new HttpPost(resource.toURI());
            RunLog log = new RunLog(session.getStartTimestampTz(),
                    session.getEndTimestampTz(), session.getPolylinePath(),
                    session.getDistance(), session.getSplitInterval(),
                    session.getSplits(), session.getComment());

            writeJSONBody(req, gson.toJson(log));
            injectIdToken(req);
        } catch (Exception e) {
            Log.e(RequestFactory.class.getSimpleName(), e.getMessage());
        }

        return req;
    }

    /**
     * Creates a GET request to retrieve all run logs
     * @return null if the process fails
     */
    public HttpUriRequest runLogsGet() {
        HttpGet req = null;

        try {
            final URL resource = ServerInfo.getV1ResourceURL("/users/me/logs");
            req = new HttpGet(resource.toURI());
            injectIdToken(req);
        } catch (Exception e) {
            Log.e(RequestFactory.class.getSimpleName(), e.getMessage());
        }

        return req;
    }

    /**
     * Creates a GET request to retrieve the user's profile
     * @return null if the process fails
     */
    public HttpUriRequest accountGet() {
        HttpGet req = null;

        try {
            final URL resource = ServerInfo.getV1ResourceURL("/users/me");
            req = new HttpGet(resource.toURI());
            injectIdToken(req);
        } catch (Exception e) {
            Log.e(RequestFactory.class.getSimpleName(), e.getMessage());
        }

        return req;
    }

    public HttpUriRequest accountPost() {
        HttpPost req = null;

        try {
            final URL resource = ServerInfo.getV1ResourceURL("/users");
            req = new HttpPost(resource.toURI());
            injectIdToken(req);
        } catch (Exception e) {
            Log.e(RequestFactory.class.getSimpleName(), e.getMessage());
        }

        return req;
    }

    /**
     * Writes content to the request body of req and sets the content type to
     * application/json
     */
    private void writeJSONBody(HttpEntityEnclosingRequestBase req, String body) {
        try {
            StringEntity respBody = new StringEntity(body);
            req.setEntity(respBody);
            req.setHeader("Content-Type", "application/json");
        } catch (IOException e) {
            Log.e(RequestFactory.class.getSimpleName(), e.getMessage());
        }
    }

    /** Adds the user's id token to req */
    private void injectIdToken(HttpUriRequest req) {
        // Todo: Refresh only when necessary.
        req.setHeader("Authorization", "Bearer " + tokenStore.getIdToken());
    }
}

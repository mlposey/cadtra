package com.marcusposey.cadtra.net;

import android.os.AsyncTask;
import android.support.v4.util.Pair;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Runs an HttpUriRequest away from the UI thread
 *
 * Only one request per ApiRequest can be executed at a time. If
 * execute() is given multiple HttpUriRequest objects, the result
 * will be null.
 *
 * On Failure:
 *      null is returned if the request could not be fulfilled, possibly
 *      due to network issues.
 * On Success:
 *      A Pair containing the response code and response body is returned.
 */
public class ApiRequest extends AsyncTask<HttpUriRequest, Void, Pair<Integer, String>> {
    private final HttpClient httpClient = new DefaultHttpClient();

    @Override
    protected Pair<Integer, String> doInBackground(HttpUriRequest... params) {
        if (params.length != 1) return null;

        try {
            return getResponse(httpClient.execute(params[0]));
        } catch (Exception e) {
            // Todo: Log this.
            return null;
        }
    }

    /**
     * Returns the response of an HTTP request
     *
     * @return (Response Code, Response Body)
     */
    private Pair<Integer, String> getResponse(HttpResponse response) {
        try {
            String respBody = EntityUtils.toString(response.getEntity());
            return new Pair<>(response.getStatusLine().getStatusCode(), respBody);
        } catch (IOException e) {
            return null;
        }
    }
}

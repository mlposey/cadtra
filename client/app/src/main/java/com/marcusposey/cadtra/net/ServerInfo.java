package com.marcusposey.cadtra.net;

import java.net.MalformedURLException;
import java.net.URL;

/** Contains static information about the destination server */
public class ServerInfo {
    public static final String host = "srv.marcusposey.com";
    public static final int port = 8000;

    /**
     * Returns an HTTP URL to version 1.0 of the API
     *
     * Resource paths that are appended onto it should begin with /
     * Ex:
     *      final URL base = ServerInfo.getV1Path();
     *      URL resource = new URL(
     *          base.getProtocol(),
     *          base.getHost(),
     *          base.getPort(),
     *          base.getFile() + "/users"
     *      );
     *
     * Alternatively, use getV1ResourceURL(String).
     */
    public static URL getV1Path() {
        try {
            return new URL("http", host, port, "/api/v1");
        } catch(MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns a URL to an API resource
     *
     * @param endpoint the resource part of the full URL
     *                 Examples: /users   /accounts/32   /users/10/messages
     * @throws MalformedURLException if the endpoint:
     *                               1. is malformed under normal URL rules.
     *                               2. does not begin with '/'.
     *                               3. ends with '/'.
     */
    public static URL getV1ResourceURL(final String endpoint) throws MalformedURLException {
        if (endpoint.charAt(0) != '/' || endpoint.charAt(endpoint.length() - 1) == '/') {
            throw new MalformedURLException();
        }

        final URL base = ServerInfo.getV1Path();
        return new URL(
                base.getProtocol(),
                base.getHost(),
                base.getPort(),
                base.getFile() + endpoint
        );
    }
}

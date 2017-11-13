package com.marcusposey.cadtra;

import com.marcusposey.cadtra.net.ServerInfo;

import junit.framework.Assert;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

/** Tests for com.marcusposey.cadtra.net.ServerInfo */
public class ServerInfoTest {
    @Test
    public void getV1ResourceURL_isValid() throws MalformedURLException{
        final String resource = "/users";
        final URL expected = new URL(String.format("http://%s:%d/api/v1%s",
                ServerInfo.host, ServerInfo.port, resource));
        final URL actual = ServerInfo.getV1ResourceURL(resource);

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void getV1ResourceURL_isMalformed() {
        for (String invalidEndpoint : new String[]{"users", "/users/"}) {
            try {
                ServerInfo.getV1ResourceURL(invalidEndpoint);
                Assert.fail("Expected MalformedURLException");
            } catch (MalformedURLException e) {
                // We want this to happen.
            }
        }
    }
}

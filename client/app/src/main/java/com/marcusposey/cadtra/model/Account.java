package com.marcusposey.cadtra.model;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.marcusposey.cadtra.net.ApiRequest;
import com.marcusposey.cadtra.net.RequestFactory;

/** Stores information about the user */
public class Account {
    // A unique id given to the user, not a Google Id
    private long id;
    private String email;
    private String name;
    @SerializedName("avatar")
    private String avatarURL;
    // A timestamp with timezone that indicates when the
    // account was first created.
    private String since;
    private String country;

    /** Creates Account objects using data from various sources */
    public static class Factory {
        private RequestFactory req;

        public Factory(RequestFactory req) {
            this.req = req;
        }

        /**
         * Retrieves an existing account from the server
         * One is created if the user is new.
         * @return null if an account cannot be retrieved
         */
        @Nullable
        public Account fromNetwork() {
            Gson gson = new Gson();
            try {
                Pair<Integer, String> resp = new ApiRequest()
                        .execute(req.accountGet())
                        .get();
                if (resp.first == 200) {
                    // 200 == account exists and is in the response body
                    return gson.fromJson(resp.second, Account.class);
                }
                resp = new ApiRequest()
                        .execute(req.accountPost())
                        .get();
                if (resp.first == 201) {
                    // 200 == account was created and is in response body
                    return gson.fromJson(resp.second, Account.class);
                }
            } catch (Exception e) {
                Log.e(Factory.class.getSimpleName(), e.getMessage());
            }
            return null;
        }

        // Todo: fromLocalCache()
    }
}

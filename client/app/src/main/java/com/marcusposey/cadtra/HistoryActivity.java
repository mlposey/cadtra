package com.marcusposey.cadtra;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.marcusposey.cadtra.net.ApiRequest;
import com.marcusposey.cadtra.net.RequestFactory;

/** Displays a list of all previous runs */
public class HistoryActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RunLog[] logs = retrieveHistory();
        if (logs == null) {
            Context ctx = getApplicationContext();
            Toast.makeText(ctx, "Run history is empty", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            RunLogAdapter adapter = new RunLogAdapter(this, logs);
            setListAdapter(adapter);
        }
    }

    /**
     * Retrieves the user's run history from the server
     *
     * This is a blocking request which performs network calls on
     * a separate thread.
     */
    @Nullable
    private RunLog[] retrieveHistory() {
        RequestFactory factory = new RequestFactory();
        try {
            Pair<Integer, String> resp = new ApiRequest()
                    .execute(factory.runLogsGet())
                    .get();
            if (resp == null || resp.first != 200) return null;
            return new Gson().fromJson(resp.second, RunLog[].class);
        } catch (Exception e) {
            Log.e(HistoryActivity.class.getSimpleName(), e.getMessage());
            return null;
        }
    }
}

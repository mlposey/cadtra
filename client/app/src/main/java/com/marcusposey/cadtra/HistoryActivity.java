package com.marcusposey.cadtra;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.marcusposey.cadtra.net.RemoteService;

/** Displays a list of all previous runs */
public class HistoryActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // We won't do a refresh of the token because it will cause this activity to crash.
        // todo: perform a token refresh using getInstance(this)
        RunLog[] logs = RemoteService.getInstance().getRunLogs();
        if (logs == null) {
            Context ctx = getApplicationContext();
            Toast.makeText(ctx, "Run history is empty", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            RunLogAdapter adapter = new RunLogAdapter(this, logs);
            setListAdapter(adapter);
        }
    }

}

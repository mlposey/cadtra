package com.marcusposey.cadtra;

import android.graphics.Color;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.marcusposey.cadtra.net.RemoteService;

public class ResultsActivity extends AppCompatActivity {
    private final String LOG_TAG = "RESULTS_ACTIVITY";

    public static final String DISTANCE_EXTRA = "DISTANCE";
    public static final String TIME_EXTRA = "TIME";
    public static final String PACE_EXTRA = "PACE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Bundle extras = getIntent().getExtras();
        ((TextView) findViewById(R.id.distanceView)).setText(extras.getString(DISTANCE_EXTRA));
        ((TextView) findViewById(R.id.timeView)).setText(extras.getString(TIME_EXTRA));
        ((TextView) findViewById(R.id.paceView)).setText(extras.getString(PACE_EXTRA));

        findViewById(R.id.saveButton).setBackgroundColor(Color.GREEN);
    }

    /** Save the information about the run */
    public void onSaveRunPressed(View view) {
        // Todo: Find out if the http request is made here or when calling upload.
        Pair<Integer, String> serverResult = RemoteService.getInstance().getResponse();
        Log.i(LOG_TAG, String.valueOf(serverResult.first));
        Log.i(LOG_TAG, serverResult.second);
        finish();
    }
}

package com.marcusposey.cadtra.activity;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.marcusposey.cadtra.R;

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
    }

    /** Save the information about the run */
    public void onDonePressed(View view) {
        finish();
    }
}

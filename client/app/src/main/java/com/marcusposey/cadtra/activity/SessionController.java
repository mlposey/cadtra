package com.marcusposey.cadtra.activity;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.marcusposey.cadtra.R;
import com.marcusposey.cadtra.model.Stopwatch;
import com.marcusposey.cadtra.model.WorkoutSession;
import com.marcusposey.cadtra.net.ApiRequest;
import com.marcusposey.cadtra.net.RequestFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/** Responds to user button input by triggering state change in ActiveSessionFragment */
public class SessionController {
    @FunctionalInterface
    public interface CompletionListener {
        /**
         * Called after the user ends the session and the data has been uploaded
         * to the remote server.
         * @param e Not null if the upload failed; null otherwise
         */
        void onPostNetRequest(@Nullable Exception e);
    }

    // UTC timestamp of when the current session started
    private String startTimestamptz;

    private ActiveSessionFragment parent;
    private Stopwatch stopwatch;
    private CompletionListener listener;

    public SessionController(ActiveSessionFragment parent, Stopwatch stopwatch,
                             CompletionListener listener) {
        this.parent = parent;
        this.stopwatch = stopwatch;
        this.listener = listener;
    }

    /** Pauses or resumes the current session */
    public void onPauseResume(View view) {
        Button button = (Button) view;
        if (button.getText() == parent.getString(R.string.Pause) && stopwatch.isRunning()) {
            button.setText(parent.getString(R.string.Resume));
            stopwatch.stop();
        }
        else if (button.getText() == parent.getString(R.string.Resume) && !stopwatch.isRunning()) {
            button.setText(parent.getString(R.string.Pause));
            stopwatch.start();
        }
    }

    /** Starts a run if none is in progress or stops the current one and prompts to save results */
    public void onStartStop(View view) {
        Button button = (Button) view;
        if (button.getText() == parent.getString(R.string.Start)) {
            startTimestamptz = new DateTime(DateTimeZone.UTC).toString();
            Log.i("time", startTimestamptz);
            stopwatch.start();
            button.setText(parent.getString(R.string.Stop));
            button.setBackgroundColor(Color.RED);
            return;
        }

        uploadSession();
    }

    /** Uploads session results to the remote server */
    private void uploadSession() {
        stopwatch.stop();

        WorkoutSession session = new WorkoutSession.Builder()
                .addTimeSegment(startTimestamptz, new DateTime(DateTimeZone.UTC).toString())
                .addRoute(parent.getPath().getPoints())
                .calculcateDistanceMiles()
                .build();

        RequestFactory factory = new RequestFactory(parent.getActivity());
        try {
            new ApiRequest().execute(factory.runLogPost(session)).get();
            listener.onPostNetRequest(null);
        } catch (Exception e) {
            listener.onPostNetRequest(e);
        }
    }
}

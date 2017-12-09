package com.marcusposey.cadtra.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.marcusposey.cadtra.R;
import com.marcusposey.cadtra.model.Stopwatch;
import com.marcusposey.cadtra.model.WorkoutSession;
import com.marcusposey.cadtra.net.ApiRequest;
import com.marcusposey.cadtra.net.RequestFactory;
import com.marcusposey.cadtra.net.TokenStore;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Observable;
import java.util.Observer;

/** Handles the current run session */
public class ActiveSessionFragment extends Fragment implements LocationListener, OnMapReadyCallback,
        Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    // UTC timestamp of when the current session started
    private String startTimestamptz;

    // Keeps track of time since starting the session, excluding paused breaks
    private final Stopwatch stopwatch = new Stopwatch();

    // The map on which a route is drawn and user's location is shown
    private GoogleMap routeMap;
    private GoogleApiClient googleApiClient;

    // A path of LatLng cords that indicate where the runner has traveled
    private PolylineOptions path = new PolylineOptions();

    private Activity activity;

    public ActiveSessionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();

        View rootView = inflater.inflate(R.layout.fragment_active_session, container, false);
        initButtons(rootView);

        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        stopwatch.addObserver(this);
        buildGoogleApiClient();

        return rootView;
    }

    private void initButtons(View view) {
        Button startStop = view.findViewById(R.id.startStopButton);
        startStop.setBackgroundColor(Color.GREEN);
        startStop.setOnClickListener(this);
        Button pauseResume = view.findViewById(R.id.pauseResumeButton);
        pauseResume.setBackgroundColor(Color.YELLOW);
        pauseResume.setOnClickListener(this);
    }

    /** Builds a Google API client that is necessary to access location services */
    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    /** Starts polling location data on a set interval */
    @SuppressLint("MissingPermission") // Permissions are acquired in MainActivity.
    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                locationRequest, this);
    }

    /** Creates a blank map canvas to draw a route on */
    @SuppressLint("MissingPermission") // Permissions are acquired in MainActivity.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        routeMap = googleMap;
        routeMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        routeMap.setMinZoomPreference(16);
        routeMap.getUiSettings().setMyLocationButtonEnabled(false);
        routeMap.setMyLocationEnabled(true);
    }

    /**
     * Records the current location and updates the user's position on the map
     *
     * If a session is in progress, the location is added to the route on the map.
     */
    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (stopwatch.isRunning()) {
            path.add(latLng);
            double milesTraveled = SphericalUtil.computeLength(path.getPoints()) * 0.00062137;

            TextView distanceDisplay = getActivity().findViewById(R.id.distanceDisplay);
            distanceDisplay.setText(String.format("%.2f", milesTraveled));

            routeMap.clear();
            routeMap.addPolyline(path);
        }

        routeMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        routeMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }

    /** Pauses or resumes the current session */
    public void onPauseResume(Button button) {
        if (button.getText() == getString(R.string.Pause) && stopwatch.isRunning()) {
            button.setText(getString(R.string.Resume));
            stopwatch.stop();
        }
        else if (button.getText() == getString(R.string.Resume) && !stopwatch.isRunning()){
            button.setText(getString(R.string.Pause));
            stopwatch.start();
        }
    }

    /** Starts a run if none is in progress or stops the current one and prompts to save results */
    public void onStartStop(Button button) {
        Log.i("ActiveSessionFragment", "start/stop pressed");
        if (button.getText() == getString(R.string.Start)) {
            startTimestamptz = new DateTime(DateTimeZone.UTC).toString();
            Log.i("time", startTimestamptz);
            stopwatch.start();
            button.setText(getString(R.string.Stop));
            button.setBackgroundColor(Color.RED);
            return;
        }

        finishSession(button);
    }

    /** Collects session results and prompts the user to act on them */
    private void finishSession(Button startStop) {
        stopwatch.stop();

        WorkoutSession session = new WorkoutSession.Builder()
                .addTimeSegment(startTimestamptz, new DateTime(DateTimeZone.UTC).toString())
                .addRoute(path.getPoints())
                .calculcateDistanceMiles()
                .build();

        RequestFactory factory = new RequestFactory(getActivity());
        try {
            new ApiRequest().execute(factory.runLogPost(session)).get();
        } catch (Exception e) {
            Log.e(MainActivity.class.getName(), e.getMessage());
        }

        displaySessionResults();
        resetSession();
    }

    /** Gathers results from the completed session and displays them in their own activity */
    private void displaySessionResults() {
        Intent intent = new Intent(activity, ResultsActivity.class);
        intent.putExtra(ResultsActivity.DISTANCE_EXTRA,
                ((TextView) activity.findViewById(R.id.distanceDisplay)).getText().toString());
        intent.putExtra(ResultsActivity.TIME_EXTRA,
                ((TextView) activity.findViewById(R.id.timeDisplay)).getText().toString());
        intent.putExtra(ResultsActivity.PACE_EXTRA,
                ((TextView) activity.findViewById(R.id.paceDisplay)).getText().toString());

        // Calling refresh from the ListActivity results in a crash, so
        // we'll just do it here.
        TokenStore.getInstance().refresh(activity);
        startActivity(intent);
    }

    /** Clears current session progress */
    private void resetSession() {
        stopwatch.reset();
        path = new PolylineOptions();
        routeMap.clear();

        TextView distanceView = activity.findViewById(R.id.distanceDisplay);
        distanceView.setText("0.00"); // todo: use a string resource

        Button startStop = activity.findViewById(R.id.startStopButton);
        startStop.setText(getString(R.string.Start));
        startStop.setBackgroundColor(Color.GREEN);

        Button pauseResume = activity.findViewById(R.id.pauseResumeButton);
        pauseResume.setText(getString(R.string.Pause));
    }

    /** Observer method that gets time updates from stopwatch */
    @Override
    public void update(Observable o, final Object arg) {
        activity.runOnUiThread(() -> {
            TextView timeDisplay = activity.findViewById(R.id.timeDisplay);
            timeDisplay.setText((String) arg);

            final double milesTraveled = SphericalUtil.computeLength(path.getPoints()) * 0.00062137;
            final double secondsTaken = stopwatch.elapsedSeconds();
            final String minutesPerMile = Stopwatch.convertTime(secondsTaken / milesTraveled);
            TextView paceDisplay = activity.findViewById(R.id.paceDisplay);
            paceDisplay.setText(milesTraveled < 0.001 ? "-" : minutesPerMile);
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startStopButton:
                onStartStop((Button) v);
                break;

            case R.id.pauseResumeButton:
                onPauseResume((Button) v);
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onConnectionSuspended(int i) {}
}

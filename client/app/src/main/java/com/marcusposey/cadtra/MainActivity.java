package com.marcusposey.cadtra;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;

/**
 * This activity displays information and controls for the current running session.
 *
 * Because the core of the app relies on geotracking, MainActivity must have fine location
 * permissions. The application will exit if a request for them is made but the user denies.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback, Observer {
    // Unique id for checking permission request results; see onPermissionRequestResult
    private final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    // UTC timestamp of when the current session started
    private String startTimestamptz;

    // Only access this variable through getClient() to ensure a valid state.
    private GoogleApiClient googleApiClient;
    // If this can be acquired, all necessary permissions have been granted and googleApiClient
    // has been built. Not to be used directly unless in onCreate and getClient()
    // Todo: Use a ReentrantLock instead.
    private final Semaphore apiClientAccess = new Semaphore(1);

    // The map on which a route is drawn and user's location is shown
    private GoogleMap routeMap;

    // A path of LatLng cords that indicate where the runner has traveled
    private PolylineOptions path = new PolylineOptions();

    // Keeps track of time since starting the session, excluding paused breaks
    private final Stopwatch stopwatch = new Stopwatch();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try { // Discourage api client access until permissions are sorted out.
            apiClientAccess.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        findViewById(R.id.startStopButton).setBackgroundColor(Color.GREEN);
        findViewById(R.id.pauseResumeButton).setBackgroundColor(Color.YELLOW);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        stopwatch.addObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.historyItem) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
        // They may have come from the Sign-In page, so we should not allow them to return there.
    }

    /** Creates a blank map canvas and then prompts for permissions to finish creation */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Todo: Prompt user to turn on location setting if it is off.

        routeMap = googleMap;
        routeMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        routeMap.setMinZoomPreference(16);
        routeMap.getUiSettings().setMyLocationButtonEnabled(false);

        requestLocationPermission();
    }

    /** Builds a Google API client that is necessary to access location services */
    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
        apiClientAccess.release(); // The client can now be accessed by getClient().
    }

    /** Returns a configured GoogleApiClient or blocks if it is still being created */
    private GoogleApiClient getClient() {
        if (!apiClientAccess.tryAcquire()) {
            // Wait for setup to complete.
            try { apiClientAccess.acquire(); } catch (InterruptedException e) {}
            apiClientAccess.release();
        }
        return googleApiClient;
    }

    /** Starts polling location data on a set interval */
    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(getClient(), locationRequest, this);
        }
    }

    /** Requests permission to access fine-grained location services */
    private void requestLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            // They approved the request previously.
            routeMap.setMyLocationEnabled(true);
            buildGoogleApiClient();
        }
    }

    /**
     * Checks the status of a permission request
     *
     * PERMISSIONS_REQUEST_FINE_LOCATION:
     * If approved, a Google API client is built; otherwise, the application exits.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION:
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish(); // exit the app; location services are required.
                    // Todo: Explain importance of location permissions before attempting to quit.
                }
                routeMap.setMyLocationEnabled(true);
                buildGoogleApiClient();
                break;
        }
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

            TextView distanceDisplay = (TextView) findViewById(R.id.distanceDisplay);
            distanceDisplay.setText(String.format("%.2f", milesTraveled));

            routeMap.clear();
            routeMap.addPolyline(path);
        }

        //move map camera
        routeMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        routeMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }

    /** Pauses or resumes the current session */
    public void onPauseResumeClicked(View view) {
        Button button = (Button) view;
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
    public void toggleStartStop(View view) {
        Button button = (Button) view;
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

        RemoteService.getInstance(this).uploadRunLog(session);
        displaySessionResults();
        resetSession();
    }

    /** Gathers results from the completed session and displays them in their own activity */
    private void displaySessionResults() {
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putExtra(ResultsActivity.DISTANCE_EXTRA,
                ((TextView) findViewById(R.id.distanceDisplay)).getText().toString());
        intent.putExtra(ResultsActivity.TIME_EXTRA,
                ((TextView) findViewById(R.id.timeDisplay)).getText().toString());
        intent.putExtra(ResultsActivity.PACE_EXTRA,
                ((TextView) findViewById(R.id.paceDisplay)).getText().toString());

        startActivity(intent);
    }

    /** Clears current session progress */
    private void resetSession() {
        stopwatch.reset();
        path = new PolylineOptions();
        routeMap.clear();

        TextView distanceView = (TextView)findViewById(R.id.distanceDisplay);
        distanceView.setText("0.00"); // todo: use a string resource

        Button startStop = (Button) findViewById(R.id.startStopButton);
        startStop.setText(getString(R.string.Start));
        startStop.setBackgroundColor(Color.GREEN);

        Button pauseResume = (Button) findViewById(R.id.pauseResumeButton);
        pauseResume.setText(getString(R.string.Pause));
    }

    /** Observer method that gets time updates from stopwatch */
    @Override
    public void update(Observable o, final Object arg) {
        runOnUiThread(() -> {
            TextView timeDisplay = (TextView) findViewById(R.id.timeDisplay);
            timeDisplay.setText((String) arg);

            final double milesTraveled = SphericalUtil.computeLength(path.getPoints()) * 0.00062137;
            final double secondsTaken = stopwatch.elapsedSeconds();
            final String minutesPerMile = Stopwatch.convertTime(secondsTaken / milesTraveled);
            TextView paceDisplay = (TextView) findViewById(R.id.paceDisplay);
            paceDisplay.setText(milesTraveled < 0.001 ? "-" : minutesPerMile);
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // todo
    }

    @Override
    public void onConnectionSuspended(int i) {
        // todo
    }
}

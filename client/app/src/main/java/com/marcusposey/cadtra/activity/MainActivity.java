package com.marcusposey.cadtra.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.view.Menu;
import android.view.MenuItem;

import com.marcusposey.cadtra.R;

/**
 * Manages app permissions and the active session fragment
 *
 * Because the core of the app relies on geotracking, MainActivity must have fine location
 * permissions. The application will exit if a request for them is made but the user denies.
 */
public class MainActivity extends AppCompatActivity {
    // Unique id for checking permission request results; see onPermissionRequestResult
    private final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private ActiveSessionFragment activeSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent signIn = new Intent(this, SignInActivity.class);
        startActivity(signIn);

        requestLocationPermission();
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

    /** Requests permission to access fine-grained location services */
    private void requestLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            loadActiveSession();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
                // Todo: Explain importance of location permissions before attempting to quit.
            }

            loadActiveSession();
        }
    }

    /** Loads the active session into the content frame */
    private void loadActiveSession() {
        if (activeSession == null) activeSession = new ActiveSessionFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, activeSession)
                .commit();
    }
}

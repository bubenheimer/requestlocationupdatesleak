/*
 * Copyright (c) 2015-2021 Uli Bubenheimer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bubenheimer.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public final class MainActivity extends Activity {
    private static final String TAG = Activity.class.getSimpleName();

    private static final int RC = 123456;

    private static final class LocationListener extends LocationCallback {
        private final byte[] bigalloc = new byte[50_000_000];

        @Override
        protected void finalize() {
            Log.d(TAG, "locationListener finalized");
        }

        @Override
        public void onLocationResult(
                final LocationResult locationResult
        ) {
            Log.d(TAG, "Received location result " + locationResult);
            // Random code to keep bigalloc allocated
            Log.d(TAG, "bigalloc size " + bigalloc.length);
        }
    }

    private LocationCallback currentCallback;

    @Override
    protected void onCreate(
            final Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);
    }

    public void requestLocationUpdates(
            final View view
    ) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            reallyRequestLocationUpdates();
        } else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, RC);
        }
    }

    @SuppressLint("MissingPermission")
    public void reallyRequestLocationUpdates(
    ) {
        forceGC();

        currentCallback = new LocationListener();

        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5_000L)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, currentCallback, null)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Location updates requested successfully");
                    } else {
                        Log.d(TAG, "Location updates request failed", task.getException());
                    }
                });
    }

    public void removeLocationUpdates(
            final View view
    ) {
        if (currentCallback == null) {
            return;
        }

        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(currentCallback)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Location updates removed successfully");
                        currentCallback = null;
                        forceGC();
                    } else {
                        Log.d(TAG, "Location updates removal request failed",
                                task.getException());
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            final String[] permissions,
            final int[] grantResults
    ) {
        if (requestCode == RC && grantResults.length >= 1 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                reallyRequestLocationUpdates();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void forceGC() {
        final Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < 2; ++i) {
            runtime.gc();
            //noinspection BusyWait
            SystemClock.sleep(500L);
            runtime.runFinalization();
            //noinspection BusyWait
            SystemClock.sleep(500L);
        }
    }
}

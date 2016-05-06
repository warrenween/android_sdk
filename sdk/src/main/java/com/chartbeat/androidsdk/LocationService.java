package com.chartbeat.androidsdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.LinkedHashMap;

/**
 * Created by Mike Dai Wang on 2016-02-09.
 */
final class LocationService {
    private static final String TAG = LocationService.class.getSimpleName();

    private static final long LOCATION_VALID_WINDOW = 1 * 60 * 60 * 1000;

    private Location location;

    void updateLocation(Context context) {
        if (!isLocationPermissionEnabled(context)) {
            Logger.w(TAG, "Location unavailable. Try requesting ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Location lastKnownNetworkLocation = getLocation(LocationManager.NETWORK_PROVIDER, locationManager);
        Location lastKnownGPSLocation = getLocation(LocationManager.GPS_PROVIDER, locationManager);

        if (lastKnownNetworkLocation == null && lastKnownGPSLocation == null) {
            location = null;
        } else if (lastKnownNetworkLocation == null) {
            location = lastKnownGPSLocation;
        } else if (lastKnownGPSLocation == null) {
            location = lastKnownNetworkLocation;
        } else {
            if (lastKnownGPSLocation.getAccuracy() < lastKnownNetworkLocation.getAccuracy()) {
                location = lastKnownGPSLocation;
            } else {
                location = lastKnownNetworkLocation;
            }
        }
    }

    @SuppressWarnings({"ResourceType"})
    private Location getLocation(String provider, LocationManager locationManager) {
        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(provider);

            if (location != null) {
                long validPeriod = System.currentTimeMillis() - location.getTime();
                if (validPeriod > LOCATION_VALID_WINDOW) {
                    location = null;
                }
            }
        } catch (SecurityException se) {
            Logger.w(TAG, "Location unavailable. Try requesting ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION");
        }

        return location;
    }

    private static boolean isLocationPermissionEnabled(Context context) {
        int coarseLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int fineLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        return coarseLocation == PackageManager.PERMISSION_GRANTED
                || fineLocation == PackageManager.PERMISSION_GRANTED;
    }

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        if (location != null) {
            params.put(QueryKeys.LONGITUDE, String.valueOf(location.getLongitude()));
            params.put(QueryKeys.LATITUDE, String.valueOf(location.getLatitude()));
        }

        return params;
    }
}

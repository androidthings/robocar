/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.controllablething.companion;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.androidthings.controllablething.shared.NearbyConnectionManager;
import com.example.androidthings.controllablething.shared.model.AdvertisingInfo;
import com.example.androidthings.controllablething.shared.model.RobocarEndpoint;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class RobocarDiscoverer extends NearbyConnectionManager implements ConnectionCallbacks {

    private static final String TAG = "RobocarDiscoverer";

    private final Map<String, RobocarEndpoint> mEndpointMap = new LinkedHashMap<>();
    private RobocarEndpoint mConnectingEndpoint;

    private MutableLiveData<Boolean> mDiscoveryLiveData;
    private MutableLiveData<List<RobocarEndpoint>> mRobocarEndpointsLiveData;
    private MutableLiveData<RobocarEndpoint> mConnectedRobocarLiveData;

    // Discovery
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId,
                        DiscoveredEndpointInfo discoveredEndpointInfo) {
                    onNearbyEndpointFound(endpointId, discoveredEndpointInfo);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    onNearbyEndpointLost(endpointId);
                }
            };

    public RobocarDiscoverer(GoogleApiClient client) {
        super(client);
        client.registerConnectionCallbacks(this);

        mDiscoveryLiveData = new MutableLiveData<>();
        mDiscoveryLiveData.setValue(false);
        mRobocarEndpointsLiveData = new MutableLiveData<>();
        mConnectedRobocarLiveData = new MutableLiveData<>();
    }

    // For observers

    public LiveData<Boolean> getDiscoveryLiveData() {
        return mDiscoveryLiveData;
    }

    public LiveData<List<RobocarEndpoint>> getRobocarEndpointsLiveData() {
        return mRobocarEndpointsLiveData;
    }

    public LiveData<RobocarEndpoint> getConnectedRobocarLiveData() {
        return mConnectedRobocarLiveData;
    }

    // Discovery

    public void startDiscovery() {
        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Google Api Client not connected");
            return;
        }
        if (mDiscoveryLiveData.getValue()) {
            Log.d(TAG, "startDiscovery already called");
            return;
        }

        // Pre-emptively set this so the check above catches calls while we wait for a result.
        mDiscoveryLiveData.setValue(true);
        Nearby.Connections.startDiscovery(mGoogleApiClient, SERVICE_ID, mEndpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY)).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Discovery started.");
                    mDiscoveryLiveData.setValue(true);
                } else {
                    Log.d(TAG, String.format("Failed to start discovery. %d, %s",
                            status.getStatusCode(), status.getStatusMessage()));
                    mDiscoveryLiveData.setValue(false);
                }
            }
        });
    }

    public void stopDiscovery() {
        if (mDiscoveryLiveData.getValue()) {
            mDiscoveryLiveData.setValue(false);
            // if we're not connected, we already should have lost discovery
            if (mGoogleApiClient.isConnected()) {
                Nearby.Connections.stopDiscovery(mGoogleApiClient);
            }
            clearEndpoints();
        }
    }

    // Google API connection

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        stopDiscovery();
        clearEndpoints();
    }

    // Nearby connection

    private void onNearbyEndpointFound(String endpointId, DiscoveredEndpointInfo endpointInfo) {
        AdvertisingInfo info = AdvertisingInfo.parseAdvertisingName(endpointInfo.getEndpointName());
        if (info != null) {
            mEndpointMap.put(endpointId, new RobocarEndpoint(endpointId, info, null));
            onEndpointsChanged();
        }
    }

    private void onNearbyEndpointLost(String endpointId) {
        mEndpointMap.remove(endpointId);
        onEndpointsChanged();
    }

    public void requestConnection(String endpointId) {
        if (mConnectingEndpoint != null) {
            // We're already connecting to something else
            return;
        }
        RobocarEndpoint endpoint = mEndpointMap.get(endpointId);
        if (endpoint == null) {
            // Not a valid ID
            return;
        }

        mConnectingEndpoint = endpoint;
        Nearby.Connections.requestConnection(mGoogleApiClient, null, endpointId, mLifecycleCallback)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Requested connection.");
                        } else {
                            Log.d(TAG, String.format("Request connection failed. %d %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                            clearRobocarConnection();
                        }
                    }
                });
    }

    @Override
    protected void onNearbyConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
        super.onNearbyConnectionInitiated(endpointId, connectionInfo);
        if (connectionInfo.isIncomingConnection() || !mEndpointMap.containsKey(endpointId)) {
            // neither of these should ever happen...
            rejectConnection(endpointId);
            return;
        }
        if (!isConnectingTo(endpointId)) {
            // We're already connecting to something else
            rejectConnection(endpointId);
            return;
        }
        // TODO callback to show auth UI
    }

    public void acceptConnection(final String endpointId) {
        if (!isConnectingTo(endpointId)) {
            return;
        }

        Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mInternalPayloadListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Accepted connection.");
                        } else {
                            Log.d(TAG, String.format("Accept unsuccessful. %d %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                        }
                    }
                });
    }

    public void rejectConnection(final String endpointId) {
        if (!isConnectingTo(endpointId)) {
            return;
        }

        Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Rejected connection.");
                        } else {
                            Log.d(TAG, String.format("Reject unsuccessful. %d %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                        }
                    }
                });
    }

    @Override
    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        super.onNearbyConnected(endpointId, connectionResolution);
        stopDiscovery();
        clearEndpoints();
    }

    @Override
    protected void onNearbyDisconnected(String endpointId) {
        super.onNearbyDisconnected(endpointId);
        if (isConnectingTo(endpointId)) {
            clearRobocarConnection();
            startDiscovery();
        }
    }

    private boolean isConnectingTo(String endpointId) {
        return mConnectingEndpoint != null && mConnectingEndpoint.mEndpointId.equals(endpointId);
    }

    private void clearRobocarConnection() {
        if (mConnectingEndpoint != null) {
            mConnectingEndpoint = null;
        }
    }

    private void clearEndpoints() {
        mEndpointMap.clear();
        onEndpointsChanged();
    }

    private void onEndpointsChanged() {
        mRobocarEndpointsLiveData.setValue(new ArrayList<>(mEndpointMap.values()));
    }
}

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
package com.example.androidthings.robocar.companion;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.androidthings.robocar.shared.NearbyConnection.ConnectionState;
import com.example.androidthings.robocar.shared.NearbyConnectionManager;
import com.example.androidthings.robocar.shared.PreferenceUtils;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.example.androidthings.robocar.shared.model.DiscovererInfo;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
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
    private DiscovererInfo mDiscovererInfo;
    private AdvertisingInfo mPairedAdvertisingInfo;

    private boolean mAutoConnectEnabled = true;

    private MutableLiveData<Boolean> mDiscoveryLiveData;
    private MutableLiveData<List<RobocarEndpoint>> mRobocarEndpointsLiveData;
    private MutableLiveData<RobocarConnection> mRobocarConnectionLiveData;

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
        mRobocarConnectionLiveData = new MutableLiveData<>();
    }

    public void setDiscovererInfo(DiscovererInfo info) {
        // This is only checked when we request a connection, and we don't need to interrupt one
        // already in progress.
        mDiscovererInfo = info;
    }

    public void setPairedAdvertisingInfo(AdvertisingInfo info) {
        mPairedAdvertisingInfo = info;
    }

    // For observers

    public LiveData<Boolean> getDiscoveryLiveData() {
        return mDiscoveryLiveData;
    }

    public LiveData<List<RobocarEndpoint>> getRobocarEndpointsLiveData() {
        return mRobocarEndpointsLiveData;
    }

    public LiveData<RobocarConnection> getRobocarConnectionLiveData() {
        return mRobocarConnectionLiveData;
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
        clearRobocarConnection();
        clearEndpoints();
    }

    // Nearby connection

    private void onNearbyEndpointFound(String endpointId, DiscoveredEndpointInfo endpointInfo) {
        AdvertisingInfo info = AdvertisingInfo.parseAdvertisingName(endpointInfo.getEndpointName());
        if (info != null) {
            boolean isRemembered = isTheDroidWeAreLookingFor(info);
            mEndpointMap.put(endpointId, new RobocarEndpoint(endpointId, info, isRemembered));
            onEndpointsChanged();

            if (isRemembered && mAutoConnectEnabled) {
                // try to auto-connect
                requestConnection(endpointId);
            }
        }
    }

    private void onNearbyEndpointLost(String endpointId) {
        mEndpointMap.remove(endpointId);
        onEndpointsChanged();
    }

    public void requestConnection(String endpointId) {
        if (mRobocarConnectionLiveData.getValue() != null) {
            // We're already connecting to something else
            return;
        }
        RobocarEndpoint endpoint = mEndpointMap.get(endpointId);
        if (endpoint == null) {
            // Not a valid ID
            return;
        }

        RobocarConnection connection = new RobocarConnection(endpoint.mEndpointId,
                endpoint.mAdvertisingInfo, this, endpoint.mIsRemembered);
        connection.setState(ConnectionState.REQUESTING);
        mRobocarConnectionLiveData.setValue(connection);

        String name = mDiscovererInfo == null ? null : mDiscovererInfo.getAdvertisingName();
        Nearby.Connections.requestConnection(mGoogleApiClient, name, endpointId, mLifecycleCallback)
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
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null && connection.endpointMatches(endpointId)) {
            connection.setAuthToken(connectionInfo.getAuthenticationToken());
            if (connection.isAutoConnect()) {
                acceptConnection();
            } else {
                // Wait for something else to call acceptConnection.
                connection.setState(ConnectionState.AUTHENTICATING);
            }
        } else {
            // We didn't request this connection, so reject it.
            rejectConnection(endpointId);
        }
    }

    private PendingResult<Status> acceptConnection(String endpointId) {
        return Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId,
                mInternalPayloadListener);
    }

    public void acceptConnection() {
        final RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection == null) {
            return;
        }

        connection.setState(ConnectionState.AUTH_ACCEPTED);
        acceptConnection(connection.getEndpointId())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Accepted connection.");
                        } else {
                            Log.d(TAG, String.format("Accept unsuccessful. %d %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                            // revert state
                            connection.setState(ConnectionState.AUTHENTICATING);
                        }
                    }
                });
    }

    private PendingResult<Status> rejectConnection(String endpointId) {
        return Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId);
    }

    public void rejectConnection() {
        final RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection == null) {
            return;
        }

        connection.setState(ConnectionState.AUTH_REJECTED);
        rejectConnection(connection.getEndpointId())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Rejected connection.");
                        } else {
                            Log.d(TAG, String.format("Reject unsuccessful. %d %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                            connection.setState(ConnectionState.AUTHENTICATING);
                        }
                    }
                });
    }

    @Override
    protected void onNearbyConnectionRejected(String endpointId) {
        super.onNearbyConnectionRejected(endpointId);
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null && connection.endpointMatches(endpointId)) {
            if (connection.isAutoConnect()) {
                // Avoid reconnecting.
                mAutoConnectEnabled = false;
            }
            clearRobocarConnection();
        }
    }

    @Override
    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        super.onNearbyConnected(endpointId, connectionResolution);
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null && connection.endpointMatches(endpointId)) {
            stopDiscovery();
            connection.setState(ConnectionState.CONNECTED);
            savePairingInformation(connection);
            // We may have disabled this due to a canceled or rejected connection. Re-enable it now.
            mAutoConnectEnabled = true;
        } else {
            // This should never happen, but let's disconnect just to be safe.
            disconnectFromEndpoint(endpointId);
        }
    }

    @Override
    protected void onNearbyDisconnected(String endpointId) {
        super.onNearbyDisconnected(endpointId);
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null && connection.endpointMatches(endpointId)) {
            clearRobocarConnection();
            startDiscovery();
        }
    }

    public void disconnect() {
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null) {
            if (connection.isConnected()) {
                disconnectFromEndpoint(connection.getEndpointId());
                // Avoid reconnecting.
                mAutoConnectEnabled = false;
            }
            // We don't receive onNearbyDisconnected() from the above, and we want to clear it
            // anyway to handle cancelation by the user.
            clearRobocarConnection();
            // If we were connected and stopped discovering, this will start it again.
            startDiscovery();
        }
    }

    private void clearRobocarConnection() {
        RobocarConnection connection = mRobocarConnectionLiveData.getValue();
        if (connection != null) {
            connection.setState(ConnectionState.NOT_CONNECTED);
            mRobocarConnectionLiveData.setValue(null);
        }
    }

    private void clearEndpoints() {
        mEndpointMap.clear();
        onEndpointsChanged();
    }

    private void onEndpointsChanged() {
        mRobocarEndpointsLiveData.setValue(new ArrayList<>(mEndpointMap.values()));
    }

    private boolean isTheDroidWeAreLookingFor(AdvertisingInfo info) {
        return mPairedAdvertisingInfo != null && mPairedAdvertisingInfo.equals(info);
    }

    private void savePairingInformation(RobocarConnection connection) {
        AdvertisingInfo ai = connection.getAdvertisingInfo();
        String authToken = connection.getAuthToken();
        DiscovererInfo diWithToken = new DiscovererInfo(mDiscovererInfo.mCompanionId, authToken);
        AdvertisingInfo aiWithToken = new AdvertisingInfo(ai.mRobocarId, ai.mLedSequence,
                authToken);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                mGoogleApiClient.getContext());
        PreferenceUtils.saveAdvertisingInfo(prefs, aiWithToken);
        PreferenceUtils.saveDiscovererInfo(prefs, diWithToken);

        setDiscovererInfo(diWithToken);
        setPairedAdvertisingInfo(aiWithToken);
    }
}

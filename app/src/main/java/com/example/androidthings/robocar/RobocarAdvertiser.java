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
package com.example.androidthings.robocar;

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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.Connections.StartAdvertisingResult;

public class RobocarAdvertiser extends NearbyConnectionManager implements ConnectionCallbacks {

    private static final String TAG = "RobocarAdvertiser";

    private AdvertisingInfo mAdvertisingInfo;
    private DiscovererInfo mPairedDiscovererInfo;

    private MutableLiveData<Boolean> mAdvertisingLiveData;
    private MutableLiveData<CompanionConnection> mCompanionConnectionLiveData;

    public RobocarAdvertiser(GoogleApiClient client) {
        super(client);
        client.registerConnectionCallbacks(this);

        mAdvertisingLiveData = new MutableLiveData<>();
        mAdvertisingLiveData.setValue(false);
        mCompanionConnectionLiveData = new MutableLiveData<>();
    }

    public void setAdvertisingInfo(AdvertisingInfo info) {
        if (mAdvertisingInfo != info) {
            boolean wasAdvertising = mAdvertisingLiveData.getValue();
            stopAdvertising();

            mAdvertisingInfo = info;
            if (wasAdvertising) {
                startAdvertising();
            }
        }
    }

    public void setPairedDiscovererInfo(DiscovererInfo info) {
        mPairedDiscovererInfo = info;
    }

    // For observers

    public LiveData<Boolean> getAdvertisingLiveData() {
        return mAdvertisingLiveData;
    }

    public LiveData<CompanionConnection> getCompanionConnectionLiveData() {
        return mCompanionConnectionLiveData;
    }

    // Advertising

    public final void startAdvertising() {
        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Google Api Client not connected");
            return;
        }
        if (mAdvertisingInfo == null) {
            Log.d(TAG, "Can't start advertising, no advertising info.");
            return;
        }
        if (mAdvertisingLiveData.getValue()) {
            Log.d(TAG, "Already advertising");
            return;
        }

        // Pre-emptively set this so the check above catches calls while we wait for a result.
        mAdvertisingLiveData.setValue(true);
        Nearby.Connections.startAdvertising(mGoogleApiClient, mAdvertisingInfo.getAdvertisingName(),
                SERVICE_ID, mLifecycleCallback, new AdvertisingOptions(STRATEGY))
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(@NonNull StartAdvertisingResult startAdvertisingResult) {
                        Status status = startAdvertisingResult.getStatus();
                        if (status.isSuccess()) {
                            Log.d(TAG, "Advertising started.");
                            mAdvertisingLiveData.setValue(true);
                        } else {
                            Log.d(TAG, String.format("Failed to start advertising. %d, %s",
                                    status.getStatusCode(), status.getStatusMessage()));
                            // revert state
                            mAdvertisingLiveData.setValue(false);
                        }
                    }
                });
    }

    public final void stopAdvertising() {
        if (mAdvertisingLiveData.getValue()) {
            mAdvertisingLiveData.setValue(false);
            // if we're not connected, we should already have lost advertising
            if (mGoogleApiClient.isConnected()) {
                Nearby.Connections.stopAdvertising(mGoogleApiClient);
            }
        }
    }

    // GoogleApiClient connection

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startAdvertising();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        stopAdvertising();
        disconnectCompanion();
    }

    // Nearby connection

    @Override
    protected void onNearbyConnectionInitiated(final String endpointId,
            ConnectionInfo connectionInfo) {
        super.onNearbyConnectionInitiated(endpointId, connectionInfo);
        if (mCompanionConnectionLiveData.getValue() != null) {
            // We already have a companion trying to connect. Reject this one.
            Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId);
            return;
        }


        DiscovererInfo info = DiscovererInfo.parse(connectionInfo.getEndpointName());
        if (info == null || isNotTheDroidWeAreLookingFor(info)) {
            // Discoverer looks malformed, or doesn't match our previous paired companion.
            Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId);
            return;
        }

        // Store the endpoint and accept.
        CompanionConnection connection = new CompanionConnection(endpointId, info, this);
        connection.setAuthToken(connectionInfo.getAuthenticationToken());
        connection.setState(ConnectionState.AUTH_ACCEPTED);
        mCompanionConnectionLiveData.setValue(connection);

        Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mInternalPayloadListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Accepted connection. " + endpointId);
                            // TODO implement a timeout
                        } else {
                            Log.d(TAG, "Accept connection failed." + endpointId);
                            // revert state
                            clearCompanionEndpoint();
                        }
                    }
                });
    }

    @Override
    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        super.onNearbyConnected(endpointId, connectionResolution);
        if (isCompanionEndpointId(endpointId)) {
            stopAdvertising();

            CompanionConnection connection = mCompanionConnectionLiveData.getValue();
            connection.setState(ConnectionState.CONNECTED);
            savePairingInformation(connection);
        } else {
            disconnectFromEndpoint(endpointId);
        }
    }

    @Override
    protected void onNearbyConnectionRejected(String endpointId) {
        super.onNearbyConnectionRejected(endpointId);
        if (isCompanionEndpointId(endpointId)) {
            clearCompanionEndpoint();
        }
    }

    @Override
    protected void onNearbyDisconnected(String endpointId) {
        super.onNearbyDisconnected(endpointId);
        if (isCompanionEndpointId(endpointId)) {
            mCompanionConnectionLiveData.getValue().setState(ConnectionState.NOT_CONNECTED);
            clearCompanionEndpoint();
            startAdvertising();
        }
    }

    private boolean isCompanionEndpointId(String id) {
        CompanionConnection connection = mCompanionConnectionLiveData.getValue();
        return connection != null && connection.endpointMatches(id);
    }

    private void clearCompanionEndpoint() {
        mCompanionConnectionLiveData.setValue(null);
    }

    public void disconnectCompanion() {
        CompanionConnection connection = mCompanionConnectionLiveData.getValue();
        if (connection != null && connection.isConnected()) {
            // Disconnect from our companion.
            // If the API client isn't connected, we should have already lost the Nearby connection.
            if (mGoogleApiClient.isConnected()) {
                disconnectFromEndpoint(connection.getEndpointId());
            }
        }
        clearCompanionEndpoint();
    }

    private boolean isNotTheDroidWeAreLookingFor(DiscovererInfo info) {
        return mPairedDiscovererInfo != null && !mPairedDiscovererInfo.equals(info);
    }

    private void savePairingInformation(CompanionConnection connection) {
        DiscovererInfo di = connection.getDiscovererInfo();
        String authToken = connection.getAuthToken();
        DiscovererInfo diWithToken = new DiscovererInfo(di.mCompanionId, authToken);
        AdvertisingInfo aiWithToken = new AdvertisingInfo(mAdvertisingInfo.mRobocarId,
                mAdvertisingInfo.mLedSequence, authToken);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                mGoogleApiClient.getContext());
        PreferenceUtils.saveDiscovererInfo(prefs, diWithToken);
        PreferenceUtils.saveAdvertisingInfo(prefs, aiWithToken);

        setAdvertisingInfo(aiWithToken);
        setPairedDiscovererInfo(diWithToken);
    }
}

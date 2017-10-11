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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.androidthings.robocar.shared.CarCommands;
import com.example.androidthings.robocar.shared.NearbyConnectionManager;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.example.androidthings.robocar.shared.model.CompanionEndpoint;
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

    private CompanionEndpoint mCompanionEndpoint;

    private MutableLiveData<Boolean> mAdvertisingLiveData;
    private MutableLiveData<CompanionEndpoint> mCompanionLiveData;

    public RobocarAdvertiser(GoogleApiClient client) {
        super(client);
        client.registerConnectionCallbacks(this);

        mAdvertisingLiveData = new MutableLiveData<>();
        mAdvertisingLiveData.setValue(false);
        mCompanionLiveData = new MutableLiveData<>();
    }

    public void setAdvertisingInfo(AdvertisingInfo info) {
        if (mAdvertisingInfo != info) {
            disconnectCompanion();
            boolean wasAdvertising = mAdvertisingLiveData.getValue();
            stopAdvertising();

            mAdvertisingInfo = info;
            if (wasAdvertising) {
                startAdvertising();
            }
        }
    }

    // For observers

    public LiveData<Boolean> getAdvertisingLiveData() {
        return mAdvertisingLiveData;
    }

    public LiveData<CompanionEndpoint> getCompanionLiveData() {
        return mCompanionLiveData;
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
        if (mCompanionEndpoint != null) {
            // We already have a companion trying to connect. Reject this one.
            Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId);
            return;
        }

        DiscovererInfo info = DiscovererInfo.parse(connectionInfo.getEndpointName());
        if (info == null) {
            // Discoverer appears to be malformed.
            Nearby.Connections.rejectConnection(mGoogleApiClient, endpointId);
            return;
        }

        // Store the endpoint and accept.
        mCompanionEndpoint = new CompanionEndpoint(endpointId,
                connectionInfo.getAuthenticationToken());
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
            mCompanionLiveData.setValue(mCompanionEndpoint);
            stopAdvertising();
            // TODO save companion data for automatic reconnect
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
            clearCompanionEndpoint();
            startAdvertising();
        }
    }

    private boolean isCompanionEndpointId(String id) {
        return mCompanionEndpoint != null && TextUtils.equals(id, mCompanionEndpoint.mEndpointId);
    }

    private void clearCompanionEndpoint() {
        mCompanionEndpoint = null;
        mCompanionLiveData.setValue(null);
    }

    private void disconnectCompanion() {
        if (mCompanionEndpoint != null) {
            // Disconnect from our companion.
            // If the API client isn't connected, we should have already lost the Nearby connection.
            if (mGoogleApiClient.isConnected()) {
                disconnectFromEndpoint(mCompanionEndpoint.mEndpointId);
            }
        }
        clearCompanionEndpoint();
    }

    public void sendCommand(byte command) {
        if (mCompanionEndpoint != null) {
            sendData(mCompanionEndpoint.mEndpointId, CarCommands.toPayload(command));
        }
    }
}

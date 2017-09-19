/*
 * Copyright (C) 2017 The Android Open Source Project
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
 */

package com.example.androidthings.controllablething.shared;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.PayloadCallback;

public class NearbyDiscoverer extends NearbyConnectionManager {

    private static final String TAG = "NearbyDiscoverer";

    public NearbyDiscoverer(Context context, PayloadCallback payloadListener) {
        this(context, payloadListener, null);
    }

    public NearbyDiscoverer(Context context, PayloadCallback payloadListener,
            ConnectionStateListener connectionStateListener) {
        super(context, payloadListener, connectionStateListener);
    }

    private void startDiscovery() {
        Nearby.Connections.startDiscovery(mGoogleApiClient, SERVICE_ID, mEndpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY)).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "onResult: Discovery started!");
                    setState(STATE_PAIRING);
                } else {
                    Log.d(TAG, "onResult: Discovery not started. " + status.getStatusMessage());
                    setState(STATE_ERROR);
                }
            }
        });
    }

    // Discovery
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId,
                        DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
                    Log.d(TAG, "onEndpointFound: Endpoint found!");
                    String name = discoveredEndpointInfo.getEndpointName();
                    remoteEndpointId = endpointId;
                    Nearby.Connections.requestConnection(mGoogleApiClient, name, endpointId,
                            mLifecycleCallback).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.d(TAG, "onResult: Endpoint found, requested connection.");
                            }
                        }
                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    private void stopDiscovery() {
        Nearby.Connections.stopDiscovery(mGoogleApiClient);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API connected. Start discovery.");
        startDiscovery();
    }

    @Override
    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        super.onNearbyConnected(endpointId, connectionResolution);
        Log.d(TAG, String.format("Connected to %s. Stop discovery.", endpointId));
        stopDiscovery();
    }

    @Override
    protected void onNearbyDisconnected(String endpointId) {
        super.onNearbyDisconnected(endpointId);
        Log.d(TAG, String.format("Disconnected from %s. Start discovery.", endpointId));
        startDiscovery();
    }
}

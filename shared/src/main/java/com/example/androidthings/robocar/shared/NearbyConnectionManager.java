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

package com.example.androidthings.robocar.shared;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

public abstract class NearbyConnectionManager {

    private final static String TAG = "NearbyConnectionManager";

    protected static final String SERVICE_ID = "com.example.androidthings.controllablething";
    protected static final Strategy STRATEGY = Strategy.P2P_STAR;

    protected final GoogleApiClient mGoogleApiClient;

    protected ConnectionLifecycleCallback mLifecycleCallback;
    private PayloadCallback mPayloadListener;
    protected PayloadCallback mInternalPayloadListener = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            if (mPayloadListener != null) {
                mPayloadListener.onPayloadReceived(endpointId, payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId,
                PayloadTransferUpdate payloadTransferUpdate) {
            if (mPayloadListener != null) {
                mPayloadListener.onPayloadTransferUpdate(endpointId, payloadTransferUpdate);
            }
        }
    };

    public static GoogleApiClient createNearbyApiClient(Context context) {
        return new GoogleApiClient.Builder(context.getApplicationContext())
                .addApi(Nearby.CONNECTIONS_API)
                .build();
    }

    public NearbyConnectionManager(GoogleApiClient client) {
        mGoogleApiClient = client;
        mLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d(TAG, "onConnectionInitiated: " + endpointId);
                onNearbyConnectionInitiated(endpointId, connectionInfo);
            }

            @Override
            public void onConnectionResult(String endpointId,
                    ConnectionResolution connectionResolution) {
                if (connectionResolution.getStatus().isSuccess()) {
                    // We're connected! Huzzah!
                    Log.d(TAG, "onConnectionResult: Connected! " + endpointId);
                    onNearbyConnected(endpointId, connectionResolution);
                } else {
                    Log.d(TAG, "onConnectionResult: Not connected :( " + endpointId);
                    onNearbyConnectionRejected(endpointId);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d(TAG, "Nearby disconnected: " + endpointId);
                onNearbyDisconnected(endpointId);
            }
        };
    }

    public void setPayloadListener(PayloadCallback listener) {
        mPayloadListener = listener;
    }

    // Nearby API connection callbacks

    protected void onNearbyConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {}

    protected void onNearbyConnectionRejected(String endpointId) {}

    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        Log.d(TAG, "Connected to " + endpointId);
    }

    protected void onNearbyDisconnected(String endpointId) {
        Log.d(TAG, "Disconnected from " + endpointId);
    }

    // end of callbacks

    public void disconnectFromEndpoint(String endpointId) {
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpointId);
    }

    public void sendData(String endpointId, Payload payload) {
        if (mGoogleApiClient.isConnected()) {
            Nearby.Connections.sendPayload(mGoogleApiClient, endpointId, payload);
        }
    }
}

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

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.Strategy;

public abstract class NearbyConnectionManager {

    private final static String TAG = "NearbyConnectionManager";

    final static String SERVICE_ID = "com.example.androidthings.controllablething";
    final static Strategy STRATEGY = Strategy.P2P_STAR;

    public static final int STATE_ERROR = -1;
    public static final int STATE_OFF = 0;
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_PAIRING = 2;
    public static final int STATE_CONNECTING = 3;
    public static final int STATE_CONNECTED = 4;
    public static final int STATE_SUSPENDED = 5;

    private int mState = STATE_OFF;

    GoogleApiClient mGoogleApiClient;
    String remoteEndpointId;

    ConnectionLifecycleCallback mLifecycleCallback;
    private PayloadCallback mPayloadListener;
    private ConnectionStateListener mConnectionStateListener;

    public interface ConnectionStateListener {

        void onConnectionStateChanged(int oldState, int newState);
    }

    NearbyConnectionManager(GoogleApiClient client, PayloadCallback payloadListener) {
        this(client, payloadListener, null);
    }

    NearbyConnectionManager(GoogleApiClient client, PayloadCallback payloadListener,
            ConnectionStateListener connectionStateListener) {
        mGoogleApiClient = client;
        mPayloadListener = payloadListener;
        mConnectionStateListener = connectionStateListener;
        initializeLifecycleCallback();
    }

    private void initializeLifecycleCallback() {
        mLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d(TAG, "onConnectionInitiated: " + endpointId);
                // TODO companion should ask user for confirmation?
                Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadListener);
                remoteEndpointId = endpointId;
                setState(STATE_CONNECTING);
                onNearbyConnectionInitiated(endpointId);
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
                    setState(STATE_PAIRING);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d(TAG, "Nearby disconnected: " + endpointId);
                remoteEndpointId = null;
                onNearbyDisconnected(endpointId);
            }
        };
    }

    protected void setState(int state) {
        if (mState != state) {
            int oldState = mState;
            mState = state;
            if (mConnectionStateListener != null) {
                mConnectionStateListener.onConnectionStateChanged(oldState, state);
            }
        }
    }

    // Nearby API connection callbacks

    protected void onNearbyConnectionInitiated(String endpointId) {}

    protected void onNearbyConnectionRejected(String endpointId) {}

    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        setState(STATE_CONNECTED);
    }

    protected void onNearbyDisconnected(String endpointId) {
        setState(STATE_PAIRING);
    }

    // end of callbacks

    public void connect() {
        setState(STATE_INITIALIZING);
    }

    public void disconnect() {
        setState(STATE_OFF);
    }

    public void sendData(int data) {
        sendData(new byte[]{(byte) data});
    }

    public void sendData(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            Log.d(TAG, "sendData: Empty byte array!");
            return;
        }
        Nearby.Connections.sendPayload(mGoogleApiClient, remoteEndpointId,
                Payload.fromBytes(bytes));
    }
}

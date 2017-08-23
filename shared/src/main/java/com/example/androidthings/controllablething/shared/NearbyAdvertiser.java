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
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.Connections.StartAdvertisingResult;
import com.google.android.gms.nearby.connection.PayloadCallback;

public class NearbyAdvertiser extends NearbyConnectionManager {

    private static final String TAG = "NearbyAdvertiser";
    private final String mAdvertisingName;

    public NearbyAdvertiser(Context context, PayloadCallback payloadListener,
            String advertisingName) {
        this(context, payloadListener, null, advertisingName);
    }

    public NearbyAdvertiser(Context context, PayloadCallback payloadListener,
            ConnectionStateListener connectionStateListener, String advertisingName) {
        super(context, payloadListener, connectionStateListener);
        mAdvertisingName = advertisingName;
    }

    private void startAdvertising() {
        Nearby.Connections.startAdvertising(mGoogleApiClient, mAdvertisingName, SERVICE_ID,
                mLifecycleCallback, new AdvertisingOptions(STRATEGY))
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(@NonNull StartAdvertisingResult startAdvertisingResult) {
                        Status status = startAdvertisingResult.getStatus();
                        if (status.isSuccess()) {
                            Log.d(TAG, "onResult: Successfully started advertising.");
                            setState(STATE_PAIRING);
                        } else {
                            Log.d(TAG, "onResult: Failed to start advertising.");
                            Log.d(TAG, "onResult: Reason: " + startAdvertisingResult.toString());
                            setState(STATE_ERROR);
                        }
                    }
                });
    }

    private void stopAdvertising() {
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API connected. Start advertising.");
        startAdvertising();
    }

    @Override
    protected void onNearbyConnected(String endpointId, ConnectionResolution connectionResolution) {
        super.onNearbyConnected(endpointId, connectionResolution);
        Log.d(TAG, String.format("Connected to %s. Stop advertising.", endpointId));
        stopAdvertising();
    }

    @Override
    protected void onNearbyDisconnected(String endpointId) {
        super.onNearbyDisconnected(endpointId);
        Log.d(TAG, String.format("Disconnected from %s. Start advertising.", endpointId));
        startAdvertising();
    }
}

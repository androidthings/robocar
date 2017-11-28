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
package com.example.androidthings.robocar.shared;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NearbyConnection {

    @IntDef({ConnectionState.NOT_CONNECTED, ConnectionState.REQUESTING,
            ConnectionState.AUTHENTICATING, ConnectionState.AUTH_ACCEPTED,
            ConnectionState.AUTH_REJECTED, ConnectionState.CONNECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionState {
        int NOT_CONNECTED = 0;
        int REQUESTING = 1;
        int AUTHENTICATING = 2;
        int AUTH_ACCEPTED = 3;
        int AUTH_REJECTED = 4;
        int CONNECTED = 5;
    }

    private final String mEndpointId;

    @ConnectionState
    private int mState;
    private String mAuthToken;

    private MutableLiveData<Integer> mStateLiveData;

    private NearbyConnectionManager mConnectionManager;

    public NearbyConnection(String endpointId, NearbyConnectionManager connectionManager) {
        if (endpointId == null) {
            throw new IllegalArgumentException("RobocarEndpoint cannot be null");
        }
        if (connectionManager == null) {
            throw new IllegalArgumentException("NearbyConnectionManager cannot be null");
        }
        mEndpointId = endpointId;
        mConnectionManager = connectionManager;
        mStateLiveData = new MutableLiveData<>();

        setState(ConnectionState.NOT_CONNECTED);
    }

    public boolean endpointMatches(String endpointId) {
        return mEndpointId.equals(endpointId);
    }

    public String getEndpointId() {
        return mEndpointId;
    }

    @ConnectionState
    public int getState() {
        return mState;
    }

    public void setState(@ConnectionState int newState) {
        if (mState != newState) {
            mState = newState;
            mStateLiveData.setValue(mState);
        }
    }

    public boolean isConnected() {
        return mState == ConnectionState.CONNECTED;
    }

    public LiveData<Integer> getConnectionStateLiveData() {
        return mStateLiveData;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public void setAuthToken(String authToken) {
        mAuthToken = authToken;
    }

    public void sendCommand(byte command) {
        if (getState() == ConnectionState.CONNECTED) {
            mConnectionManager.sendData(getEndpointId(), CarCommands.toPayload(command));
        }
    }
}

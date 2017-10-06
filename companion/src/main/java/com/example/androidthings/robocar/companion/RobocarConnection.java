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
import android.support.annotation.IntDef;

import com.example.androidthings.robocar.shared.CarCommands;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.example.androidthings.robocar.shared.model.RobocarEndpoint;

/**
 * Handle for a connection to a Robocar, providing convenient methods for both authenticating the
 * connection and transmitting data through it.
 */
public class RobocarConnection {

    @IntDef({ConnectionState.NOT_CONNECTED, ConnectionState.REQUESTING,
            ConnectionState.AUTHENTICATING, ConnectionState.AUTH_ACCEPTED,
            ConnectionState.AUTH_REJECTED, ConnectionState.CONNECTED})
    public @interface ConnectionState {
        int NOT_CONNECTED = 0;
        int REQUESTING = 1;
        int AUTHENTICATING = 2;
        int AUTH_ACCEPTED = 3;
        int AUTH_REJECTED = 4;
        int CONNECTED = 5;
    }

    private final RobocarDiscoverer mRobocarDiscoverer;
    private final String mEndpointId;
    private final AdvertisingInfo mAdvertisingInfo;

    @ConnectionState
    private int mState;
    private String mAuthToken;

    private MutableLiveData<Integer> mStateLiveData;

    public RobocarConnection(RobocarEndpoint endpoint, RobocarDiscoverer robocarDiscoverer) {
        if (endpoint == null) {
            throw new IllegalArgumentException("RobocarEndpoint cannot be null");
        }
        if (robocarDiscoverer == null) {
            throw new IllegalArgumentException("RobocarDiscoverer cannot be null");
        }
        mEndpointId = endpoint.mEndpointId;
        mAdvertisingInfo = endpoint.mAdvertisingInfo;
        mRobocarDiscoverer = robocarDiscoverer;
        mStateLiveData = new MutableLiveData<>();

        setState(ConnectionState.NOT_CONNECTED);
    }

    public boolean endpointMatches(String endpointId) {
        return mEndpointId.equals(endpointId);
    }

    public String getEndpointId() {
        return mEndpointId;
    }

    public AdvertisingInfo getAdvertisingInfo() {
        return mAdvertisingInfo;
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

    public void accept() {
        if (mState == ConnectionState.AUTHENTICATING) {
            mRobocarDiscoverer.acceptConnection();
        }
    }

    public void reject() {
        if (mState == ConnectionState.AUTHENTICATING) {
            mRobocarDiscoverer.rejectConnection();
        }
    }

    public void disconnect() {
        if (mState == ConnectionState.CONNECTED) {
            mRobocarDiscoverer.disconnect();
        }
    }

    public void sendCommand(byte command) {
        if (mState == ConnectionState.CONNECTED) {
            mRobocarDiscoverer.sendData(mEndpointId, CarCommands.toPayload(command));
        }
    }
}

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

import com.example.androidthings.robocar.shared.NearbyConnection;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;

/**
 * Handle for a connection to a Robocar, providing convenient methods for both authenticating the
 * connection and transmitting data through it.
 */
public class RobocarConnection extends NearbyConnection {

    private final RobocarDiscoverer mRobocarDiscoverer;
    private final AdvertisingInfo mAdvertisingInfo;

    private final boolean mAutoConnect;

    public RobocarConnection(String endpointId, AdvertisingInfo advertisingInfo,
            RobocarDiscoverer robocarDiscoverer, boolean autoConnect) {
        super(endpointId, robocarDiscoverer);
        if (advertisingInfo == null) {
            throw new IllegalArgumentException("AdvertisingInfo cannot be null");
        }
        mAdvertisingInfo = advertisingInfo;
        mRobocarDiscoverer = robocarDiscoverer;
        mAutoConnect = autoConnect;
    }

    public AdvertisingInfo getAdvertisingInfo() {
        return mAdvertisingInfo;
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public void accept() {
        if (getState() == ConnectionState.AUTHENTICATING) {
            mRobocarDiscoverer.acceptConnection();
        }
    }

    public void reject() {
        if (getState() == ConnectionState.AUTHENTICATING) {
            mRobocarDiscoverer.rejectConnection();
        }
    }

    public void disconnect() {
        mRobocarDiscoverer.disconnect();
    }
}

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

import com.example.androidthings.robocar.shared.NearbyConnection;
import com.example.androidthings.robocar.shared.model.DiscovererInfo;

/**
 * Handle for a connection to a Companion
 */
public class CompanionConnection extends NearbyConnection {

    private final DiscovererInfo mDiscovererInfo;
    private final RobocarAdvertiser mRobocarAdvertiser;

    public CompanionConnection(String endpointId, DiscovererInfo discovererInfo,
            RobocarAdvertiser advertiser) {
        super(endpointId, advertiser);
        if (discovererInfo == null) {
            throw new IllegalArgumentException("DiscovererInfo cannot be null");
        }
        mDiscovererInfo = discovererInfo;
        mRobocarAdvertiser = advertiser;
    }

    public DiscovererInfo getDiscovererInfo() {
        return mDiscovererInfo;
    }

    public boolean isAuthenticating() {
        return getState() == ConnectionState.AUTHENTICATING
                || getState() == ConnectionState.AUTH_ACCEPTED;
    }
}

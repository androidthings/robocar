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

import com.example.androidthings.robocar.shared.model.AdvertisingInfo;

/**
 * Immutable class representing a Robocar's Nearby endpoint information.
 */
public class RobocarEndpoint {

    public final String mEndpointId;
    public final AdvertisingInfo mAdvertisingInfo;
    public final boolean mIsPaired;
    public final boolean mIsRemembered;

    public RobocarEndpoint(String endpointId, AdvertisingInfo advertisingInfo,
            boolean isRemembered) {
        if (endpointId == null) {
            throw new IllegalArgumentException("Endpoint ID cannot be null");
        }
        if (advertisingInfo == null) {
            throw new IllegalArgumentException("Advertising Info cannot be null");
        }
        mEndpointId = endpointId;
        mAdvertisingInfo = advertisingInfo;
        mIsPaired = mAdvertisingInfo.mIsPaired;
        mIsRemembered = mIsPaired && isRemembered;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(RobocarEndpoint.class)) {
            return false;
        }
        return this.mEndpointId.equals(((RobocarEndpoint) obj).mEndpointId);
    }

    @Override
    public int hashCode() {
        return mEndpointId.hashCode();
    }
}

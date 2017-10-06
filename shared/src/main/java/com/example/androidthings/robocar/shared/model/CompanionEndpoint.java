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
package com.example.androidthings.robocar.shared.model;

/**
 * Immutable class representing a Companion's Nearby endpoint information.
 */
public class CompanionEndpoint {

    public final String mEndpointId;
    public final String mAuthToken;

    public CompanionEndpoint(String endpointId, String authToken) {
        if (endpointId == null) {
            throw new IllegalArgumentException("Ednpoint ID cannot be null");
        }
        mEndpointId = endpointId;
        mAuthToken = authToken;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(CompanionEndpoint.class)) {
            return false;
        }
        return this.mEndpointId.equals(((CompanionEndpoint) obj).mEndpointId);
    }

    @Override
    public int hashCode() {
        return mEndpointId.hashCode();
    }
}

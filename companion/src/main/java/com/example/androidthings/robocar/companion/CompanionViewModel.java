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

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.IntDef;

import com.example.androidthings.robocar.shared.NearbyConnectionManager;
import com.google.android.gms.common.api.GoogleApiClient;


public class CompanionViewModel extends AndroidViewModel {

    @IntDef({NavigationState.DISCOVERY_UI, NavigationState.CONTROLLER_UI})
    public @interface NavigationState {
        int DISCOVERY_UI = 1;
        int CONTROLLER_UI = 2;
    }

    private final GoogleApiClient mGoogleApiClient;
    private final RobocarDiscoverer mRobocarDiscoverer;

    private final MutableLiveData<Integer> mNavigationState;

    public CompanionViewModel(Application application) {
        super(application);
        mGoogleApiClient = NearbyConnectionManager.createNearbyApiClient(application);
        mRobocarDiscoverer = new RobocarDiscoverer(mGoogleApiClient);

        mNavigationState = new MutableLiveData<>();
        mNavigationState.setValue(NavigationState.DISCOVERY_UI);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public RobocarDiscoverer getRobocarDiscoverer() {
        return mRobocarDiscoverer;
    }

    public LiveData<Integer> getNavigationStateLiveData() {
        return mNavigationState;
    }

    public void navigateTo(@NavigationState int state) {
        mNavigationState.setValue(state);
    }
}

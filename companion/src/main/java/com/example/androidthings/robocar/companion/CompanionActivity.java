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

package com.example.androidthings.robocar.companion;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.androidthings.robocar.shared.ConnectorFragment;
import com.example.androidthings.robocar.shared.ConnectorFragment.ConnectorCallbacks;
import com.google.android.gms.common.ConnectionResult;


public class CompanionActivity extends AppCompatActivity implements ConnectorCallbacks {

    private static final String TAG = "CompanionActivity";

    private static final int REQUEST_RESOLVE_CONNECTION = 1;

    private CompanionViewModel mViewModel;
    private RobocarDiscoverer mNearbyDiscoverer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companion);

        mViewModel = ViewModelProviders.of(this).get(CompanionViewModel.class);
        mNearbyDiscoverer = mViewModel.getRobocarDiscoverer();
        // TODO observe things in the ViewModel

        if (savedInstanceState == null) {
            // First launch. Attach the connector fragment and give it our client to connect.
            ConnectorFragment.attachTo(this, mViewModel.getGoogleApiClient());
        }
    }

    @Override
    public void onGoogleApiConnected(Bundle bundle) {}

    @Override
    public void onGoogleApiConnectionSuspended(int cause) {}

    @Override
    public void onGoogleApiConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_CONNECTION);
            } catch (SendIntentException e) {
                Log.e(TAG, "Google API connection failed. " + connectionResult, e);
            }
        } else {
            Log.e(TAG, "Google API connection failed. " + connectionResult);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_CONNECTION) {
            if (resultCode == RESULT_OK) {
                // try to reconnect
                ConnectorFragment.connect(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

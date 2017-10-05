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

package com.example.androidthings.controllablething.companion;

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.example.androidthings.controllablething.shared.CarCommands;
import com.example.androidthings.controllablething.shared.ConnectorFragment;
import com.example.androidthings.controllablething.shared.ConnectorFragment.ConnectorCallbacks;
import com.example.androidthings.controllablething.shared.GoogleApiClientCreator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;


public class CompanionActivity extends AppCompatActivity implements ConnectorCallbacks {

    private static final String TAG = "CompanionActivity";

    private RobocarDiscoverer mNearbyDiscoverer;

    private SparseArray<View> mCarControlMap = new SparseArray<>(5);
    private View mActivatedControl;
    private View mErrorView;
    private TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companion);
        mErrorView = findViewById(R.id.error);
        mLogView = (TextView) findViewById(R.id.log_text);

        configureButton(R.id.btn_forward, CarCommands.GO_FORWARD);
        configureButton(R.id.btn_back, CarCommands.GO_BACK);
        configureButton(R.id.btn_left, CarCommands.TURN_LEFT);
        configureButton(R.id.btn_right, CarCommands.TURN_RIGHT);
        configureButton(R.id.btn_stop, CarCommands.STOP);

        GoogleApiClient client = GoogleApiClientCreator.getClient(this);
        ConnectorFragment.attachTo(this, client);
        mNearbyDiscoverer = new RobocarDiscoverer(client);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNearbyDiscoverer.setPayloadListener(mPayloadListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNearbyDiscoverer.setPayloadListener(null);
    }

    void configureButton(int buttonId, final int command) {
        View button = findViewById(buttonId);
        if (button != null) {
            mCarControlMap.append(command, button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNearbyDiscoverer.sendData(command);
                    setActivatedControl(v);
                }
            });
        }
    }

    private void setActivatedControl(View view) {
        if (mActivatedControl != null && mActivatedControl != view) {
            mActivatedControl.setActivated(false);
        }
        mActivatedControl = view;
        if (mActivatedControl != null) {
            mActivatedControl.setActivated(true);
        }
    }

    private void disableControls() {
        for (int i = 0; i < mCarControlMap.size(); i++) {
            mCarControlMap.valueAt(i).setEnabled(false);
        }
    }

    private void logUi(String text) {
        if (mLogView.getText().length() > 0) {
            mLogView.append("\n");
        }
        mLogView.append(text);
    }

    PayloadCallback mPayloadListener = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            byte[] bytes = CarCommands.fromPayload(payload);
            if (bytes == null || bytes.length == 0) {
                return;
            }

            byte command = bytes[0];
            if (command == CarCommands.ERROR) {
                mErrorView.setVisibility(View.VISIBLE);
                Log.d(TAG, "onPayloadReceived: error");
            } else {
                mErrorView.setVisibility(View.GONE);
                Log.d(TAG, "onPayloadReceived: " + command);
                // activate control
                View toActivate = mCarControlMap.get(command);
                setActivatedControl(toActivate);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    @Override
    public void onGoogleApiConnected(Bundle bundle) {}

    @Override
    public void onGoogleApiConnectionSuspended(int cause) {}

    @Override
    public void onGoogleApiConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this,
                        R.integer.connection_resolution_request_code);
            } catch (SendIntentException e) {
                Log.e(TAG, "Google API connection failed. " + connectionResult, e);
            }
        } else {
            Log.e(TAG, "Google API connection failed. " + connectionResult);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == R.integer.connection_resolution_request_code) {
            if (resultCode == RESULT_OK) {
                // try to reconnect
                ConnectorFragment.connect(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

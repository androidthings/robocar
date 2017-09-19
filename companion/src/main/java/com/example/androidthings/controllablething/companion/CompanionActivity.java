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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.example.androidthings.controllablething.shared.CarCommands;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager
        .ConnectionStateListener;
import com.example.androidthings.controllablething.shared.NearbyDiscoverer;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public class CompanionActivity extends AppCompatActivity {

    private static final String TAG = "CompanionActivity";

    private static final int PERMISSION_REQUEST_CODE = 1;

    private NearbyConnectionManager mNearbyConnectionManager;
    private SparseArray<View> mCarControlMap = new SparseArray<>(5);
    private View mActivatedControl;
    private View mErrorView;
    private TextView mLogView;

    private ConnectionStateListener mConnectionStateListener = new ConnectionStateListener() {
        @Override
        public void onConnectionStateChanged(int oldState, int newState) {
            switch (newState) {
                case NearbyConnectionManager.STATE_OFF:
                    logUi("OFF"); break;
                case NearbyConnectionManager.STATE_ERROR:
                    logUi("ERROR"); break;
                case NearbyConnectionManager.STATE_INITIALIZING:
                    logUi("INIT"); break;
                case NearbyConnectionManager.STATE_SUSPENDED:
                    logUi("SUSPENDED"); break;
                case NearbyConnectionManager.STATE_PAIRING:
                    logUi("PAIRING"); break;
                case NearbyConnectionManager.STATE_CONNECTING:
                    logUi("CONNECTING"); break;
                case NearbyConnectionManager.STATE_CONNECTED:
                    logUi("CONNECTED"); break;
            }
        }
    };

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

        mNearbyConnectionManager = new NearbyDiscoverer(this, payloadListener,
                mConnectionStateListener);
    }

    void configureButton(int buttonId, final int command) {
        View button = findViewById(buttonId);
        if (button != null) {
            mCarControlMap.append(command, button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNearbyConnectionManager.sendData(command);
                    setActivatedControl(v);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasNecessaryPermissions()) {
            mNearbyConnectionManager.connect();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNearbyConnectionManager.disconnect();
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

    public void getRuntimePermissions() {
        // Here, thisActivity is the current activity
        if (!hasNecessaryPermissions()) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    public boolean hasNecessaryPermissions() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    mNearbyConnectionManager.connect();

                } else {
                    // Permission denied, boo! Disable the corresponding functionality.
                    disableControls();
                }
            }
        }
    }

    PayloadCallback payloadListener = new PayloadCallback() {
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
}

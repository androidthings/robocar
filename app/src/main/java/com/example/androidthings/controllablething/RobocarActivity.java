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

package com.example.androidthings.controllablething;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.example.androidthings.controllablething.shared.CarCommands;
import com.example.androidthings.controllablething.shared.ConnectorFragment;
import com.example.androidthings.controllablething.shared.ConnectorFragment.ConnectorCallbacks;
import com.example.androidthings.controllablething.shared.model.AdvertisingInfo;
import com.example.androidthings.controllablething.shared.model.CompanionEndpoint;
import com.example.motorhat.MotorHat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;


public class RobocarActivity extends AppCompatActivity implements ConnectorCallbacks {

    private static final String TAG = "RobocarActivity";

    private AdvertisingInfo mAdvertisingInfo;
    private RobocarAdvertiser mNearbyAdvertiser;

    private MotorHat mMotorHat;
    private TricolorLed mLed;
    private CarController mCarController;
    private RobocarViewModel mViewModel;

    private boolean mIsAdvertising;
    private boolean mIsCompanionConnected;

    PayloadCallback mPayloadListener = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            byte[] bytes = CarCommands.fromPayload(payload);
            if (bytes == null || bytes.length == 0) {
                return;
            }

            byte command = bytes[0];
            Log.d(TAG, "onPayloadReceived: Command: " + command);
            byte response = CarCommands.ERROR;
            if (mCarController != null && mCarController.onCarCommand(command)) {
                response = command;
            }
            mNearbyAdvertiser.sendData(response);
            if (response == CarCommands.ERROR) {
                // TODO flash red
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdvertisingInfo = AdvertisingInfoStore.getInstance(this).get();

        try {
            mMotorHat = new MotorHat(BoardDefaults.getI2cBus());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MotorHat", e);
        }

        String[] ledPins = BoardDefaults.getLedGpioPins();
        mLed = new TricolorLed(ledPins[0], ledPins[1], ledPins[2]);
        mCarController = new CarController(mMotorHat, mLed);

        mViewModel = ViewModelProviders.of(this).get(RobocarViewModel.class);
        mViewModel.setAdvertisingInfo(mAdvertisingInfo);

        mNearbyAdvertiser = mViewModel.getRobocarAdvertiser();
        mNearbyAdvertiser.getAdvertisingLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean value) {
                mIsAdvertising = value == null ? false : value;
                updateUi();
            }
        });
        mNearbyAdvertiser.getCompanionLiveData().observe(this, new Observer<CompanionEndpoint>() {
            @Override
            public void onChanged(@Nullable CompanionEndpoint companionEndpoint) {
                mIsCompanionConnected = companionEndpoint != null;
                updateUi();
            }
        });

        if (savedInstanceState == null) {
            // First launch. Attach the connector fragment and give it our client to connect.
            ConnectorFragment.attachTo(this, mViewModel.getGoogleApiClient());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNearbyAdvertiser.setPayloadListener(mPayloadListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNearbyAdvertiser.setPayloadListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCarController != null) {
            mCarController.shutDown();
        }

        if (mMotorHat != null) {
            try {
                mMotorHat.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing MotorHat", e);
            } finally {
                mMotorHat = null;
            }
        }

        if (mLed != null) {
            try {
                mLed.setColor(TricolorLed.OFF);
                mLed.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing LED", e);
            } finally {
                mLed = null;
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyCode(keyCode) || super.onKeyUp(keyCode, event);
    }

    private boolean handleKeyCode(int keyCode) {
        if (mCarController != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_F:
                    mCarController.onCarCommand(CarCommands.GO_FORWARD);
                    return true;
                case KeyEvent.KEYCODE_B:
                    mCarController.onCarCommand(CarCommands.GO_BACK);
                    return true;
                case KeyEvent.KEYCODE_L:
                    mCarController.onCarCommand(CarCommands.TURN_LEFT);
                    return true;
                case KeyEvent.KEYCODE_R:
                    mCarController.onCarCommand(CarCommands.TURN_RIGHT);
                    return true;
                case KeyEvent.KEYCODE_S:
                    mCarController.onCarCommand(CarCommands.STOP);
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onGoogleApiConnected(Bundle bundle) {}

    @Override
    public void onGoogleApiConnectionSuspended(int cause) {}

    @Override
    public void onGoogleApiConnectionFailed(ConnectionResult connectionResult) {
        // We don't have a UI with which to resolve connection issues.
        Log.e(TAG, "Google API connection failed: " + connectionResult);
    }

    private void updateUi() {
        if (mIsCompanionConnected) {
            mCarController.setLedColor(TricolorLed.GREEN);
        } else if (mIsAdvertising) {
            mCarController.setLedSequence(mAdvertisingInfo.mLedSequence);
        } else {
            mCarController.setLedColor(TricolorLed.YELLOW);
        }
    }
}

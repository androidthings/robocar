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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.example.androidthings.controllablething.shared.CarCommands;
import com.example.androidthings.controllablething.shared.ConnectorFragment;
import com.example.androidthings.controllablething.shared.ConnectorFragment.ConnectorCallbacks;
import com.example.androidthings.controllablething.shared.GoogleApiClientCreator;
import com.example.androidthings.controllablething.shared.NearbyAdvertiser;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager
        .ConnectionStateListener;
import com.example.motorhat.MotorHat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;


public class MainActivity extends Activity implements ConnectorCallbacks {

    private static final String TAG = "MainActivity";

    private NearbyConnectionManager mNearbyConnectionManager;

    private MotorHat mMotorHat;
    private TricolorLed mLed;
    private CarController mCarController;

    PayloadCallback payloadListener = new PayloadCallback() {
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
            mNearbyConnectionManager.sendData(response);
            if (response == CarCommands.ERROR) {
                // TODO flash red
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    private ConnectionStateListener mConnectionStateListener = new ConnectionStateListener() {
        @Override
        public void onConnectionStateChanged(int oldState, int newState) {
            if (mCarController == null) {
                return;
            }
            switch (newState) {
                case NearbyConnectionManager.STATE_OFF:
                    mLed.setColor(TricolorLed.OFF);
                    mCarController.setLedColor(TricolorLed.OFF);
                    break;
                case NearbyConnectionManager.STATE_ERROR:
                    mCarController.setLedColor(TricolorLed.RED);
                    break;
                case NearbyConnectionManager.STATE_INITIALIZING:
                    mCarController.setLedColor(TricolorLed.YELLOW);
                    break;
                case NearbyConnectionManager.STATE_SUSPENDED:
                    mCarController.setLedColor(TricolorLed.YELLOW);
                    break;
                case NearbyConnectionManager.STATE_PAIRING:
                    mCarController.setLedColor(TricolorLed.MAGENTA); // TODO flash red/blue
                    break;
                case NearbyConnectionManager.STATE_CONNECTING:
//                    mCarController.setLedColor(TricolorLed.CYAN); // TODO flash blue/green
                    break;
                case NearbyConnectionManager.STATE_CONNECTED:
                    mCarController.setLedColor(TricolorLed.GREEN);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mMotorHat = new MotorHat(BoardDefaults.getI2cBus());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MotorHat", e);
        }

        String[] ledPins = BoardDefaults.getLedGpioPins();
        mLed = new TricolorLed(ledPins[0], ledPins[1], ledPins[2]);
        mCarController = new CarController(mMotorHat, mLed);

        GoogleApiClient client = GoogleApiClientCreator.getClient(this);
        ConnectorFragment.attachTo(this, client);
        mNearbyConnectionManager = new NearbyAdvertiser(client, payloadListener,
                mConnectionStateListener, mCarController.getAdvertisingName());
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
    public void onGoogleApiConnected(Bundle bundle) {
        mNearbyConnectionManager.connect();
    }

    @Override
    public void onGoogleApiConnectionSuspended(int cause) {
        mNearbyConnectionManager.disconnect();
    }

    @Override
    public void onGoogleApiConnectionFailed(ConnectionResult connectionResult) {
        // We don't have a UI with which to resolve connection issues.
        Log.e(TAG, "Google API connection failed: " + connectionResult);
    }
}

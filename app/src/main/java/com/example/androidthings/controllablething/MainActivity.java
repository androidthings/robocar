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
import com.example.androidthings.controllablething.shared.NearbyAdvertiser;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager;
import com.example.androidthings.controllablething.shared.NearbyConnectionManager.ConnectionStateListener;
import com.example.motorhat.MotorHat;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;

public class MainActivity extends Activity {

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
            switch (newState) {
                case NearbyConnectionManager.STATE_OFF:
                    Log.d(TAG, "onConnectionStateChanged: OFF");
                    mLed.setColor(TricolorLed.OFF);
                    break;
                case NearbyConnectionManager.STATE_ERROR:
                    Log.d(TAG, "onConnectionStateChanged: ERROR (R)");
                    mLed.setColor(TricolorLed.RED);
                    break;
                case NearbyConnectionManager.STATE_INITIALIZING:
                    Log.d(TAG, "onConnectionStateChanged: INIT (Y)");
                    mLed.setColor(TricolorLed.YELLOW);
                    break;
                case NearbyConnectionManager.STATE_SUSPENDED:
                    Log.d(TAG, "onConnectionStateChanged: SUSPEND (Y)");
                    mLed.setColor(TricolorLed.YELLOW);
                    break;
                case NearbyConnectionManager.STATE_PAIRING:
                    Log.d(TAG, "onConnectionStateChanged: PAIR (RB)");
                    mLed.setColor(TricolorLed.MAGENTA); // TODO flash red/blue
                    break;
                case NearbyConnectionManager.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChanged: CONNECTING (BG)");
                    mLed.setColor(TricolorLed.CYAN); // TODO flash blue/green
                    break;
                case NearbyConnectionManager.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChanged: CONNECTED (G)");
                    mLed.setColor(TricolorLed.GREEN);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNearbyConnectionManager = new NearbyAdvertiser(this, payloadListener,
                mConnectionStateListener);

        try {
            mMotorHat = new MotorHat(BoardDefaults.getI2cBus());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MotorHat", e);
        }

        mLed = new TricolorLed("GPIO_34", "GPIO_39", "GPIO_32");
        mCarController = new CarController(mMotorHat, mLed);
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
    protected void onStart() {
        super.onStart();
        mNearbyConnectionManager.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNearbyConnectionManager.disconnect();
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
}

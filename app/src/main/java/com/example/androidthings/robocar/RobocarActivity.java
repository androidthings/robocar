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

package com.example.androidthings.robocar;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.example.androidthings.robocar.shared.CarCommands;
import com.example.androidthings.robocar.shared.ConnectorFragment;
import com.example.androidthings.robocar.shared.ConnectorFragment.ConnectorCallbacks;
import com.example.androidthings.robocar.shared.PreferenceUtils;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.google.android.things.contrib.driver.motorhat.MotorHat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.things.contrib.driver.button.Button.LogicState;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;

import java.io.IOException;


public class RobocarActivity extends AppCompatActivity implements ConnectorCallbacks {

    private static final String TAG = "RobocarActivity";
    private static final long DISCONNECT_DELAY = 2500L; //ms
    private static final long RESET_DELAY = 5000L; //ms

    private AdvertisingInfo mAdvertisingInfo;
    private RobocarAdvertiser mNearbyAdvertiser;
    private CompanionConnection mCompanionConnection;

    private MotorHat mMotorHat;
    private TricolorLed mLed;
    private AlphanumericDisplay mDisplay;
    private ButtonInputDriver mButtonInputDriver;

    private CarController mCarController;
    private RobocarViewModel mViewModel;

    private boolean mIsAdvertising;

    private Handler mResetHandler;
    private boolean mKeyPressed;

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
            mCompanionConnection.sendCommand(response);
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
        // init AdvertisingInfo
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAdvertisingInfo = PreferenceUtils.loadAdvertisingInfo(prefs);
        if (mAdvertisingInfo == null) {
            mAdvertisingInfo = AdvertisingInfo.generateAdvertisingInfo();
            PreferenceUtils.saveAdvertisingInfo(prefs, mAdvertisingInfo);
        }

        try {
            mMotorHat = new MotorHat(BoardDefaults.getI2cBus());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MotorHat", e);
        }
        try {
            mDisplay = new AlphanumericDisplay(BoardDefaults.getI2cBus());
            mDisplay.setEnabled(true);
            mDisplay.setBrightness(0.5f);
            mDisplay.clear();
        } catch (IOException e) {
            // We may not have a display, which is OK. CarController only uses it if it's not null.
            Log.e(TAG, "Failed to open display.", e);
            mDisplay = null;
        }
        try {
            mButtonInputDriver = new ButtonInputDriver(BoardDefaults.getButtonGpioPin(),
                    LogicState.PRESSED_WHEN_HIGH, KeyEvent.KEYCODE_A);
            mButtonInputDriver.register();
        } catch (IOException e) {
            Log.e(TAG, "Failed to open button driver.", e);
            mButtonInputDriver = null;
        }

        String[] ledPins = BoardDefaults.getLedGpioPins();
        mLed = new TricolorLed(ledPins[0], ledPins[1], ledPins[2]);
        mCarController = new CarController(mMotorHat, mLed, mDisplay);

        mResetHandler = new Handler();

        mViewModel = ViewModelProviders.of(this).get(RobocarViewModel.class);
        mNearbyAdvertiser = mViewModel.getRobocarAdvertiser();

        mNearbyAdvertiser.setAdvertisingInfo(mAdvertisingInfo);
        mNearbyAdvertiser.setPairedDiscovererInfo(PreferenceUtils.loadDiscovererInfo(prefs));
        mNearbyAdvertiser.getAdvertisingLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean value) {
                mIsAdvertising = value == null ? false : value;
                updateUi();
            }
        });
        mNearbyAdvertiser.getCompanionConnectionLiveData().observe(this,
                new Observer<CompanionConnection>() {
            @Override
            public void onChanged(@Nullable CompanionConnection connection) {
                setConnection(connection);
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

        if (mDisplay != null) {
            try {
                mDisplay.clear();
                mDisplay.setBrightness(0);
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing display", e);
            } finally {
                mDisplay = null;
            }
        }

        if (mButtonInputDriver != null) {
            mButtonInputDriver.unregister();
            try {
                mButtonInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing button driver", e);
            } finally{
                mButtonInputDriver = null;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) { //29
            if (!mKeyPressed) {
                mKeyPressed = true;
                mResetHandler.postDelayed(mDisconnectRunnable, DISCONNECT_DELAY);
                mResetHandler.postDelayed(mResetRunnable, RESET_DELAY);

            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) { //29
            mKeyPressed = false;
            // No effect if these have already run
            mResetHandler.removeCallbacks(mDisconnectRunnable);
            mResetHandler.removeCallbacks(mResetRunnable);
            return true;
        }
        return handleKeyCode(keyCode) || super.onKeyUp(keyCode, event);
    }

    // For testing commands to the motors via ADB.
    private boolean handleKeyCode(int keyCode) {
        if (mCarController != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: //19
                    mCarController.onCarCommand(CarCommands.GO_FORWARD);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: //20
                    mCarController.onCarCommand(CarCommands.GO_BACK);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: //21
                    mCarController.onCarCommand(CarCommands.TURN_LEFT);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: //22
                    mCarController.onCarCommand(CarCommands.TURN_RIGHT);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER: //23
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

    private void setConnection(CompanionConnection connection) {
        if (mCompanionConnection != connection) {
            if (mCompanionConnection != null) {
                mCompanionConnection.getConnectionStateLiveData()
                        .removeObserver(mConnectionStateObserver);
            }
            mCompanionConnection = connection;
            if (mCompanionConnection != null) {
                mCompanionConnection.getConnectionStateLiveData()
                        .observe(this, mConnectionStateObserver);
            }
        }
    }

    private Observer<Integer> mConnectionStateObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer integer) {
            updateUi();
        }
    };

    private void updateUi() {
        if (mCompanionConnection != null) {
            if (mCompanionConnection.isConnected()) {
                mCarController.setLedColor(TricolorLed.GREEN);
                mCarController.display(mAdvertisingInfo.mRobocarId);
            } else if (mCompanionConnection.isAuthenticating()) {
                mCarController.setLedSequence(mAdvertisingInfo.mLedSequence);
                mCarController.display(mCompanionConnection.getAuthToken());
            }
        } else {
            mCarController.display(mAdvertisingInfo.mRobocarId);
            if (mIsAdvertising) {
                mCarController.setLedSequence(mAdvertisingInfo.mLedSequence);
            } else {
                mCarController.setLedColor(TricolorLed.YELLOW);
            }
        }
    }

    private Runnable mDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (mKeyPressed) {
                disconnectCompanion();
            }
        }
    };

    private Runnable mResetRunnable = new Runnable() {
        @Override
        public void run() {
            if (mKeyPressed) {
                reset();
            }
        }
    };

    private void disconnectCompanion() {
        mNearbyAdvertiser.disconnectCompanion();
        mNearbyAdvertiser.startAdvertising();
    }

    private void reset() {
        mNearbyAdvertiser.disconnectCompanion();
        mNearbyAdvertiser.stopAdvertising();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Remove the saved discoverer info.
        PreferenceUtils.clearDicovererInfo(prefs);
        mNearbyAdvertiser.setPairedDiscovererInfo(null);

        // Remove pair token from advertising info.
        mAdvertisingInfo = new AdvertisingInfo(mAdvertisingInfo.mRobocarId,
                mAdvertisingInfo.mLedSequence, null);
        PreferenceUtils.saveAdvertisingInfo(prefs, mAdvertisingInfo);
        mNearbyAdvertiser.setAdvertisingInfo(mAdvertisingInfo);

        // Start advertising after a delay so the display & LED changes are obvious to the user.
        mCarController.display(null);
        mResetHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNearbyAdvertiser.startAdvertising();
            }
        }, 1500L);
    }
}

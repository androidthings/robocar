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
package com.example.androidthings.robocar;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.androidthings.robocar.TricolorLed.Tricolor;
import com.example.androidthings.robocar.shared.CarCommands;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo.LedColor;
import com.example.motorhat.MotorHat;

import java.io.IOException;
import java.util.List;


public class CarController {

    private static final String TAG = "CarController";

    private static final int[] ALL_MOTORS = {0, 1, 2, 3};
    private static final int[] LEFT_MOTORS = {0, 2};
    private static final int[] RIGHT_MOTORS = {1, 3};

    private static final int SPEED_NORMAL = 100;
    private static final int SPEED_TURNING_INSIDE = 70;
    private static final int SPEED_TURNING_OUTSIDE = 250;

    private MotorHat mMotorHat;

    private TricolorLed mLed;
    private LedPatternBlinker mBlinker;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public CarController(MotorHat motorHat, TricolorLed led) {
        mMotorHat = motorHat;
        mLed = led;
        mHandlerThread = new HandlerThread("CarController-worker");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void shutDown() {
        stop();
        clearBlinker();
        mHandlerThread.quit();
    }

    // Motor controls

    public boolean onCarCommand(int command) {
        switch (command) {
            case CarCommands.GO_FORWARD:
                return goForward();
            case CarCommands.GO_BACK:
                return goBackward();
            case CarCommands.STOP:
                return stop();
            case CarCommands.TURN_LEFT:
                return turnLeft();
            case CarCommands.TURN_RIGHT:
                return turnRight();
        }
        return false;
    }

    private boolean goForward() {
        return setSpeed(SPEED_NORMAL) && setMotorState(MotorHat.MOTOR_STATE_CW, ALL_MOTORS);
    }

    private boolean goBackward() {
        return setSpeed(SPEED_NORMAL) && setMotorState(MotorHat.MOTOR_STATE_CCW, ALL_MOTORS);
    }

    private boolean stop() {
        return setMotorState(MotorHat.MOTOR_STATE_RELEASE, ALL_MOTORS);
    }

    private boolean turnLeft() {
        return turn(LEFT_MOTORS, RIGHT_MOTORS);
    }

    private boolean turnRight() {
        return turn(RIGHT_MOTORS, LEFT_MOTORS);
    }

    private boolean setMotorState(int state, int... motors) {
        try {
            if (motors != null && motors.length > 0) {
                for (int motor : motors) {
                    mMotorHat.setMotorState(motor, state);
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);
            return false;
        }
    }

    private boolean turn(int[] insideMotors, int[] outsideMotors) {
        try {
            setMotorState(MotorHat.MOTOR_STATE_CW, ALL_MOTORS);

            for (int motor : insideMotors) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_INSIDE);
            }
            for (int motor : outsideMotors) {
                mMotorHat.setMotorSpeed(motor, SPEED_TURNING_OUTSIDE);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting motor state", e);
            return false;
        }
    }

    private boolean setSpeed(int speed) {
        try {
            for (int motor : ALL_MOTORS) {
                mMotorHat.setMotorSpeed(motor, speed);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting speed", e);
            return false;
        }
    }

    // LED controls

    public void setLedColor(final @Tricolor int color) {
        clearBlinker();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLed.setColor(color);
            }
        });
    }

    public void setLedSequence(List<LedColor> colors) {
        clearBlinker();
        if (colors != null && !colors.isEmpty()) {
            final int size = colors.size() + 2;
            int[] pattern = new int[size];
            for (int i = 0; i < size - 2; i++) {
                LedColor color = colors.get(i);
                pattern[i] = TricolorLed.ledColorToTricolor(color);
            }
            // Add 2 OFF beats
            pattern[size - 2] = pattern[size - 1] = TricolorLed.OFF;
            blinkLed(pattern, LedPatternBlinker.REPEAT_INFINITE);
        }
    }

    private void blinkLed(int[] colors, int repeatCount) {
        clearBlinker();
        mBlinker = new LedPatternBlinker(colors, repeatCount);
        mHandler.post(mBlinker);
    }

    private void clearBlinker() {
        if (mBlinker != null) {
            mBlinker.mCanceled = true; // removeCallbacks() might not catch it in time.
            mHandler.removeCallbacks(mBlinker);
            mBlinker = null;
        }
    }

    private class LedPatternBlinker implements Runnable {

        static final long BLINK_MS = 400L;
        static final int REPEAT_INFINITE = -1;

        private final int[] mColors;
        private final int mRepeatCount;
        private boolean mCanceled;
        private int mCount = 0;
        private int mIndex = 0;

        LedPatternBlinker(int[] colors, int repeatCount) {
            mColors = colors;
            mRepeatCount = repeatCount;
        }

        @Override
        public void run() {
            if (mCanceled || mColors == null || mColors.length == 0) {
                return; // nothing to blink
            }
            if (mRepeatCount < 0 || mCount <= mRepeatCount) {
                mLed.setColor(mColors[mIndex++]);
                if (mIndex >= mColors.length) {
                    mIndex = 0;
                    mCount++;
                }

                mHandler.postDelayed(this, BLINK_MS);
            }
        }
    }
}

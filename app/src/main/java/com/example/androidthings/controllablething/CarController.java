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
package com.example.androidthings.controllablething;

import android.util.Log;

import com.example.androidthings.controllablething.shared.CarCommands;
import com.example.motorhat.MotorHat;

import java.io.IOException;

public class CarController {

    private static final String TAG = "CarController";

    private static final int[] ALL_MOTORS = {0, 1, 2, 3};
    private static final int[] LEFT_MOTORS = {0, 2};
    private static final int[] RIGHT_MOTORS = {1, 3};

    private int SPEED_NORMAL = 100;
    private int SPEED_TURNING_INSIDE = 70;
    private int SPEED_TURNING_OUTSIDE = 250;

    private MotorHat mMotorHat;

    public CarController(MotorHat motorHat) {
        mMotorHat = motorHat;
    }

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

    boolean goForward() {
        return setSpeed(SPEED_NORMAL) && setMotorState(MotorHat.MOTOR_STATE_CW, ALL_MOTORS);
    }

    boolean goBackward() {
        return setSpeed(SPEED_NORMAL) && setMotorState(MotorHat.MOTOR_STATE_CCW, ALL_MOTORS);
    }

    boolean stop() {
        return setMotorState(MotorHat.MOTOR_STATE_RELEASE, ALL_MOTORS);
    }

    boolean turnLeft() {
        return turn(LEFT_MOTORS, RIGHT_MOTORS);
    }

    boolean turnRight() {
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
}

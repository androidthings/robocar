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

import android.os.Build;

class BoardDefaults {
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX7D_PICO = "imx7d_pico";

    public static String getI2cBus() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "I2C1";
            case DEVICE_IMX7D_PICO:
                return "I2C1";
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    public static String[] getLedGpioPins() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return new String[]{"BCM5", "BCM6", "BCM12"};
            case DEVICE_IMX7D_PICO:
                return new String[]{"GPIO2_IO02", "GPIO2_IO07", "GPIO2_IO00"};
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    public static String getButtonGpioPin() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "BCM26";
            case DEVICE_IMX7D_PICO:
                return "GPIO2_IO01";
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    private BoardDefaults() {
        //no instance
    }
}

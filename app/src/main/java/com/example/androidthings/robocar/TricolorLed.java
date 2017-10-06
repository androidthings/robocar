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

import android.support.annotation.IntDef;
import android.util.Log;

import com.example.androidthings.robocar.shared.model.AdvertisingInfo.LedColor;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

public class TricolorLed implements AutoCloseable {

    private static final String TAG = "TricolorLed";

    public static final int OFF = 0;
    public static final int RED = 1; // __R
    public static final int GREEN = 2; // _G_
    public static final int YELLOW = 3; // _GR
    public static final int BLUE = 4; // B__
    public static final int MAGENTA = 5; // B_R
    public static final int CYAN = 6; // BG_
    public static final int WHITE = 7; // BGR

    @IntDef({OFF, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Tricolor{}

    private static final Map<LedColor, Integer> MAP;
    static {
        MAP = new HashMap<>();
        MAP.put(LedColor.RED, RED);
        MAP.put(LedColor.GREEN, GREEN);
        MAP.put(LedColor.YELLOW, YELLOW);
        MAP.put(LedColor.BLUE, BLUE);
        MAP.put(LedColor.MAGENTA, MAGENTA);
        MAP.put(LedColor.CYAN, CYAN);
        MAP.put(LedColor.WHITE, WHITE);
    }

    public static @Tricolor int ledColorToTricolor(LedColor color) {
        Integer v = MAP.get(color);
        return v == null ? OFF : v;
    }

    private Gpio mGpioRed;
    private Gpio mGpioGreen;
    private Gpio mGpioBlue;

    private @Tricolor int mColor = OFF;

    public TricolorLed(String redPin, String greenPin, String bluePin) {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mGpioRed = createGpio(pioService, redPin);
        mGpioGreen = createGpio(pioService, greenPin);
        mGpioBlue = createGpio(pioService, bluePin);
    }

    private Gpio createGpio(PeripheralManagerService pioService, String pin) {
        try {
            Gpio gpio = pioService.openGpio(pin);
            gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            return gpio;
        } catch (IOException e) {
            Log.e(TAG, "Error creating GPIO for pin " + pin, e);
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        setColor(OFF);
        closeGpio(mGpioRed);
        closeGpio(mGpioGreen);
        closeGpio(mGpioBlue);
        mGpioRed = mGpioGreen = mGpioBlue = null;
    }

    private void closeGpio(Gpio gpio) {
        if (gpio != null) {
            try {
                gpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing gpio", e);
            }
        }
    }

    public @Tricolor int getColor() {
        return mColor;
    }

    public void setColor(@Tricolor int color) {
        // only care about the 3 LSBs
        mColor = color & WHITE;
        // Common-Anode uses LOW to activate the color, so unset bits are set HIGH
        setGpioValue(mGpioRed, (color & 1) == 0);
        setGpioValue(mGpioGreen, (color & 2) == 0);
        setGpioValue(mGpioBlue, (color & 4) == 0);
    }

    private void setGpioValue(Gpio gpio, boolean value) {
        if (gpio != null) {
            try {
                gpio.setValue(value);
            } catch (IOException ignored) {
            }
        }
    }
}

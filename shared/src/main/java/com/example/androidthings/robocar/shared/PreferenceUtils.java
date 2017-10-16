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
package com.example.androidthings.robocar.shared;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo.LedColor;
import com.example.androidthings.robocar.shared.model.DiscovererInfo;

import java.util.List;

public class PreferenceUtils {

    private static final String KEY_ROBOCAR_ID = "robocar_id";
    private static final String KEY_ROBOCAR_LED_SEQUENCE = "robocar_led_sequence";
    private static final String KEY_ROBOCAR_PAIR_TOKEN = "robocar_pair_token";

    private static final String KEY_COMPANION_ID = "companion_id";
    private static final String KEY_COMPANION_PAIR_TOKEN = "companion_pair_token";

    public static void saveAdvertisingInfo(SharedPreferences prefs, AdvertisingInfo info) {
        if (info == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ROBOCAR_ID, info.mRobocarId)
                .putString(KEY_ROBOCAR_LED_SEQUENCE,
                        AdvertisingInfo.ledColorsToString(info.mLedSequence));
        if (info.mIsPaired) {
            editor.putString(KEY_ROBOCAR_PAIR_TOKEN, info.mPairToken);
        } else {
            editor.remove(KEY_ROBOCAR_PAIR_TOKEN);
        }
        editor.apply();
    }

    public static @Nullable AdvertisingInfo loadAdvertisingInfo(SharedPreferences prefs) {
        String id = prefs.getString(KEY_ROBOCAR_ID, null);
        List<LedColor> leds = AdvertisingInfo.stringToLedColors(
                prefs.getString(KEY_ROBOCAR_LED_SEQUENCE, null));
        if (id == null || (leds == null || leds.size() < 1)) {
            // insufficient or invalid data
            return null;
        }
        String pairToken = prefs.getString(KEY_ROBOCAR_PAIR_TOKEN, null);
        return new AdvertisingInfo(id, leds, pairToken);
    }

    public static void saveDiscovererInfo(SharedPreferences prefs, DiscovererInfo info) {
        if (info == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_COMPANION_ID, info.mCompanionId);
        if (info.mIsPaired) {
            editor.putString(KEY_COMPANION_PAIR_TOKEN, info.mPairToken);
        } else {
            editor.remove(KEY_COMPANION_PAIR_TOKEN);
        }
        editor.apply();
    }

    public static @Nullable DiscovererInfo loadDiscovererInfo(SharedPreferences prefs) {
        String id = prefs.getString(KEY_COMPANION_ID, null);
        if (id == null) {
            return null;
        }
        String pairToken = prefs.getString(KEY_COMPANION_PAIR_TOKEN, null);
        return new DiscovererInfo(id, pairToken);
    }

    public static void clearDicovererInfo(SharedPreferences prefs) {
        prefs.edit().remove(KEY_COMPANION_ID).remove(KEY_COMPANION_PAIR_TOKEN).apply();
    }
}

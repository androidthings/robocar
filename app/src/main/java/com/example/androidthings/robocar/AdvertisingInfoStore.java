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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.androidthings.robocar.shared.model.AdvertisingInfo;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo.LedColor;

import java.util.List;

/**
 * Responsible for generating and storing this Robocar's AdvertisingInfo.
 */
public class AdvertisingInfoStore {

    private static final String PREFS_KEY_ROBOCAR_ID = "robocar_id";
    private static final String PREFS_KEY_LED_SEQUENCE = "led_sequence";
    private static final String PREFS_KEY_PAIR_TOKEN = "pair_token";

    private static AdvertisingInfoStore sInstance;

    private final SharedPreferences mPreferences;
    private AdvertisingInfo mAdvertisingInfo;

    public static synchronized AdvertisingInfoStore getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AdvertisingInfoStore(context);
        }
        return sInstance;
    }

    public AdvertisingInfoStore(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
    }

    public AdvertisingInfo get() {
        if (mAdvertisingInfo == null) {
            AdvertisingInfo info = load();
            if (info == null) {
                info = AdvertisingInfo.generateAdvertisingInfo();
                save(info);
            }
            mAdvertisingInfo = info;
        }
        return mAdvertisingInfo;
    }

    private AdvertisingInfo load() {
        int id = mPreferences.getInt(PREFS_KEY_ROBOCAR_ID, -1);
        List<LedColor> leds = AdvertisingInfo.stringToLedColors(
                mPreferences.getString(PREFS_KEY_LED_SEQUENCE, null));
        if (id < 0 || (leds == null || leds.size() < 1)) {
            // insufficient or invalid data
            return null;
        }
        String pairToken = mPreferences.getString(PREFS_KEY_PAIR_TOKEN, null);
        return new AdvertisingInfo(id, leds, !TextUtils.isEmpty(pairToken), pairToken);
    }

    public void save(AdvertisingInfo info) {
        if (info == null) {
            return;
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(PREFS_KEY_ROBOCAR_ID, info.mRobocarId)
                .putString(PREFS_KEY_LED_SEQUENCE,
                        AdvertisingInfo.ledColorsToString(info.mLedSequence));
        if (info.mIsPaired) {
            editor.putString(PREFS_KEY_PAIR_TOKEN, info.mPairToken);
        } else {
            editor.remove(info.mPairToken);
        }
        editor.apply();

        mAdvertisingInfo = info;
    }
}

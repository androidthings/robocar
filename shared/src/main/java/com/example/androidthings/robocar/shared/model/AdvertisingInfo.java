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
package com.example.androidthings.robocar.shared.model;

import android.graphics.Color;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable class representing advertising data parsed from a Robocar's advertising name.
 */
public class AdvertisingInfo {
    private static final String ROBOCAR = "Robocar";
    private static final String SEPARATOR_COLON = ":";
    private static final String SEPARATOR_HYPHEN = "-";

    // Robocar:(\d{4}-\d{4}):([a-zA-Z-]+)(:(\S{5}))?
    private static final Pattern ADVERTISING_PATTERN = Pattern.compile(ROBOCAR + SEPARATOR_COLON
            + "(\\d{4}" + SEPARATOR_HYPHEN + "\\d{4})" + SEPARATOR_COLON
            + "([a-zA-z" + SEPARATOR_HYPHEN + "]+)(" + SEPARATOR_COLON + "(\\S{5}))?"
    );

    public enum LedColor {
        RED(Color.RED, "red"),
        GREEN(Color.GREEN, "green"),
        BLUE(Color.BLUE, "blue"),
        CYAN(Color.CYAN, "cyan"),
        MAGENTA(Color.MAGENTA, "magenta"),
        YELLOW(Color.YELLOW, "yellow"),
        WHITE(Color.WHITE, "white");

        final int mColor;
        final String mName; // TODO replace with resource IDs

        LedColor(int color, String name) {
            mColor = color;
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static AdvertisingInfo generateAdvertisingInfo() {
        return new AdvertisingInfo(generateId(), generateLedSequence(), null);
    }

    static String generateId() {
        // generate an 8 digit id in the form ####-####
        Random r = new Random();
        return String.format(Locale.US,
                "%04d" + SEPARATOR_HYPHEN + "%04d", r.nextInt(10000), r.nextInt(10000));
    }

    static List<LedColor> generateLedSequence() {
        final int size = 4;
        List<LedColor> list = new ArrayList<>(size);

        final LedColor[] colors = LedColor.values();
        Random random = new Random();
        LedColor previous = null;
        for (int i = 0; i < size; i++) {
            LedColor color = colors[random.nextInt(colors.length)];
            if (color == previous) {
                i--; // pick again
            } else {
                list.add(color);
                previous = color;
            }
        }
        return list;
    }

    public static String ledColorsToString(List<LedColor> colors) {
        if (colors == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (LedColor ledColor : colors) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR_HYPHEN);
            }
            builder.append(ledColor.name());
        }
        return builder.toString();
    }

    public static List<LedColor> stringToLedColors(String input) {
        if (input == null) {
            return Collections.emptyList();
        }
        String[] tokens = input.split(SEPARATOR_HYPHEN + "+");
        if (tokens.length < 1) {
            return Collections.emptyList();
        }
        List<LedColor> list = new ArrayList<>();
        for (String token : tokens) {
            try {
                LedColor color = LedColor.valueOf(token);
                list.add(color);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return list;
    }

    public static AdvertisingInfo parseAdvertisingName(String name) {
        Matcher matcher = ADVERTISING_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        String id = matcher.group(1);
        List<LedColor> colors = stringToLedColors(matcher.group(2));
        String pairToken = matcher.group(4);
        return new AdvertisingInfo(id, colors, pairToken);
    }

    public final String mRobocarId;
    public final List<LedColor> mLedSequence;

    public final String mPairToken;
    public final boolean mIsPaired;

    private int mHashcode; // cache hashcode

    public AdvertisingInfo(String robocarId, List<LedColor> ledSequence, String pairToken) {
        if (robocarId == null) {
            throw new IllegalArgumentException("Robocar ID cannot be null");
        }
        mRobocarId = robocarId;
        mLedSequence = Collections.unmodifiableList(ledSequence);
        mPairToken = pairToken;
        mIsPaired = !TextUtils.isEmpty(mPairToken);
    }

    public String getAdvertisingName() {
        StringBuilder builder = new StringBuilder();
        builder.append(ROBOCAR)
                .append(SEPARATOR_COLON)
                .append(mRobocarId)
                .append(SEPARATOR_COLON)
                .append(ledColorsToString(mLedSequence));
        if (mPairToken != null) {
            builder.append(SEPARATOR_COLON)
                    .append(mPairToken);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(AdvertisingInfo.class)) {
            return false;
        }
        AdvertisingInfo other = (AdvertisingInfo) obj;
        return mRobocarId.equals(other.mRobocarId)
                && mLedSequence.equals(other.mLedSequence)
                && TextUtils.equals(mPairToken, other.mPairToken);
    }

    @Override
    public int hashCode() {
        int h = mHashcode;
        if (h == 0) {
            h = mRobocarId.hashCode();
            h *= 1000003;
            h ^= mLedSequence.hashCode();
            h *= 1000003;
            h ^= mPairToken == null ? 0 : mPairToken.hashCode();
            mHashcode = h;
        }
        return mHashcode;
    }
}

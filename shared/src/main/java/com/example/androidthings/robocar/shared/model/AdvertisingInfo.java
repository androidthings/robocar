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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable class representing advertising data parsed from a Robocar's advertising name.
 */
public class AdvertisingInfo {
    private static final String ROBOCAR = "Robocar";
    private static final String SEGMENT_SEPARATOR = ":";
    private static final String LED_COLOR_SEPARATOR = "-";

    private static final int PAIRED = 1;
    private static final int UNPAIRED = 0;

    // Robocar:(\d{8}):([01]):([a-zA-Z-]+)(:(\S{5}))?
    private static final Pattern ADVERTISING_PATTERN = Pattern.compile(ROBOCAR + SEGMENT_SEPARATOR
            + "(\\d{8})" + SEGMENT_SEPARATOR
            + "([" + UNPAIRED + PAIRED + "])" + SEGMENT_SEPARATOR
            + "([a-zA-z" + LED_COLOR_SEPARATOR + "]+)(" + SEGMENT_SEPARATOR + "(\\S{5}))?"
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
        return new AdvertisingInfo(generateId(), generateLedSequence(), false, null);
    }

    static int generateId() {
        // generate an 8 digit id
        return 1000_0000 + (new Random().nextInt(9000_0000));
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
                builder.append(LED_COLOR_SEPARATOR);
            }
            builder.append(ledColor.name());
        }
        return builder.toString();
    }

    public static List<LedColor> stringToLedColors(String input) {
        if (input == null) {
            return Collections.emptyList();
        }
        String[] tokens = input.split(LED_COLOR_SEPARATOR + "+");
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
        int id = Integer.valueOf(matcher.group(1));
        boolean isPaired = Integer.valueOf(matcher.group(2)) == PAIRED;
        List<LedColor> colors = stringToLedColors(matcher.group(3));
        String pairToken = matcher.group(5);
        return new AdvertisingInfo(id, colors, isPaired, pairToken);
    }

    public final int mRobocarId;
    public final List<LedColor> mLedSequence;

    public final boolean mIsPaired;
    public final String mPairToken;

    public AdvertisingInfo(int robocarId, List<LedColor> ledSequence, boolean isPaired,
            String pairToken) {
        mRobocarId = robocarId;
        mLedSequence = Collections.unmodifiableList(ledSequence);
        mIsPaired = isPaired;
        mPairToken = pairToken;
    }

    public String getAdvertisingName() {
        StringBuilder builder = new StringBuilder();
        builder.append(ROBOCAR)
                .append(SEGMENT_SEPARATOR)
                .append(mRobocarId)
                .append(SEGMENT_SEPARATOR)
                .append(mIsPaired ? PAIRED : UNPAIRED)
                .append(SEGMENT_SEPARATOR)
                .append(ledColorsToString(mLedSequence));
        if (mPairToken != null) {
            builder.append(SEGMENT_SEPARATOR)
                    .append(mPairToken);
        }
        return builder.toString();
    }
}

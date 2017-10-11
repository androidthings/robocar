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

import android.text.TextUtils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscovererInfo {

    private static final String ROBOCAR_COMPANION = "RobocarCompanion";
    private static final String SEPARATOR_COLON = ":";
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final int ID_LENGTH = 12;

    // RobocarCompanion:([0123456789abcdef]{12})(:(\S{5}))?
    private static final Pattern COMPANION_PATTERN = Pattern.compile(ROBOCAR_COMPANION
            + SEPARATOR_COLON + "([" + HEX_CHARS + "]{" + ID_LENGTH + "})("
            + SEPARATOR_COLON + "(\\S{5}))?"
    );

    private static String generateId() {
        char[] c = new char[ID_LENGTH];
        Random r = new Random();
        for (int i = 0; i < c.length; i++) {
            c[i] = HEX_CHARS.charAt(r.nextInt(HEX_CHARS.length()));
        }
        return new String(c);
    }

    public static DiscovererInfo generateDiscoveryInfo() {
        return new DiscovererInfo(generateId(), null);
    }

    public static DiscovererInfo parse(String name) {
        Matcher matcher = COMPANION_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        String id = matcher.group(1);
        String pairToken = matcher.group(3);
        return new DiscovererInfo(id, pairToken);
    }

    public final String mCompanionId;
    public final String mPairToken;
    public final boolean mIsPaired;

    private int mHashcode; // cache hashcode

    public DiscovererInfo(String companionId, String pairToken) {
        if (companionId == null) {
            throw new IllegalArgumentException("Companion ID cannot be null");
        }
        mCompanionId = companionId;
        mPairToken = pairToken;
        mIsPaired = !TextUtils.isEmpty(mPairToken);
    }

    public String getAdvertisingName() {
        StringBuilder builder = new StringBuilder(ROBOCAR_COMPANION)
                .append(SEPARATOR_COLON)
                .append(mCompanionId);
        if (mIsPaired) {
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
        if (obj == null || !obj.getClass().equals(DiscovererInfo.class)) {
            return false;
        }
        DiscovererInfo other = (DiscovererInfo) obj;
        return mCompanionId.equals(other.mCompanionId)
                && TextUtils.equals(mPairToken, other.mPairToken);
    }

    @Override
    public int hashCode() {
        int h = mHashcode;
        if (h == 0) {
            h = mCompanionId.hashCode();
            h *= 1000003;
            h ^= mPairToken == null ? 0 : mPairToken.hashCode();
            mHashcode = h;
        }
        return mHashcode;
    }
}

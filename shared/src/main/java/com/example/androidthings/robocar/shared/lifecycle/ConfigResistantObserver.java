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
package com.example.androidthings.robocar.shared.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Handler;


/**
 * {@link LifecycleObserver} that avoids spurious stops and starts due to configuration changes.
 * Generally this would be used with a retained Fragment. If using with an Activity, take note that
 * the same instance of the observer needs to be used by the Activity instances on either side of a
 * configuration change.
 * <p>
 * This trick is borrowed from {@link android.arch.lifecycle.ProcessLifecycleOwner}.
 */
public class ConfigResistantObserver implements LifecycleObserver {

    private static final long TIMEOUT_MS = 700; //ms

    private Handler mHandler = new Handler();
    private boolean mStopSent = true;
    private int mStartCount = 0;

    private Runnable mDelayedStopRunnable = new Runnable() {
        @Override
        public void run() {
            if (mStartCount == 0) {
                mStopSent = true;
                onReallyStop();
            }
        }
    };

    @OnLifecycleEvent(Event.ON_START)
    public final void onStart() {
        mStartCount++;
        if (mStartCount == 1 && mStopSent) {
            mStopSent = false;
            onReallyStart();
        } else {
            mHandler.removeCallbacks(mDelayedStopRunnable);
        }
    }

    @OnLifecycleEvent(Event.ON_STOP)
    public final void onStop() {
        mStartCount--;
        if (mStartCount == 0) {
            mHandler.postDelayed(mDelayedStopRunnable, TIMEOUT_MS);
        }
    }

    protected void onReallyStart() {}

    protected void onReallyStop() {}
}

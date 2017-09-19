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
package com.example.androidthings.controllablething.shared;

import android.os.Handler;

/**
 * This class allows creating a lifecycle "scope" around other, potentially overlapping,
 * lifecycles. The intended use is to watch Activity starts and stops to avoid thrashing state
 * when a configuration change occurs.
 * <br/><br/>
 * Usage: Activities that want to share a common "scope", beginning with the first start and ending
 * with last stop among them, should report to the same instance of LifecycleWatcher in their
 * onStart() and onStop() callbacks. Note: these calls <b>must</b> be balanced or this class will
 * not work properly.
 *
 */
// TODO: replace with a LifecycleObserver from Architecture Components.
public abstract class LifecycleWatcher {

    private static final long TIMEOUT_MILLIS = 700;

    private int mStartCount = 0;
    private Handler mHandler;
    private final Runnable mDelayStopRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive()) {
                onInactive();
            }
        }
    };

    protected LifecycleWatcher() {
        mHandler = new Handler();
    }

    public final void start() {
        mStartCount++;
        if (mStartCount == 1) {
            // We've just been started.
            mHandler.removeCallbacks(mDelayStopRunnable);
            onActive();
        }
    }

    public final void stop() {
        mStartCount--;
        if (mStartCount == 0) {
            // Our scope is disappearing, but this could be spurious if we're undergoing a
            // configuration change.
            mHandler.postDelayed(mDelayStopRunnable, TIMEOUT_MILLIS);
        }
    }

    public boolean isActive() {
        return mStartCount > 0;
    }

    protected void onActive() {}

    protected void onInactive() {}
}

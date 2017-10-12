/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.robocar.companion;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.androidthings.robocar.companion.CompanionViewModel.NavigationState;
import com.example.androidthings.robocar.shared.ConnectorFragment;
import com.example.androidthings.robocar.shared.ConnectorFragment.ConnectorCallbacks;
import com.example.androidthings.robocar.shared.PreferenceUtils;
import com.example.androidthings.robocar.shared.model.DiscovererInfo;
import com.google.android.gms.common.ConnectionResult;


public class CompanionActivity extends AppCompatActivity implements ConnectorCallbacks {

    private static final String TAG = "CompanionActivity";

    private static final String FRAGMENT_TAG_DISCOVERY = "fragment.robocar_discovery";
    private static final String FRAGMENT_TAG_CONTROLLER = "fragment.robocar_controller";
    private static final String SAVEDSTATE_FRAGMENT_TAG = "savedstate.fragment_tag";

    private static final int REQUEST_RESOLVE_CONNECTION = 1;

    private CompanionViewModel mViewModel;

    private RobocarDiscoveryFragment mDiscoveryFragment;
    private ControllerFragment mControllerFragment;
    private Fragment mCurrentFragment;
    private String mCurrentFragmentTag;

    private DiscovererInfo mDiscovererInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companion);

        // init DiscovererInfo
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDiscovererInfo = PreferenceUtils.loadDiscovererInfo(prefs);
        if (mDiscovererInfo == null) {
            mDiscovererInfo = DiscovererInfo.generateDiscoveryInfo();
            PreferenceUtils.saveDiscovererInfo(prefs, mDiscovererInfo);
        }

        mViewModel = ViewModelProviders.of(this).get(CompanionViewModel.class);

        if (savedInstanceState == null) {
            // First launch. Attach the connector fragment and give it our client to connect.
            ConnectorFragment.attachTo(this, mViewModel.getGoogleApiClient());
        } else {
            // Re-acquire references to attached fragments.
            FragmentManager fm = getSupportFragmentManager();
            mDiscoveryFragment =
                    (RobocarDiscoveryFragment) fm.findFragmentByTag(FRAGMENT_TAG_DISCOVERY);
            mControllerFragment =
                    (ControllerFragment) fm.findFragmentByTag(FRAGMENT_TAG_CONTROLLER);

            mCurrentFragmentTag = savedInstanceState.getString(SAVEDSTATE_FRAGMENT_TAG);
            if (FRAGMENT_TAG_DISCOVERY.equals(mCurrentFragmentTag)) {
                mCurrentFragment = mDiscoveryFragment;
            } else if (FRAGMENT_TAG_CONTROLLER.equals(mCurrentFragmentTag)) {
                mCurrentFragment = mControllerFragment;
            }
        }

        RobocarDiscoverer discoverer = mViewModel.getRobocarDiscoverer();
        discoverer.setDiscovererInfo(mDiscovererInfo);
        discoverer.setPairedAdvertisingInfo(PreferenceUtils.loadAdvertisingInfo(prefs));
        mViewModel.getNavigationStateLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer value) {
                assert value != null;
                if (value == NavigationState.DISCOVERY_UI) {
                    showDiscoveryUi();
                } else if (value == NavigationState.CONTROLLER_UI) {
                    showControllerUi();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVEDSTATE_FRAGMENT_TAG, mCurrentFragmentTag);
    }

    private void showDiscoveryUi() {
        if (mDiscoveryFragment == null) {
            mDiscoveryFragment = new RobocarDiscoveryFragment();
        }
        swapFragment(mDiscoveryFragment, FRAGMENT_TAG_DISCOVERY);
    }

    private void showControllerUi() {
        if (mControllerFragment == null) {
            mControllerFragment = new ControllerFragment();
        }
        swapFragment(mControllerFragment, FRAGMENT_TAG_CONTROLLER);
    }

    private void swapFragment(Fragment fragment, String tag) {
        if (mCurrentFragment != fragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();
            mCurrentFragment = fragment;
            mCurrentFragmentTag = tag;
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment == mControllerFragment) {
            mControllerFragment.disconnect();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onGoogleApiConnected(Bundle bundle) {}

    @Override
    public void onGoogleApiConnectionSuspended(int cause) {}

    @Override
    public void onGoogleApiConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_CONNECTION);
            } catch (SendIntentException e) {
                Log.e(TAG, "Google API connection failed. " + connectionResult, e);
            }
        } else {
            Log.e(TAG, "Google API connection failed. " + connectionResult);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_CONNECTION) {
            if (resultCode == RESULT_OK) {
                // try to reconnect
                ConnectorFragment.connect(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

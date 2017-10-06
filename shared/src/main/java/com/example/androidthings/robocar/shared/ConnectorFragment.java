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


import android.Manifest;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.Lifecycle.State;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;

import com.example.androidthings.robocar.shared.lifecycle.ConfigResistantObserver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


/**
 * Fragment that handles connecting with GoogleClientApi. It is retained across configuration
 * changes, so there won't be spurious disconnecting and reconnecting when the user does
 * something like rotate the screen.
 * <br/><br/>
 * Usage: call {@link #attachTo(FragmentActivity, GoogleApiClient)} from your Activity. You need not
 * check that there is already one of these attached.
 */
public class ConnectorFragment extends Fragment implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String FRAGMENT_TAG =
            "com.example.androidthings.robocar.shared.ConnectorFragment";

    private static final int REQUEST_PERMISSIONS = 1;

    private GoogleApiClient mGoogleApiClient;
    private ConnectorCallbacks mCallbacks;
    private LifecycleObserver mLifecycleObserver = new ConfigResistantObserver() {
        @Override
        protected void onReallyStart() {
            connect();
        }

        @Override
        protected void onReallyStop() {
            disconnect();
        }

        @OnLifecycleEvent(Event.ON_CREATE)
        public void create() {
            mGoogleApiClient.registerConnectionCallbacks(ConnectorFragment.this);
            mGoogleApiClient.registerConnectionFailedListener(ConnectorFragment.this);
        }

        @OnLifecycleEvent(Event.ON_DESTROY)
        public void destroy() {
            mGoogleApiClient.unregisterConnectionCallbacks(ConnectorFragment.this);
            mGoogleApiClient.unregisterConnectionFailedListener(ConnectorFragment.this);
        }
    };

    public interface ConnectorCallbacks {
        void onGoogleApiConnected(Bundle bundle);
        void onGoogleApiConnectionSuspended(int cause);
        void onGoogleApiConnectionFailed(ConnectionResult connectionResult);
    }

    public static void attachTo(FragmentActivity activity, GoogleApiClient client) {
        FragmentManager fm = activity.getSupportFragmentManager();
        ConnectorFragment fragment = get(fm);
        if (fragment == null) {
            fragment = newInstance(client);
            fm.beginTransaction().add(fragment, FRAGMENT_TAG).commitAllowingStateLoss();
        }
    }

    public static void connect(FragmentActivity activity) {
        ConnectorFragment fragment = get(activity.getSupportFragmentManager());
        if (fragment != null) {
            fragment.connect();
        }
    }

    private static ConnectorFragment get(FragmentManager fm) {
        if (fm.isDestroyed()) {
            throw new IllegalStateException("Can't get fragment after onDestroy");
        }

        Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null && !(fragment instanceof ConnectorFragment)) {
            throw new IllegalStateException("Unexpected fragment instance was returned by tag");
        }
        return (ConnectorFragment) fragment;
    }

    private static ConnectorFragment newInstance(GoogleApiClient client) {
        ConnectorFragment fragment = new ConnectorFragment();
        fragment.mGoogleApiClient = client;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConnectorCallbacks) {
            mCallbacks = (ConnectorCallbacks) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getLifecycle().addObserver(mLifecycleObserver);
    }

    private void connect() {
        if (getLifecycle().getCurrentState().isAtLeast(State.STARTED)
                && !mGoogleApiClient.isConnected()
                && !mGoogleApiClient.isConnecting()) {
            if (hasPermissions()) {
                mGoogleApiClient.connect();
            } else {
                requestPermissions();
            }
        }
    }

    private void disconnect() {
        if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.disconnect();
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        super.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connect();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCallbacks != null) {
            mCallbacks.onGoogleApiConnected(bundle);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (mCallbacks != null) {
            mCallbacks.onGoogleApiConnectionSuspended(cause);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mCallbacks != null) {
            mCallbacks.onGoogleApiConnectionFailed(connectionResult);
        }
    }
}

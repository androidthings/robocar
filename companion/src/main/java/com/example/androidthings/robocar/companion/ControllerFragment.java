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
package com.example.androidthings.robocar.companion;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidthings.robocar.companion.CompanionViewModel.NavigationState;
import com.example.androidthings.robocar.shared.CarCommands;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;


public class ControllerFragment extends Fragment {

    private static final String TAG = "ControllerFragment";

    private SparseArray<View> mCarControlMap = new SparseArray<>(5);
    private View mActivatedControl;
    private View mErrorView;
    private TextView mLogView;

    private CompanionViewModel mViewModel;
    private RobocarDiscoverer mRobocarDiscoverer;
    private RobocarConnection mRobocarConnection;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_controller, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mErrorView = view.findViewById(R.id.error);
        mLogView = view.findViewById(R.id.log_text);

        configureButton(view, R.id.btn_forward, CarCommands.GO_FORWARD);
        configureButton(view, R.id.btn_back, CarCommands.GO_BACK);
        configureButton(view, R.id.btn_left, CarCommands.TURN_LEFT);
        configureButton(view, R.id.btn_right, CarCommands.TURN_RIGHT);
        configureButton(view, R.id.btn_stop, CarCommands.STOP);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(CompanionViewModel.class);
        mRobocarDiscoverer = mViewModel.getRobocarDiscoverer();

        mRobocarDiscoverer.getRobocarConnectionLiveData().observe(this,
                new Observer<RobocarConnection>() {
                    @Override
                    public void onChanged(@Nullable RobocarConnection connection) {
                        mRobocarConnection = connection;
                        if (connection == null || !connection.isConnected()) {
                            // We're not connected, so go back to discovery UI
                            mViewModel.navigateTo(NavigationState.DISCOVERY_UI);
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mRobocarDiscoverer.setPayloadListener(mPayloadListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRobocarDiscoverer.setPayloadListener(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.controller, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_disconnect) {
            disconnect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void disconnect() {
        if (mRobocarConnection != null) {
            mRobocarConnection.disconnect();
        }
    }

    void configureButton(View view, int buttonId, final byte command) {
        View button = view.findViewById(buttonId);
        if (button != null) {
            mCarControlMap.append(command, button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRobocarConnection.sendCommand(command);
                    setActivatedControl(v);
                }
            });
        }
    }

    private void setActivatedControl(View view) {
        if (mActivatedControl != null && mActivatedControl != view) {
            mActivatedControl.setActivated(false);
        }
        mActivatedControl = view;
        if (mActivatedControl != null) {
            mActivatedControl.setActivated(true);
        }
    }

    private void logUi(String text) {
        if (mLogView.getText().length() > 0) {
            mLogView.append("\n");
        }
        mLogView.append(text);
    }

    PayloadCallback mPayloadListener = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            byte[] bytes = CarCommands.fromPayload(payload);
            if (bytes == null || bytes.length == 0) {
                return;
            }

            byte command = bytes[0];
            if (command == CarCommands.ERROR) {
                mErrorView.setVisibility(View.VISIBLE);
                Log.d(TAG, "onPayloadReceived: error");
            } else {
                mErrorView.setVisibility(View.GONE);
                Log.d(TAG, "onPayloadReceived: " + command);
                // activate control
                View toActivate = mCarControlMap.get(command);
                setActivatedControl(toActivate);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpoitnId, PayloadTransferUpdate xferUpdate) {
        }
    };
}

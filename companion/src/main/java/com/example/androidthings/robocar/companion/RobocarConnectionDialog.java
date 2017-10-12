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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.example.androidthings.robocar.companion.CompanionViewModel.NavigationState;
import com.example.androidthings.robocar.shared.NearbyConnection.ConnectionState;
import com.example.androidthings.robocar.shared.model.AdvertisingInfo;


/**
 * DialogFragment used for connecting to a Robocar.
 */
public class RobocarConnectionDialog extends DialogFragment implements OnClickListener {

    private TextView mMessageText;
    private View mPositiveButton;
    private View mNegativeButton;
    private View mProgressContainer;
    private TextView mProgressText;

    private CompanionViewModel mViewModel;
    private RobocarConnection mRobocarConnection;

    @ConnectionState
    private int mState = ConnectionState.NOT_CONNECTED;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_connect_to_robocar)
                .create();
        // Inflate the content separately so that we can use findViewById()
        @SuppressLint("InflateParams")
        View view = dialog.getLayoutInflater().inflate(R.layout.fragment_auth_dialog, null, false);
        dialog.setView(view);
        dialog.setCanceledOnTouchOutside(false); // can still cancel with Back

        mMessageText = view.findViewById(android.R.id.message);
        mPositiveButton = view.findViewById(R.id.positive_button);
        mNegativeButton = view.findViewById(R.id.negative_button);
        mProgressContainer = view.findViewById(R.id.progress_container);
        mProgressText = view.findViewById(R.id.progress_text);

        mPositiveButton.setOnClickListener(this);
        mNegativeButton.setOnClickListener(this);

        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(getActivity()).get(CompanionViewModel.class);
        mRobocarConnection = mViewModel.getRobocarDiscoverer()
                .getRobocarConnectionLiveData().getValue();
        if (mRobocarConnection == null || mRobocarConnection.isConnected()) {
            // Don't need to show
            dismiss();
            return;
        }

        mRobocarConnection.getConnectionStateLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer value) {
                //noinspection ConstantConditions
                setState(value);
            }
        });

        AdvertisingInfo info = mRobocarConnection.getAdvertisingInfo();
        getDialog().setTitle(getString(R.string.dialog_title_connect_to_robocar,
                info.mRobocarId));
        updateUi();
    }

    private void setState(@ConnectionState int state) {
        if (mState == state) {
            return;
        }

        if (state == ConnectionState.CONNECTED) {
            // TODO save Robocar info for automatic reconnect
            dismiss();
            mViewModel.navigateTo(NavigationState.CONTROLLER_UI);
            return;
        } else if (state == ConnectionState.NOT_CONNECTED) {
            // TODO trigger a Snackbar to show error message based on the prior state
            dismiss();
            mViewModel.navigateTo(NavigationState.DISCOVERY_UI);
            return;
        }

        mState = state;
        updateUi();
    }

    private void updateUi() {
        if (mState == ConnectionState.REQUESTING) {
            mMessageText.setVisibility(View.GONE);
            mPositiveButton.setVisibility(View.GONE);
            mNegativeButton.setVisibility(View.GONE);
            mProgressContainer.setVisibility(View.VISIBLE);
            mProgressText.setText(R.string.dialog_message_requesting_connection);
        } else if (mState == ConnectionState.AUTHENTICATING
                || mState == ConnectionState.AUTH_ACCEPTED) {
            showMessageText();
            boolean showProgress = mState == ConnectionState.AUTH_ACCEPTED;
            mPositiveButton.setVisibility(showProgress ? View.GONE : View.VISIBLE);
            mNegativeButton.setVisibility(showProgress ? View.GONE : View.VISIBLE);
            mProgressContainer.setVisibility(showProgress ? View.VISIBLE : View.GONE);
            mProgressText.setText(R.string.dialog_message_authenticating);
        }
    }

    private void showMessageText() {
        AdvertisingInfo info = mRobocarConnection.getAdvertisingInfo();
        mMessageText.setText(getString(R.string.dialog_message_auth_instructions,
                info.mRobocarId,
                AdvertisingInfo.ledColorsToString(info.mLedSequence),
                mRobocarConnection.getAuthToken()));
        mMessageText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.positive_button:
                mRobocarConnection.accept();
                break;
            case R.id.negative_button:
                dismiss();
                mRobocarConnection.reject();
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        // We shouldn't be showing if we're connected, but in case we are, let's not drop the
        // connection since the user should be expecting to see the controls.
        if (!mRobocarConnection.isConnected()) {
            if (mRobocarConnection.getState() == ConnectionState.AUTHENTICATING) {
                mRobocarConnection.reject();
            } else {
                // This will clear the connection even if not fully connected.
                mRobocarConnection.disconnect();
            }
        }
    }
}

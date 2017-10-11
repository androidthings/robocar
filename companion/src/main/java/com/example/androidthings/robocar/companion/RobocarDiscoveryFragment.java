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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.androidthings.robocar.companion.CompanionViewModel.NavigationState;

import java.util.List;


public class RobocarDiscoveryFragment extends Fragment {

    private static final String FRAGMENT_TAG_AUTH_DIALOG = "fragment.robocar_auth_dialog";

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private RobocarEndpointsAdapter mAdapter;

    private CompanionViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discoverer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(android.R.id.list);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(
                mRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        mEmptyView = view.findViewById(android.R.id.empty);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(getActivity()).get(CompanionViewModel.class);
        RobocarDiscoverer discoverer = mViewModel.getRobocarDiscoverer();

        mAdapter = new RobocarEndpointsAdapter(discoverer);
        mRecyclerView.setAdapter(mAdapter);
        discoverer.getRobocarEndpointsLiveData().observe(this,
                new Observer<List<RobocarEndpoint>>() {
                    @Override
                    public void onChanged(@Nullable List<RobocarEndpoint> list) {
                        updateList(list);
                    }
                });

        discoverer.getRobocarConnectionLiveData().observe(this, new Observer<RobocarConnection>() {
            @Override
            public void onChanged(@Nullable RobocarConnection connection) {
                clearAuthDialog();
                if (connection != null) {
                    if (connection.isConnected()) {
                        // Advance to controller UI
                        mViewModel.navigateTo(NavigationState.CONTROLLER_UI);
                    } else {
                        showAuthDialog();
                    }
                }
            }
        });
    }

    private void updateList(List<RobocarEndpoint> list) {
        boolean empty = list == null || list.isEmpty();
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(empty ? View.GONE :  View.VISIBLE);
        mAdapter.setItems(list);
    }

    private void clearAuthDialog() {
        Fragment f = getFragmentManager().findFragmentByTag(FRAGMENT_TAG_AUTH_DIALOG);
        if (f != null) {
            getFragmentManager().beginTransaction().remove(f).commit();
        }
    }

    private void showAuthDialog() {
        RobocarConnectionDialog dialog = new RobocarConnectionDialog();
        dialog.show(getFragmentManager(), FRAGMENT_TAG_AUTH_DIALOG);
    }
}

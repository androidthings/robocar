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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidthings.robocar.shared.model.AdvertisingInfo;

import java.util.List;

public class RobocarEndpointsAdapter
        extends RecyclerView.Adapter<RobocarEndpointsAdapter.RobocarEndpointViewHolder> {

    private List<RobocarEndpoint> mList;
    private RobocarDiscoverer mRobocarDiscoverer;

    public RobocarEndpointsAdapter(RobocarDiscoverer robocarDiscoverer) {
        mRobocarDiscoverer = robocarDiscoverer;
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public RobocarEndpointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.list_item_robocar_endpoint, parent, false);
        return new RobocarEndpointViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RobocarEndpointViewHolder holder, int position) {
        holder.bind(mList.get(position));
    }

    public void setItems(List<RobocarEndpoint> newList) {
        mList = newList;
        notifyDataSetChanged();
    }

    class RobocarEndpointViewHolder extends ViewHolder implements OnClickListener {

        private final TextView mNameView;
        private final TextView mColorPatternView;

        private RobocarEndpoint mRobocarEndpoint;

        public RobocarEndpointViewHolder(View itemView) {
            super(itemView);
            mNameView = itemView.findViewById(R.id.name);
            mColorPatternView = itemView.findViewById(R.id.color_pattern);
            itemView.setOnClickListener(this);
        }

        public void bind(RobocarEndpoint item) {
            mRobocarEndpoint = item;
            mNameView.setText(mNameView.getResources()
                    .getString(R.string.robocar_name, item.mAdvertisingInfo.mRobocarId));
            mColorPatternView.setText(
                    AdvertisingInfo.ledColorsToString(item.mAdvertisingInfo.mLedSequence));

            boolean enabled = canConnect();
            mNameView.setEnabled(enabled);
            mColorPatternView.setEnabled(enabled);
        }

        private boolean canConnect() {
            return !mRobocarEndpoint.mIsPaired || mRobocarEndpoint.mIsRemembered;
        }

        @Override
        public void onClick(View view) {
            if (canConnect()) {
                mRobocarDiscoverer.requestConnection(mRobocarEndpoint.mEndpointId);
            } else {
                // TODO show dialog explaining how to reset Robocar
            }
        }
    }
}

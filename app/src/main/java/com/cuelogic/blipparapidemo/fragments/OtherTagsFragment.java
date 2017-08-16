/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.cuelogic.blipparapidemo.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cuelogic.blipparapidemo.R;
import com.cuelogic.blipparapidemo.models.Tag;

import java.util.List;


/**
 * A simple dialog that allows user to pick an aspect ratio.
 */
public class OtherTagsFragment extends DialogFragment {

    private static final String ARG_TAGS = "tags";
    private static final String ARG_CURRENT_ASPECT_RATIO = "current_aspect_ratio";

    private Listener mListener;

    public static OtherTagsFragment newInstance(List<Tag> tags) {
        final OtherTagsFragment fragment = new OtherTagsFragment();
        final Bundle args = new Bundle();
        args.putParcelableArray(ARG_TAGS,
                tags.toArray(new Tag[tags.size()]));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (Listener) context;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final Tag[] tags = (Tag[]) args.getParcelableArray(ARG_TAGS);
        if (tags == null) {
            throw new RuntimeException("No tags");
        }
        final TagsAdapter adapter = new TagsAdapter(tags);
        return new AlertDialog.Builder(getActivity())
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        mListener.onTagSelected(tags[position]);
                    }
                })
                .create();
    }

    private static class TagsAdapter extends BaseAdapter {

        private final Tag[] mTags;

        TagsAdapter(Tag[] tags) {
            mTags = tags;
        }

        @Override
        public int getCount() {
            return mTags.length;
        }

        @Override
        public Tag getItem(int position) {
            return mTags[position];
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TagsAdapter.ViewHolder holder;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.each_tags_item, parent, false);
                holder = new TagsAdapter.ViewHolder();
                holder.text = (TextView) view.findViewById(android.R.id.text1);
                view.setTag(holder);
            } else {
                holder = (TagsAdapter.ViewHolder) view.getTag();
            }
            Tag tag = getItem(position);
            StringBuilder sb = new StringBuilder(tag.toString());
            holder.text.setText(sb);
            return view;
        }

        private static class ViewHolder {
            TextView text;
        }

    }

    public interface Listener {
        void onTagSelected(@NonNull Tag tag);
    }

}

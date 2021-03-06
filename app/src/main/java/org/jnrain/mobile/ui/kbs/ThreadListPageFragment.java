/*
 * Copyright 2012-2013 JNRain
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jnrain.mobile.ui.kbs;

import org.jnrain.kbs.collection.ListPosts;
import org.jnrain.kbs.entity.Post;
import org.jnrain.mobile.R;
import org.jnrain.mobile.network.requests.ThreadListRequest;
import org.jnrain.mobile.util.CacheKeyManager;
import org.jnrain.mobile.util.GlobalState;

import roboguice.inject.InjectView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;


public class ThreadListPageFragment extends RoboSherlockFragment {
    private static final String TAG = "ThreadListFragment";

    @InjectView(R.id.listPosts)
    ListView listPosts;

    private static final String BOARD_ID_STORE = "_brdId";
    private static final String POSTS_STORE = "_posts";

    private ThreadListFragmentListener _threadListListener;
    private String _brd_id;
    private ListPosts _posts;
    private int _page;

    private ThreadListAdapter _adapter;

    public ThreadListPageFragment(String brd_id, int page) {
        super();

        _brd_id = brd_id;
        _page = page;

        _posts = null;
    }

    public ThreadListPageFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _threadListListener = (ThreadListFragmentListener) getParentFragment();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.frag_kbs_thread_list,
                container,
                false);

        if (savedInstanceState != null) {
            _brd_id = savedInstanceState.getString(BOARD_ID_STORE);
            _posts = (ListPosts) savedInstanceState
                .getSerializable(POSTS_STORE);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (_posts != null) {
            updateData();
        } else {
            // fetch a page of thread list
            makeRequest(_page);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(BOARD_ID_STORE, _brd_id);
        outState.putSerializable(POSTS_STORE, _posts);
    }

    public void makeRequest(int page) {
        _threadListListener.makeSpiceRequest(
                new ThreadListRequest(_brd_id, page),
                CacheKeyManager.keyForPagedThreadList(
                        _brd_id,
                        page,
                        GlobalState.getUserName()),
                DurationInMillis.ONE_MINUTE,
                new ThreadListRequestListener());
    }

    public synchronized void updateData() {
        if (getActivity() == null) {
            return;
        }

        _adapter = new ThreadListAdapter(this
            .getActivity()
            .getApplicationContext(), _posts);
        listPosts.setAdapter(_adapter);

        listPosts
            .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(
                        AdapterView<?> parent,
                        View view,
                        int position,
                        long id) {
                    Post post = _posts.getPosts().get(position);

                    Log.i(TAG, "clicked: " + position + ", id=" + id
                            + ", post=" + post.toString());

                    _threadListListener.showReadThreadUI(post);
                }
            });
    }

    private class ThreadListRequestListener
            implements RequestListener<ListPosts> {
        @Override
        public void onRequestFailure(SpiceException arg0) {
            Log.d(TAG, "err on req: " + arg0.toString());
        }

        @Override
        public void onRequestSuccess(ListPosts posts) {
            Log.v(TAG, "got posts list: " + posts.toString());
            _posts = posts;

            updateData();
        }
    }
}

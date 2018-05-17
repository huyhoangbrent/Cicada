package com.mediaplayer.Cicada.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mediaplayer.Cicada.R;
import com.mediaplayer.Cicada.adapters.SongsListAdapter;
import com.mediaplayer.Cicada.beans.Track;
import com.mediaplayer.Cicada.utilities.MediaLibraryManager;
import com.mediaplayer.Cicada.utilities.Utilities;

import java.util.ArrayList;

import static com.mediaplayer.Cicada.utilities.MediaPlayerConstants.LOG_TAG_EXCEPTION;

public class SongsFragment extends Fragment {
    private Context context;
    public static RecyclerView trackListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        TextView emptyLibraryMessage;
        ArrayList<Track> trackInfoList;
        RecyclerView.Adapter songsListAdapter;

        try {
            view = inflater.inflate(R.layout.fragment_songs, container, false);
            emptyLibraryMessage = (TextView) view.findViewById(R.id.emptyLibraryMessage);
            trackListView = (RecyclerView) view.findViewById(R.id.recycler_view);
            trackInfoList = MediaLibraryManager.getTrackInfoList();

            if(trackInfoList == null || trackInfoList.isEmpty()) {
                emptyLibraryMessage.setVisibility(View.VISIBLE);
                trackListView.setVisibility(View.GONE);
            } else {
                songsListAdapter = new SongsListAdapter(context, trackInfoList);
                trackListView.setAdapter(songsListAdapter);
                trackListView.setLayoutManager(new LinearLayoutManager(context));
            }
        } catch(Exception e) {
            Log.e(LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        }

        return view;
    }
}
package com.mediaplayer.Cicada.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.mediaplayer.Cicada.utilities.MediaLibraryManager;
import com.mediaplayer.Cicada.utilities.MediaPlayerConstants;

public class MediaManagerService extends IntentService {

    public MediaManagerService() {
        super("MediaManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("MediaManagerService", "Inside MediaManagerService");

        //Initialising/Updating Mediaplayer library
        boolean isChanged = MediaLibraryManager.init(this);

        //Sending broadcast indicating that MediaManagerService has finished initiliasing/updating the library
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MediaPlayerConstants.ACTION_INIT_COMPLETE);
        broadcastIntent.putExtra(MediaPlayerConstants.FLAG_LIBRARY_CHANGED, isChanged);
        sendBroadcast(broadcastIntent);
    }
}
package com.mediaplayer.Cicada.dao;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mediaplayer.Cicada.R;
import com.mediaplayer.Cicada.adapters.SongsListAdapter;
import com.mediaplayer.Cicada.beans.Playlist;
import com.mediaplayer.Cicada.beans.Track;
import com.mediaplayer.Cicada.fragments.SongsFragment;
import com.mediaplayer.Cicada.utilities.MediaLibraryManager;
import com.mediaplayer.Cicada.utilities.MediaPlayerConstants;
import com.mediaplayer.Cicada.utilities.MessageConstants;
import com.mediaplayer.Cicada.utilities.SQLConstants;
import com.mediaplayer.Cicada.utilities.Utilities;

import java.util.ArrayList;
import java.util.Iterator;

public class MediaPlayerDAO {
    private static SQLiteDatabase db;
    private MediaPlayerDBHelper mDbHelper;
    private Context context;

    public MediaPlayerDAO(Context context) {
        this.context = context;
        mDbHelper = new MediaPlayerDBHelper(context);
        db = mDbHelper.getWritableDatabase();
    }

    //Closes SQLite database connection
    public void closeConnection() {
        if(mDbHelper != null) {
            mDbHelper.close();
        }

        if(db != null) {
            db.close();
        }
    }

    //Add to playlist
    public void addToPlaylists(ArrayList<Playlist> selectedPlaylists, Track selectedTrack) {
        SQLiteStatement insertStmt, updateStmt;
        String toastText;
        int trackID, playlistID, playlistSize, newPlaylistSize, playlistDuration, newPlaylistDuration, increment = SQLConstants.ONE;
        Playlist playlist;

        try {
            insertStmt = db.compileStatement(SQLConstants.SQL_INSERT_PLAYLIST_DETAIL);

            //Retrieving selected track from trackInfoList
            trackID = selectedTrack.getTrackID();

            Iterator<Playlist> selectedPlaylistsIterator = selectedPlaylists.iterator();
            while(selectedPlaylistsIterator.hasNext()) {
                //Fetching current values for the selected playlist
                playlist = selectedPlaylistsIterator.next();
                playlistID = playlist.getPlaylistID();
                playlistSize = playlist.getPlaylistSize();
                playlistDuration = playlist.getPlaylistDuration();
                newPlaylistSize = playlistSize + increment;
                newPlaylistDuration = playlistDuration + selectedTrack.getTrackDuration();

                //Making an entry in table 'Playlist_Detail' for the selected playlist
                insertStmt.clearBindings();
                insertStmt.bindLong(1, playlistID);
                insertStmt.bindLong(2, trackID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, insertStmt.toString());
                insertStmt.execute();

                //Updating table 'Playlists' for the selected playlist with new values of 'playlistSize' and 'playlistDuration'
                updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST);
                updateStmt.bindLong(1, newPlaylistSize);
                updateStmt.bindLong(2, newPlaylistDuration);
                updateStmt.bindString(3, Utilities.getCurrentDate());
                updateStmt.bindLong(4, playlistID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                updateStmt.execute();

                //Setting new values for 'playlistSize' and 'playlistDuration' for selected playlist
                playlist.setPlaylistSize(newPlaylistSize);
                playlist.setPlaylistDuration(newPlaylistDuration);
                MediaLibraryManager.getPlaylistInfoList().set(playlist.getPlaylistIndex(), playlist);

                //If selected playlist is default playlist 'Favourites' then updating fav_sw in table 'Tracks' and trackInfoList
                if(playlistID == SQLConstants.PLAYLIST_ID_FAVOURITES) {
                    //Updating fav_sw in table Tracks
                    updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_TRACK_FAV_SW);
                    updateStmt.bindLong(1, SQLConstants.FAV_SW_YES);
                    updateStmt.bindLong(2, trackID);
                    Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                    updateStmt.execute();

                    //Setting favSw in trackInfoList
                    selectedTrack.setFavSw(SQLConstants.FAV_SW_YES);
                    MediaLibraryManager.getTrackInfoList().set(selectedTrack.getTrackIndex(), selectedTrack);
                }
            }

            //Setting success toast message
            if(selectedPlaylists.contains(MediaLibraryManager.getPlaylistByIndex(SQLConstants.PLAYLIST_INDEX_FAVOURITES))) {
                toastText = MessageConstants.ADDED_TO_FAVOURITES;
            } else {
                toastText = MessageConstants.ADDED_TO_PLAYLISTS;
            }
        } catch(SQLiteConstraintException sqle) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, sqle.getMessage());
            Utilities.reportCrash(sqle);

            toastText = MessageConstants.ERROR_DUPLICATE_TRACK_FAVOURITES;
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        //Displaying toast message to user
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void removeFromPlaylist(Playlist selectedPlaylist, Track selectedTrack) {
        SQLiteStatement updateStmt;
        String toastText;
        int trackID, playlistID, playlistSize, newPlaylistSize, playlistDuration, newPlaylistDuration;

        try {
            //Fetching existing values for selected playlist
            trackID = selectedTrack.getTrackID();
            playlistID = selectedPlaylist.getPlaylistID();
            playlistSize = selectedPlaylist.getPlaylistSize();
            playlistDuration = selectedPlaylist.getPlaylistDuration();

            //Deleting record from table 'Playlist_Detail'
            String args[] = {String.valueOf(playlistID), String.valueOf(trackID)};
            db.execSQL(SQLConstants.SQL_DELETE_TRACK_FROM_PLAYLIST, args);
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_DELETE_TRACK_FROM_PLAYLIST);

            newPlaylistSize = playlistSize - 1;
            newPlaylistDuration = playlistDuration - selectedTrack.getTrackDuration();

            //Updating table 'Playlists' for the selected playlist with new values of 'playlistSize' and 'playlistDuration'
            updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST);
            updateStmt.bindLong(1, newPlaylistSize);
            updateStmt.bindLong(2, newPlaylistDuration);
            updateStmt.bindString(3, Utilities.getCurrentDate());
            updateStmt.bindLong(4, playlistID);
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
            updateStmt.execute();

            //Setting new values for 'playlistSize' and 'playlistDuration' for selected playlist
            selectedPlaylist.setPlaylistSize(newPlaylistSize);
            selectedPlaylist.setPlaylistDuration(newPlaylistDuration);
            MediaLibraryManager.getPlaylistInfoList().set(selectedPlaylist.getPlaylistIndex(), selectedPlaylist);

            //If selected playlist is default playlist 'Favourites' then updating fav_sw in table 'Tracks' and trackInfoList
            if(playlistID == SQLConstants.PLAYLIST_ID_FAVOURITES) {
                //Updating fav_sw to 0 in table 'Tracks'
                updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_TRACK_FAV_SW);
                updateStmt.bindLong(1, SQLConstants.FAV_SW_NO);
                updateStmt.bindLong(2, trackID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                updateStmt.execute();

                //Setting favSw in trackInfoList
                selectedTrack.setFavSw(SQLConstants.FAV_SW_NO);
                MediaLibraryManager.getTrackInfoList().set(selectedTrack.getTrackIndex(), selectedTrack);

                //Setting success toast message
                toastText = MessageConstants.REMOVED_FROM_FAVOURITES;
            } else {
                //Setting success toast message
                toastText = MessageConstants.REMOVED_FROM_PLAYLIST;
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        //Displaying toast message to user
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static int updateTrackIndices() {
        SQLiteStatement updateStmt = null;
        int tracksUpdated = 0;

        try {
            updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_TRACK_INDICES);
            Iterator<Track> trackListIterator = MediaLibraryManager.getTrackInfoList().iterator();

            //Updating the indices of all the tracks
            while(trackListIterator.hasNext()) {
                Track track = trackListIterator.next();

                updateStmt.bindLong(1, track.getTrackIndex());
                updateStmt.bindString(2, Utilities.getCurrentDate());
                updateStmt.bindLong(3, track.getTrackID());

                Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                updateStmt.execute();
                updateStmt.clearBindings();
                tracksUpdated++;
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        } finally {
            if(updateStmt != null) {
                updateStmt.close();
            }
        }

        return tracksUpdated;
    }

    public void createPlaylist(Playlist playlist) {
        SQLiteStatement insertStmt;
        int playlistID;
        String toastText;

        try {
            //Inserting new playlist into table 'Playlists'
            insertStmt = db.compileStatement(SQLConstants.SQL_INSERT_PLAYLIST);

            //Setting playlist_index to current max index + 1 initially
            insertStmt.bindLong(1, MediaLibraryManager.getPlaylistInfoListSize());
            insertStmt.bindString(2, playlist.getPlaylistName());
            insertStmt.bindLong(3, playlist.getPlaylistSize());
            insertStmt.bindLong(4, playlist.getPlaylistDuration());
            insertStmt.bindString(5, Utilities.getCurrentDate());
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, insertStmt.toString());
            playlistID = (int) insertStmt.executeInsert();

            //Setting playlistID of newly created playlist
            playlist.setPlaylistID(playlistID);

            //Adding new playlist to playlistInfoList
            MediaLibraryManager.addPlaylist(playlist);

            //Updating indices of all the playlists in table 'Playlists'
            updatePlaylistIndices();

            //Setting success toast message
            toastText = MessageConstants.PLAYLIST_CREATED;
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void renamePlaylist(Playlist selectedPlaylist) {
        SQLiteStatement updateStmt;
        String toastText;

        try {
            //Updating the title of the renamed playlist
            updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST_TITLE);
            updateStmt.bindString(1, selectedPlaylist.getPlaylistName());
            updateStmt.bindString(2, Utilities.getCurrentDate());
            updateStmt.bindLong(3, selectedPlaylist.getPlaylistID());
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
            updateStmt.execute();

            //Updating indices of all the playlists in table 'Playlists'
            updatePlaylistIndices();

            //Setting success toast message
            toastText = MessageConstants.PLAYLIST_RENAMED;
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        //Displaying toast message to user
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updatePlaylistIndices() {
        SQLiteStatement updateStmt;
        updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST_INDICES);
        Iterator<Playlist> playlistIterator = MediaLibraryManager.getPlaylistInfoList().iterator();

        while(playlistIterator.hasNext()) {
            Playlist playlist = playlistIterator.next();

            //Updating the indices of all the playlists
            updateStmt.bindLong(1, playlist.getPlaylistIndex());
            updateStmt.bindString(2, Utilities.getCurrentDate());
            updateStmt.bindLong(3, playlist.getPlaylistID());
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
            updateStmt.execute();
            updateStmt.clearBindings();
        }
    }

    public void deletePlaylist(Playlist playlist) {
        SQLiteStatement deleteStmt;
        String toastText;
        int playlistID = playlist.getPlaylistID();

        try {
            //Deleting playlist from table 'Playlist_Detail'
            deleteStmt = db.compileStatement(SQLConstants.SQL_DELETE_PLAYLIST_FROM_PLAYLIST_DETAIL);
            deleteStmt.bindLong(1, playlistID);
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, deleteStmt.toString());
            deleteStmt.executeUpdateDelete();

            //Deleting playlist from table 'Playlist'
            deleteStmt = db.compileStatement(SQLConstants.SQL_DELETE_FROM_PLAYLISTS);
            deleteStmt.bindLong(1, playlistID);
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, deleteStmt.toString());
            deleteStmt.executeUpdateDelete();

            //Removing playlist from playlistInfoList
            MediaLibraryManager.removePlaylist(playlist.getPlaylistIndex());

            //Updating playlist_index for all playlists
            updatePlaylistIndices();

            //Setting success toast message
            toastText = MessageConstants.PLAYLIST_DELETED;
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        //Displaying toast message to user
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void addTracks(ArrayList<Track> selectedTracks, Playlist selectedPlaylist) {
        SQLiteStatement insertStmt, updateStmt = null;
        String toastText;
        int trackID, trackDuration, trackIndex, playlistID, playlistIndex, playlistSize, playlistDuration;
        Track track;

        try {
            //Retrieving current values for selected playlist
            playlistID = selectedPlaylist.getPlaylistID();
            playlistIndex = selectedPlaylist.getPlaylistIndex();
            playlistSize = selectedPlaylist.getPlaylistSize();
            playlistDuration = selectedPlaylist.getPlaylistDuration();
            insertStmt = db.compileStatement(SQLConstants.SQL_INSERT_PLAYLIST_DETAIL);

            //Checking if selected playlist is default playlist 'Favourites'
            if(playlistID == SQLConstants.PLAYLIST_ID_FAVOURITES) {
                updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_TRACK_FAV_SW);
            }

            Iterator<Track> trackIterator = selectedTracks.iterator();
            while(trackIterator.hasNext()) {
                track = trackIterator.next();
                trackID = track.getTrackID();
                trackIndex = track.getTrackIndex();
                trackDuration = track.getTrackDuration();

                //Inserting in table 'Playlist_Detail'
                insertStmt.clearBindings();
                insertStmt.bindLong(1, playlistID);
                insertStmt.bindLong(2, trackID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, insertStmt.toString());
                insertStmt.executeInsert();

                //If selected playlist is 'Favourites', updating 'fav_sw' in table 'Tracks'
                if(playlistID == SQLConstants.PLAYLIST_ID_FAVOURITES) {
                    updateStmt.clearBindings();
                    updateStmt.bindLong(1, SQLConstants.FAV_SW_YES);
                    updateStmt.bindLong(2, trackID);
                    Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                    updateStmt.executeUpdateDelete();

                    //Updating trackInfoList with new value if favSw
                    track.setFavSw(SQLConstants.FAV_SW_YES);
                    MediaLibraryManager.updateTrackInfoList(trackIndex, track);
                }

                //Calculating new values for selected playlist
                playlistSize++;
                playlistDuration = playlistDuration + trackDuration;
            }

            //Updating selected playlist with new values of playlist_size and playlist_duration
            updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST);
            updateStmt.bindLong(1, playlistSize);
            updateStmt.bindLong(2, playlistDuration);
            updateStmt.bindString(3, Utilities.getCurrentDate());
            updateStmt.bindLong(4, playlistID);
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
            updateStmt.executeUpdateDelete();

            //Updating playlistInfoList with new values of playlistSize and playlistDuration
            selectedPlaylist.setPlaylistSize(playlistSize);
            selectedPlaylist.setPlaylistDuration(playlistDuration);
            MediaLibraryManager.updatePlaylistInfoList(playlistIndex, selectedPlaylist);

            //Set success toast message
            toastText = MessageConstants.ADDED_TRACKS;
        } catch(SQLiteConstraintException sqle) {
            Log.e(MediaPlayerConstants.LOG_TAG_SQL, sqle.getMessage());
            Utilities.reportCrash(sqle);

            toastText = MessageConstants.ERROR_DUPLICATE_TRACK_FAVOURITES;
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_SQL, e.getMessage());
            Utilities.reportCrash(e);

            //Setting error toast message
            toastText = MessageConstants.ERROR;
        }

        //Displaying toast message to user
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void addTracksToLibrary(@NonNull ArrayList<Track> trackList) {
        Track track;
        int c;
        long tracksAdded = 0;

        SQLiteStatement insertStmt = db.compileStatement(SQLConstants.SQL_INSERT_TRACK);
        Iterator<Track> trackIterator = trackList.iterator();

        //Inserting tracks in table 'Tracks'
        while(trackIterator.hasNext()) {
            track = trackIterator.next();
            c = SQLConstants.ONE;

            insertStmt.bindString(c++, track.getTrackTitle());
            insertStmt.bindLong(c++, track.getTrackIndex());
            insertStmt.bindString(c++, track.getFileName());
            insertStmt.bindLong(c++, track.getTrackDuration());
            insertStmt.bindLong(c++, track.getFileSize());
            insertStmt.bindString(c++, track.getAlbumName());
            insertStmt.bindString(c++, track.getArtistName());
            insertStmt.bindBlob(c++, track.getAlbumArt());
            insertStmt.bindString(c++, track.getTrackLocation());
            insertStmt.bindLong(c++, track.isFavSw());
            insertStmt.bindString(c, Utilities.getCurrentDate());

            Log.d(MediaPlayerConstants.LOG_TAG_SQL, insertStmt.toString());

            try {
                insertStmt.executeInsert();
                insertStmt.clearBindings();
                ++tracksAdded;
            } catch(SQLException sqle) {
                Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, sqle.getMessage());
                Utilities.reportCrash(sqle);
            }
        }

        Log.d("Tracks added to library", String.valueOf(tracksAdded));
    }

    public void deleteTracksFromLibrary(@NonNull ArrayList<String> deletedTracksList) {
        int tracksDeleted;
        Iterator<String> deletedTracksIterator = deletedTracksList.iterator();
        StringBuilder fileNames = new StringBuilder();

        while(deletedTracksIterator.hasNext()) {
            fileNames.append(SQLConstants.DOUBLE_QUOTE).append(deletedTracksIterator.next()).append(SQLConstants.DOUBLE_QUOTE);

            if(deletedTracksIterator.hasNext()) {
                fileNames.append(SQLConstants.COMMA_SEP);
            }
        }

        SQLiteStatement deleteStmt = db.compileStatement(SQLConstants.SQL_DELETE_TRACK_FOR_FILENAME + fileNames + ")");
        tracksDeleted = deleteStmt.executeUpdateDelete();

        Log.d("Tracks deleted", String.valueOf(tracksDeleted));
    }

    public ArrayList<Track> getTracksForPlaylist(int playlistID) {
        ArrayList<Track> trackList = new ArrayList<Track>();
        String args[] = {String.valueOf(playlistID)};
        Cursor playlistsCursor = null;

        try {
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_ALL_TRACKS_FOR_PLAYLIST);
            playlistsCursor = db.rawQuery(SQLConstants.SQL_SELECT_ALL_TRACKS_FOR_PLAYLIST, args);
            playlistsCursor.moveToFirst();
            int c;

            while(!playlistsCursor.isAfterLast()) {
                Track track = new Track();
                c = SQLConstants.ZERO;

                track.setTrackID(playlistsCursor.getInt(c++));
                track.setTrackTitle(playlistsCursor.getString(c++));
                track.setTrackIndex(playlistsCursor.getInt(c++));
                track.setFileName(playlistsCursor.getString(c++));
                track.setTrackDuration(playlistsCursor.getInt(c++));
                track.setFileSize(playlistsCursor.getInt(c++));
                track.setAlbumName(playlistsCursor.getString(c++));
                track.setArtistName(playlistsCursor.getString(c++));
                track.setAlbumArt(playlistsCursor.getBlob(c++));
                track.setTrackLocation(playlistsCursor.getString(c++));
                track.setFavSw(playlistsCursor.getInt(c));

                trackList.add(track);
                playlistsCursor.moveToNext();
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        } finally {
            if(playlistsCursor != null) {
                playlistsCursor.close();
            }
        }

        return trackList;
    }

    public ArrayList<Integer> getTrackIDsForPlaylist(int playlistID) {
        ArrayList<Integer> trackList = null;
        Cursor cursor = null;
        String args[] = {String.valueOf(playlistID)};

        try {
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_PLAYLISTS_FOR_TRACK);
            cursor = db.rawQuery(SQLConstants.SQL_SELECT_TRACK_IDS_FOR_PLAYLIST, args);

            if(cursor != null && cursor.getCount() > 0) {
                trackList = new ArrayList<Integer>();
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    trackList.add(cursor.getInt(SQLConstants.ZERO));
                    cursor.moveToNext();
                }
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return trackList;
    }

    /**
     * Method to get the list of playlists from database
     * @return Sorted list of Playlists
     */
    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlistInfoList = new ArrayList<Playlist>();

        Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_PLAYLISTS);
        Cursor playlistsCursor = db.rawQuery(SQLConstants.SQL_SELECT_PLAYLISTS, null);
        playlistsCursor.moveToFirst();

        while(!playlistsCursor.isAfterLast()) {
            Playlist playlist = new Playlist();
            playlist.setPlaylistID(playlistsCursor.getInt(0));
            playlist.setPlaylistIndex(playlistsCursor.getInt(1));
            playlist.setPlaylistName(playlistsCursor.getString(2));
            playlist.setPlaylistSize(playlistsCursor.getInt(3));
            playlist.setPlaylistDuration(playlistsCursor.getInt(4));

            playlistInfoList.add(playlist);
            playlistsCursor.moveToNext();
        }

        playlistsCursor.close();
        return playlistInfoList;
    }

    //Instantiates a new track list and populates it from table 'Tracks'
    public ArrayList<Track> getTracks() {
        ArrayList<Track> trackInfoList = null;
        Track track;
        int c;

        Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_TRACKS);
        Cursor tracksCursor = db.rawQuery(SQLConstants.SQL_SELECT_TRACKS, null);

        if(tracksCursor.getCount() > 0) {
            tracksCursor.moveToFirst();
            trackInfoList = new ArrayList<Track>();

            while(!tracksCursor.isAfterLast()) {
                track = new Track();
                c = SQLConstants.ZERO;

                track.setTrackID(tracksCursor.getInt(c++));
                track.setTrackTitle(tracksCursor.getString(c++));
                track.setTrackIndex(tracksCursor.getInt(c++));
                track.setFileName(tracksCursor.getString(c++));
                track.setTrackDuration(tracksCursor.getInt(c++));
                track.setFileSize(tracksCursor.getInt(c++));
                track.setAlbumName(tracksCursor.getString(c++));
                track.setArtistName(tracksCursor.getString(c++));
                track.setAlbumArt(tracksCursor.getBlob(c++));
                track.setTrackLocation(tracksCursor.getString(c++));
                track.setFavSw(tracksCursor.getInt(c));

                trackInfoList.add(track);
                tracksCursor.moveToNext();
            }
        }

        tracksCursor.close();
        return trackInfoList;
    }

    public ArrayList<Integer> getPlaylistsForTrack(int trackID) {
        ArrayList<Integer> playlist = null;
        Cursor cursor = null;

        try {
            String args[] = {String.valueOf(trackID)};

            //Fetching existing values for selected playlist
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_PLAYLISTS_FOR_TRACK);
            cursor = db.rawQuery(SQLConstants.SQL_SELECT_PLAYLISTS_FOR_TRACK, args);

            if(cursor != null && cursor.getCount() > 0) {
                playlist = new ArrayList<Integer>();
                cursor.moveToFirst();

                while(!cursor.isAfterLast()) {
                    playlist.add(cursor.getInt(SQLConstants.ZERO));
                    cursor.moveToNext();
                }
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return playlist;
    }

    public ArrayList<String> getFileNamesFromLibrary() {
        ArrayList<String> fileNamesList = null;
        Cursor tracksCursor =  null;
        int fileNamesListSize = 0;

        try {
            Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_FILE_NAMES);
            tracksCursor = db.rawQuery(SQLConstants.SQL_SELECT_FILE_NAMES, null);

            if(tracksCursor != null && tracksCursor.getCount() > 0) {
                fileNamesList = new ArrayList<String>();
                tracksCursor.moveToFirst();

                while(!tracksCursor.isAfterLast()) {
                    fileNamesList.add(tracksCursor.getString(SQLConstants.ZERO));
                    tracksCursor.moveToNext();
                }

                fileNamesListSize = fileNamesList.size();
            }
        } catch(Exception e) {
            Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
            Utilities.reportCrash(e);
        } finally {
            if(tracksCursor != null) {
                tracksCursor.close();
            }
        }

        Log.d("File names fetched", String.valueOf(fileNamesListSize));
        return fileNamesList;
    }

    public static class UpdateTracksTask extends AsyncTask<Track, Void, Integer> {
        private Activity activity;

        public UpdateTracksTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Integer doInBackground(Track... selectedTracks) {
            SQLiteStatement updateStmt, deleteStmt;
            String toastText;
            int trackID, playlistID, playlistSize, newPlaylistSize, playlistDuration, newPlaylistDuration, trackDuration, tracksUpdated = 0;
            Playlist playlist;
            Cursor cursor = null;
            Track selectedTrack = selectedTracks[0];

            try {
                trackID = selectedTrack.getTrackID();
                trackDuration = selectedTrack.getTrackDuration();
                String args[] = {String.valueOf(trackID)};
                String currentDate = Utilities.getCurrentDate();

                //Fetching existing values for selected playlist
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, SQLConstants.SQL_SELECT_PLAYLIST_INDICES_FOR_TRACK);
                cursor = db.rawQuery(SQLConstants.SQL_SELECT_PLAYLIST_INDICES_FOR_TRACK, args);
                updateStmt = db.compileStatement(SQLConstants.SQL_UPDATE_PLAYLIST);
                cursor.moveToFirst();

                while(!cursor.isAfterLast()) {
                    playlist = MediaLibraryManager.getPlaylistByIndex(cursor.getInt(SQLConstants.ZERO));
                    playlistID = playlist.getPlaylistID();
                    playlistSize = playlist.getPlaylistSize();
                    playlistDuration = playlist.getPlaylistDuration();

                    //Calculating new values for selected playlist
                    newPlaylistSize = playlistSize - SQLConstants.ONE;
                    newPlaylistDuration = playlistDuration - trackDuration;

                    //Updating table 'Playlists' for the selected playlist with new values of 'playlistSize' and 'playlistDuration'
                    updateStmt.clearBindings();
                    updateStmt.bindLong(1, newPlaylistSize);
                    updateStmt.bindLong(2, newPlaylistDuration);
                    updateStmt.bindString(3, currentDate);
                    updateStmt.bindLong(4, playlistID);
                    Log.d(MediaPlayerConstants.LOG_TAG_SQL, updateStmt.toString());
                    updateStmt.execute();

                    //Setting new values for 'playlistSize' and 'playlistDuration' for selected playlist
                    playlist.setPlaylistSize(newPlaylistSize);
                    playlist.setPlaylistDuration(newPlaylistDuration);
                    MediaLibraryManager.getPlaylistInfoList().set(playlist.getPlaylistIndex(), playlist);

                    cursor.moveToNext();
                }

                //Deleting track from table 'Playlist_Detail'
                deleteStmt = db.compileStatement(SQLConstants.SQL_DELETE_TRACK_FROM_PLAYLIST_DETAIL);
                deleteStmt.bindLong(1, trackID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, deleteStmt.toString());
                deleteStmt.execute();

                //Deleting track from table 'Tracks'
                deleteStmt = db.compileStatement(SQLConstants.SQL_DELETE_FROM_TRACKS);
                deleteStmt.bindLong(1, trackID);
                Log.d(MediaPlayerConstants.LOG_TAG_SQL, deleteStmt.toString());
                deleteStmt.execute();

                //Removing selected track from the list of tracks
                MediaLibraryManager.removeTrack(MediaPlayerConstants.TAG_PLAYLIST_LIBRARY, selectedTrack.getTrackIndex());

                //Sorting the list of tracks to update track indices
                MediaLibraryManager.sortTracklist(MediaPlayerConstants.TAG_PLAYLIST_LIBRARY);

                //Updating track indices in db
                tracksUpdated = updateTrackIndices();

                //Setting success toast message
                toastText = MessageConstants.REMOVED_FROM_LIBRARY;
            } catch(Exception e) {
                Log.e(MediaPlayerConstants.LOG_TAG_EXCEPTION, e.getMessage());
                Utilities.reportCrash(e);

                //Setting error toast message
                toastText = MessageConstants.ERROR;
            } finally {
                if(cursor != null) {
                    cursor.close();
                }
            }

            return tracksUpdated;
        }

        protected void onPostExecute(Integer result) {
            //Updating songs list view adapter
            updateSongsListAdapter();

            Toast toast = Toast.makeText(activity, "Tracks updated", Toast.LENGTH_SHORT);
            toast.show();
        }

        private void updateSongsListAdapter() {
            ArrayList<Track> trackList = MediaLibraryManager.getTrackInfoList();

            if(trackList.isEmpty()) {
                TextView emptyLibraryMessage = (TextView) activity.findViewById(R.id.emptyLibraryMessage);
                emptyLibraryMessage.setVisibility(View.VISIBLE);
            }

            SongsListAdapter adapter = new SongsListAdapter(activity, trackList);
            RecyclerView recyclerView = SongsFragment.trackListView;
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }
}
package uk.wjdp.mp3player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import uk.wjdp.mp3player.SongList.Song;

public class MainActivity extends AppCompatActivity {

    ListView listView_songs;
    TextView textView_status;
    Button button_play;
    Button button_pause;
    Button button_next;
    Button button_stop;

    final String TAG = "mp3player-MainActivity";

    private PlayerService.PlayerBinder myPlayerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");



        // Store ref to UI components
        textView_status = (TextView)findViewById(R.id.status_text);
        listView_songs = (ListView)findViewById(R.id.song_list);
        button_play = (Button)findViewById(R.id.play_button);
        button_pause = (Button)findViewById(R.id.pause_button);
        button_next = (Button)findViewById(R.id.next_button);
        button_stop = (Button)findViewById(R.id.stop_button);

        // Create a thread to fetch the song list
        // We do this in a thread to prevent blocking the UI during the app's creation
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get phone's media, making this final ensures it's available to the next Runnable
                final SongList songList = getMedia();
                Log.d(TAG, "Songs: " + songList.song_list.size());

                // We now need to update the UI, which we cannot do inside a thread as the Android
                // UI is not thread-safe. So we post to the message queue of the UI thread.
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "adding songs to listView from posted Runnable");
                        // Create an adapter from the song list
                        final ArrayAdapter<Song> songItemsAdapter = new ArrayAdapter<Song>(MainActivity.this,
                                android.R.layout.simple_list_item_1, songList.song_list);

                        // Pass that adapter to the list view, the list will update with the contents of the adapter
                        listView_songs.setAdapter(songItemsAdapter);

                        // Set a click listener for the list view
                        listView_songs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                // User 'clicks' a song and an activity method is fired with that song
                                // As the listview has the full song objects we don't have to do any lookup here
                                Song song = (Song) ((ListView) parent).getItemAtPosition(position);
                                Log.d(TAG, "Song selected - " + song.title);
                                songSelected(song);
                            }
                        });
                    }
                });

            }
        }).start();

    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        Intent intent= new Intent(this, PlayerService.class);
        // Have to start using startService so service isn't killed when this activity unbinds from
        // it onStop
        startService(intent);
        bindService(intent, playerServiceConnection, 0);
        // Register a receiver to the service's callbacks
        registerReceiver(receiver, new IntentFilter(PlayerService.NOTIFICATION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Unbind so activity can sleep
        unbindService(playerServiceConnection);
    }

    private ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            // Store a ref to the service
            myPlayerService = (PlayerService.PlayerBinder) service;
            // Request the service sends us the current state so UI can be updated
            myPlayerService.update_state();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            myPlayerService = null;
        }
    };

    // Broadcast receiving

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Grab extra vars attached to the broadcast
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Extract vars
                String callback = bundle.getString(PlayerService.CALLBACK);
                String artist = bundle.getString(PlayerService.SONG_ARTIST);
                String title = bundle.getString(PlayerService.SONG_TITLE);
                int queue = bundle.getInt(PlayerService.QUEUE);

                // Perform some UI work depedending on callback
                Boolean nextState = queue > 1;
                switch (callback) {
                    case PlayerService.PLAY:
                        textView_status.setText("► " + artist + " - " + title + " (" + queue + ")");
                        setButtonStates(false, true, nextState, true);
                        break;
                    case PlayerService.PAUSE:
                        textView_status.setText("▋▋ " + artist + " - " + title + " (" + queue + ")");
                        setButtonStates(true, false, nextState, true);
                        break;
                    case PlayerService.STOP:
                        textView_status.setText("No Song Playing");
                        setButtonStates(false, false, false, false);
                        break;
                }
            }
        }
    };

    // Media functions

    protected SongList getMedia() {
        // Get a list of songs from Android's external storage via a content provider
        SongList songList = new SongList();

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        Log.d(TAG, "Scanning thru media");

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Grab column indexes for the fields we're interested in
            int idColumn     = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn  = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int pathColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            // Iterate over the cursor
            do {
                // Transpose data retrieved from the cursor into a songList
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                String songPath = musicCursor.getString(pathColumn);
                Log.d(TAG, songTitle);
                songList.song_list.add(new Song(songId, songTitle, songArtist, songPath));
            } while(musicCursor.moveToNext());
        }

        musicCursor.close();
        return songList;
    }

    // State changes

    void songSelected(Song song) {
        myPlayerService.setup(song);
    }

    void setButtonStates(Boolean play_state, Boolean pause_state, Boolean next_state, Boolean stop_state) {
        // Utitlity function to change all three of the button's states
        button_play.setEnabled(play_state);
        button_pause.setEnabled(pause_state);
        button_next.setEnabled(next_state);
        button_stop.setEnabled(stop_state);
    }

    // Button event handlers

    public void buttonPlay(View v) {
        myPlayerService.play();
    }
    public void buttonPause(View v) {
        myPlayerService.pause();
    }
    public void buttonNext(View v) {
        myPlayerService.next();
    }
    public void buttonStop(View v) {
        myPlayerService.stop();
    }

}

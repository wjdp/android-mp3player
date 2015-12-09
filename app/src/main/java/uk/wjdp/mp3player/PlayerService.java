package uk.wjdp.mp3player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

import uk.wjdp.mp3player.SongList.Song;

public class PlayerService extends Service {
    SongList songList = new SongList();
    Song song;
    MediaPlayer playerMediaPlayer;

    final String TAG = "PlayerService";

    // Intent keys
    public static final String CALLBACK = "CALLBACK";
    public static final String SONG_TITLE = "SONG_TITLE";
    public static final String SONG_ARTIST = "SONG_ARTIST";
    public static final String QUEUE = "QUEUE";

    public static final String PLAY = "PLAY";
    public static final String PAUSE = "PAUSE";
    public static final String STOP = "STOP";

    protected String state = STOP;

    // Notification keys
    public static final String NOTIFICATION = "uk.wjdp.mp3player.playerservice.receiver";

    public PlayerService() {
    }

    protected void sendNotification(String notification) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(CALLBACK, notification);
        if (song != null) {
            intent.putExtra(SONG_ARTIST, song.artist);
            intent.putExtra(SONG_TITLE, song.title);
            intent.putExtra(QUEUE, songList.song_list.size() + 1);
        }
        sendBroadcast(intent);
    }

    private final Binder binder = new PlayerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class PlayerBinder extends Binder {
        void setup(Song newSong) {
            songList.addSong(newSong);
            if (playerMediaPlayer == null) {
                // Starting cold, lets get this moving
                setForeground();
                // Play the song
                next();
            }
            else {
                // Just adding a song to the list, send the active state to update the UI
                sendNotification(state);
            }
        }

        void play() {
            // Check if we can play
            if (playerMediaPlayer != null) {
                // Start
                playerMediaPlayer.start();
                // Send callback to UI
                state = PLAY;
                sendNotification(state);
            }
        }

        void pause() {
            if (playerMediaPlayer != null) {
                playerMediaPlayer.pause();
                state = PAUSE;
                sendNotification(state);
            }
        }

        void stop() {
            if (playerMediaPlayer != null) {
                // Stop and get rid of the playerMediaPlayer
                playerMediaPlayer.stop();
                playerMediaPlayer = null;
            }

            // Set our state and notify the UI
            state = STOP;
            sendNotification(state);

            // Empty the playlist
            songList.emptyList();

            // Set service to background so it can die
            setBackground();
        }

        void next() {
            // Play next
            if (songList.song_list.size() == 0) {
                // Playlist empty, we should stop
                stop();
            }
            else {
                // Create a new media player
                if (playerMediaPlayer != null) {
                    // Get rid of the old one if we already have one
                    playerMediaPlayer.stop();
                    // Release the mp, otherwise we'll leak a lot of memory
                    playerMediaPlayer.release();
                    playerMediaPlayer = null;
                }

                playerMediaPlayer = new MediaPlayer();
                playerMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                // Grab the song to be played and set our instance ref
                song = songList.popSong();

                try {
                    // Select and load media file
                    playerMediaPlayer.setDataSource(song.path);
                    playerMediaPlayer.prepare();
                    // Run our play method
                    this.play();

                } catch (IOException e) {
                    // If the file doesn't exist, post an error to the log
                    Log.e(TAG, "File not found!");
                }

                // Set callbacks
                playerMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (songList.song_list.size() > 0) {
                            // More songs to play, play the next
                            next();
                        }
                        else {
                            // No more songs, stop without calling stop on the MediaPlayer
                            playerMediaPlayer.release();
                            playerMediaPlayer = null; // Prevent stop() from calling .stop() on mp
                            stop();
                        }
                    }
                });

                state = PLAY;
            }
        }

        void update_state() {
            sendNotification(state);
        }
    }

    void setForeground() {
        // Build a notification to use with startForeground to keep this service running
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this).setContentTitle("mp3player")
                .setContentText("Music is playing")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    void setBackground() {
        // Return the service to the background so it can die
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        // Double check the player has been released
        if (playerMediaPlayer != null) playerMediaPlayer.release();
    }

}

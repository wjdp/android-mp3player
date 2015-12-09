package uk.wjdp.mp3player;

import java.util.ArrayList;

public class SongList {
    // Class for managing a list of Songs

    public ArrayList<Song> song_list = new ArrayList<Song>();

    public String[] getSongTitles() {
        int num_of_songs = song_list.size();
        String[] songTitles = new String[num_of_songs];

        for (int i = 0; i < num_of_songs; i++) {
            songTitles[i] = song_list.get(i).title;
        }

        return songTitles;
    }

    public void addSong(Song song) {
        song_list.add(song);
    }

    public Song popSong() {
        // Return the Song at the top of the list and remove it (pop)
        Song next = song_list.get(0);
        song_list.remove(0);
        return next;
    }

    public void emptyList() {
        song_list.clear();
    }

    public static class Song {
        // A single song
        long id;
        String title;
        String artist;
        String path;

        public Song(long id, String title, String artist, String path) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.path = path;
        }

        public String toString() {
            return this.artist + " - "  + this.title;
        }
    }

}

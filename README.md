# G54MDP Coursework

Will Pimblett, G405, 4264291

## Music Player

The implemented application is an music player. On startup the application uses the external content provider to query for media files on the device. These are presented in a scrollable list view.

Songs are shown in the format "ARTIST - TITLE". If a song is missing the correct tags "<unknown> - FILENAME" will be shown.

Pressing an item will start playback, whereas pressing another will queue it. The number of items in the playback queue are displayed in brackets beside the currently playing item at the bottom of the screen.

Playback can be paused and resumed using the buttons at the bottom of the screen. The next button will skip to the next song in the queue. The stop button will abort playback and empty the queue.

While playback is engaged (i.e. not stopped) a background service to facilitate this will run. This is indicated by a persistent notification.

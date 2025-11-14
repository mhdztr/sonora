package com.musicplayer.service;

import com.musicplayer.model.Track;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Service untuk audio playback menggunakan VLCJ
 * Menangani streaming dari YouTube Music dan kontrol playback
 */
public class AudioPlayerService {

    private AudioPlayerComponent audioPlayer;
    private YouTubeMusicService youtubeService;

    private Track currentTrack;
    private List<Track> queue;
    private int currentIndex;
    private boolean isPlaying;
    private boolean isRepeat;
    private boolean isShuffle;

    private List<PlayerStateListener> listeners;

    public AudioPlayerService() {
        this.audioPlayer = new AudioPlayerComponent();
        this.youtubeService = new YouTubeMusicService();
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
        this.isPlaying = false;
        this.listeners = new ArrayList<>();

        setupPlayerListeners();
    }

    /**
     * Setup event listeners untuk player
     */
    private void setupPlayerListeners() {
        audioPlayer.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                isPlaying = true;
                notifyStateChanged();
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                isPlaying = false;
                notifyStateChanged();
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                isPlaying = false;
                notifyStateChanged();
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                // Auto play next track
                playNext();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.err.println("Player error occurred");
                // Try to refresh URL and replay
                if (currentTrack != null) {
                    System.out.println("Attempting to refresh stream URL...");
                    new Thread(() -> {
                        try {
                            String newUrl = youtubeService.getStreamUrl(currentTrack.getYoutubeId());
                            if (newUrl != null) {
                                audioPlayer.mediaPlayer().media().play(newUrl);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                notifyTimeChanged(newTime);
            }
        });
    }

    /**
     * Play track dari YouTube Music dengan error handling
     */
    public void playTrack(Track track) {
        if (track == null || track.getYoutubeId() == null) {
            System.err.println("Invalid track or missing YouTube ID");
            notifyError("Invalid track");
            return;
        }

        // Stop current playback
        if (isPlaying) {
            audioPlayer.mediaPlayer().controls().stop();
        }

        currentTrack = track;
        notifyTrackChanged(track);

        // Get streaming URL in background
        new Thread(() -> {
            try {
                System.out.println("🔄 Fetching stream for: " + track.getArtist() + " - " + track.getTitle());

                String streamUrl = youtubeService.getStreamUrl(track.getYoutubeId());

                if (streamUrl != null && !streamUrl.isEmpty()) {
                    // Play the stream
                    boolean success = audioPlayer.mediaPlayer().media().play(streamUrl);

                    if (success) {
                        System.out.println("✅ Playing: " + track.getArtist() + " - " + track.getTitle());
                    } else {
                        System.err.println("❌ Failed to start playback");
                        notifyError("Failed to start playback");
                    }
                } else {
                    System.err.println("❌ Could not get stream URL for: " + track.getTitle());
                    notifyError("Could not get stream URL. Try another song.");

                    // Auto skip to next if in queue
                    if (queue.size() > 1) {
                        System.out.println("⏭️ Auto-skipping to next track...");
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                Thread.sleep(1000);
                                playNext();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error playing track: " + e.getMessage());
                e.printStackTrace();
                notifyError("Error: " + e.getMessage());

                // Auto skip to next
                if (queue.size() > 1) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(1000);
                            playNext();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Play/Resume
     */
    public void play() {
        if (currentTrack == null && !queue.isEmpty()) {
            playTrack(queue.get(0));
        } else {
            audioPlayer.mediaPlayer().controls().play();
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        audioPlayer.mediaPlayer().controls().pause();
    }

    /**
     * Stop playback
     */
    public void stop() {
        audioPlayer.mediaPlayer().controls().stop();
        currentTrack = null;
        notifyTrackChanged(null);
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        if (isPlaying) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Play next track in queue
     */
    public void playNext() {
        if (queue.isEmpty()) {
            return;
        }

        if (isShuffle) {
            // Random next track
            int randomIndex = (int) (Math.random() * queue.size());
            currentIndex = randomIndex;
        } else {
            currentIndex++;
            if (currentIndex >= queue.size()) {
                if (isRepeat) {
                    currentIndex = 0;
                } else {
                    stop();
                    return;
                }
            }
        }

        playTrack(queue.get(currentIndex));
    }

    /**
     * Play previous track in queue
     */
    public void playPrevious() {
        if (queue.isEmpty()) {
            return;
        }

        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = queue.size() - 1;
        }

        playTrack(queue.get(currentIndex));
    }

    /**
     * Seek to position (in seconds)
     */
    public void seekTo(long seconds) {
        audioPlayer.mediaPlayer().controls().setTime(seconds * 1000);
    }

    /**
     * Set volume (0-100)
     */
    public void setVolume(int volume) {
        int normalizedVolume = Math.max(0, Math.min(100, volume));
        audioPlayer.mediaPlayer().audio().setVolume(normalizedVolume);
    }

    /**
     * Get current volume
     */
    public int getVolume() {
        return audioPlayer.mediaPlayer().audio().volume();
    }

    /**
     * Get current playback time (in seconds)
     */
    public long getCurrentTime() {
        return audioPlayer.mediaPlayer().status().time() / 1000;
    }

    /**
     * Get track duration (in seconds)
     */
    public long getDuration() {
        return audioPlayer.mediaPlayer().status().length() / 1000;
    }

    /**
     * Check if playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Get current track
     */
    public Track getCurrentTrack() {
        return currentTrack;
    }

    /**
     * Set queue and play first track
     */
    public void setQueueAndPlay(List<Track> tracks, int startIndex) {
        this.queue = new ArrayList<>(tracks);
        this.currentIndex = startIndex;
        if (!queue.isEmpty() && startIndex >= 0 && startIndex < queue.size()) {
            playTrack(queue.get(startIndex));
        }
    }

    /**
     * Add track to queue
     */
    public void addToQueue(Track track) {
        queue.add(track);
        if (queue.size() == 1) {
            currentIndex = 0;
            playTrack(track);
        }
    }

    /**
     * Clear queue
     */
    public void clearQueue() {
        queue.clear();
        currentIndex = -1;
    }

    /**
     * Get current queue
     */
    public List<Track> getQueue() {
        return new ArrayList<>(queue);
    }

    /**
     * Toggle repeat mode
     */
    public void toggleRepeat() {
        isRepeat = !isRepeat;
    }

    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        isShuffle = !isShuffle;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    /**
     * Add state listener
     */
    public void addPlayerStateListener(PlayerStateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove state listener
     */
    public void removePlayerStateListener(PlayerStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners of state change
     */
    private void notifyStateChanged() {
        for (PlayerStateListener listener : listeners) {
            listener.onPlayerStateChanged(isPlaying);
        }
    }

    /**
     * Notify listeners of track change
     */
    private void notifyTrackChanged(Track track) {
        for (PlayerStateListener listener : listeners) {
            listener.onTrackChanged(track);
        }
    }

    /**
     * Notify listeners of time change
     */
    private void notifyTimeChanged(long timeMs) {
        for (PlayerStateListener listener : listeners) {
            listener.onTimeChanged(timeMs / 1000);
        }
    }

    /**
     * Notify listeners of error
     */
    private void notifyError(String message) {
        for (PlayerStateListener listener : listeners) {
            listener.onError(message);
        }
    }

    /**
     * Release resources
     */
    public void release() {
        if (audioPlayer != null) {
            audioPlayer.release();
        }
    }

    /**
     * Player state listener interface
     */
    public interface PlayerStateListener {
        void onPlayerStateChanged(boolean isPlaying);
        void onTrackChanged(Track track);
        void onTimeChanged(long seconds);

        default void onError(String message) {
            // Optional error handling
        }
    }
}

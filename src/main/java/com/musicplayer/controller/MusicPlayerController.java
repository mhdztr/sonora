package com.musicplayer.controller;

import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;
import com.musicplayer.service.*;
import com.musicplayer.view.NowPlayingPanel;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Updated Controller dengan Metadata Enrichment Service
 */
public class MusicPlayerController {
    private DatabaseManager dbManager;
    private YouTubeMusicService youtubeService;
    private AudioFingerprintService fingerprintService;
    private AudioRecordingService recordingService;
    private RecommendationService recommendationService;
    private AudioPlayerService audioPlayerService;
    private MetadataEnrichmentService enrichmentService; // NEW!

    private NowPlayingPanel nowPlayingPanel;
    private File tempRecordingFile;

    public MusicPlayerController() {
        this.dbManager = DatabaseManager.getInstance();
        this.youtubeService = new YouTubeMusicService();
        this.fingerprintService = new AudioFingerprintService();
        this.recordingService = new AudioRecordingService();
        this.recommendationService = new RecommendationService();
        this.audioPlayerService = new AudioPlayerService();
        this.enrichmentService = new MetadataEnrichmentService(); // NEW!

        setupPlayerListeners();
    }

    public void initialize() {
        // Initialization logic
    }

    /**
     * Setup listeners untuk player events
     */
    private void setupPlayerListeners() {
        audioPlayerService.addPlayerStateListener(new AudioPlayerService.PlayerStateListener() {
            @Override
            public void onPlayerStateChanged(boolean isPlaying) {
                // Player state changed (playing/paused)
            }

            @Override
            public void onTrackChanged(Track track) {
                // Update Now Playing panel
                if (nowPlayingPanel != null) {
                    nowPlayingPanel.updateNowPlaying(track);
                }

                // Record play history
                if (track != null) {
                    try {
                        dbManager.recordPlay(track.getId());
                        System.out.println("📊 Play recorded for: " + track.getTitle());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onTimeChanged(long seconds) {
                // Time update (handled by UI timer)
            }

            @Override
            public void onError(String message) {
                System.err.println("❌ Player error: " + message);
            }
        });
    }

    public void setNowPlayingPanel(NowPlayingPanel panel) {
        this.nowPlayingPanel = panel;
    }

    // ========== YouTube Music Operations ==========

    /**
     * Search YouTube Music - UPDATED with metadata enrichment
     */
    public List<Track> searchYouTubeMusic(String query) {
        try {
            System.out.println("🔍 Searching YouTube Music: " + query);

            // 1. Search via InnerTube (fast, gets basic info)
            List<Track> tracks = youtubeService.searchTracks(query);

            if (tracks.isEmpty()) {
                System.out.println("❌ No results found");
                return tracks;
            }

            System.out.println("✅ Found " + tracks.size() + " tracks");

            // 2. Save basic info to database (title, artist, thumbnail)
            for (Track track : tracks) {
                try {
                    // Set default metadata first
                    if (track.getGenre() == null || track.getGenre().isEmpty()) {
                        track.setGenre("Music"); // Temporary
                    }
                    if (track.getMood() == null || track.getMood().isEmpty()) {
                        track.setMood("Unknown"); // Temporary
                    }
                    if (track.getBpm() <= 0) {
                        track.setBpm(120); // Temporary
                    }

                    dbManager.saveTrack(track);
                } catch (SQLException e) {
                    System.err.println("⚠️ Failed to save track: " + e.getMessage());
                }
            }

            // 3. Enrich metadata in background (non-blocking!)
            System.out.println("🔄 Starting metadata enrichment in background...");
            enrichmentService.enrichTracksAsync(tracks);

            return tracks;

        } catch (Exception e) {
            System.err.println("❌ Search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== Audio Fingerprinting Operations ==========

    public void startRecording() throws Exception {
        recordingService.startRecording();
    }

    public Track stopRecordingAndIdentify() throws Exception {
        tempRecordingFile = recordingService.stopRecording("temp_recording.wav");
        Track track = fingerprintService.identifyTrack(tempRecordingFile);

        if (track != null) {
            // Enrich metadata
            enrichmentService.enrichTrackSync(track);
        }

        if (tempRecordingFile != null && tempRecordingFile.exists()) {
            tempRecordingFile.delete();
        }

        return track;
    }

    public Track identifyTrack(File audioFile) throws Exception {
        Track track = fingerprintService.identifyTrack(audioFile);

        if (track != null) {
            // Enrich metadata
            enrichmentService.enrichTrackSync(track);
        }

        return track;
    }

    public void saveTrack(Track track) {
        try {
            dbManager.saveTrack(track);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== Recommendation Operations ==========

    public List<Recommendation> generateDailyMix(int limit) {
        try {
            return recommendationService.generateDailyMix(limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Recommendation> getSimilarTracks(Track track, int limit) {
        try {
            return recommendationService.getSimilarTracks(track, limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Recommendation> getSimilarToRecent(int limit) {
        try {
            List<Track> recentTracks = dbManager.getMostPlayedTracks(1);
            if (recentTracks.isEmpty()) {
                return new ArrayList<>();
            }

            return recommendationService.getSimilarTracks(recentTracks.get(0), limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== Playback Operations ==========

    public void setQueueAndPlay(List<Track> tracks, int startIndex) {
        audioPlayerService.setQueueAndPlay(tracks, startIndex);
    }

    public void play() {
        audioPlayerService.play();
    }

    public void pause() {
        audioPlayerService.pause();
    }

    public void stop() {
        audioPlayerService.stop();
    }

    public void togglePlayPause() {
        audioPlayerService.togglePlayPause();
    }

    public void playNext() {
        audioPlayerService.playNext();
    }

    public void playPrevious() {
        audioPlayerService.playPrevious();
    }

    public void setVolume(int volume) {
        audioPlayerService.setVolume(volume);
    }

    public void seekTo(long seconds) {
        audioPlayerService.seekTo(seconds);
    }

    public void toggleShuffle() {
        audioPlayerService.toggleShuffle();
    }

    public void toggleRepeat() {
        audioPlayerService.toggleRepeat();
    }

    public boolean isPlayerPlaying() {
        return audioPlayerService.isPlaying();
    }

    public Track getCurrentTrack() {
        return audioPlayerService.getCurrentTrack();
    }

    public long getPlayerCurrentTime() {
        return audioPlayerService.getCurrentTime();
    }

    public long getPlayerDuration() {
        return audioPlayerService.getDuration();
    }

    public boolean isShuffleEnabled() {
        return audioPlayerService.isShuffle();
    }

    public boolean isRepeatEnabled() {
        return audioPlayerService.isRepeat();
    }

    public List<Track> getAllTracks() {
        try {
            return dbManager.getAllTracks();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Cleanup resources - UPDATED
     */
    public void shutdown() {
        enrichmentService.shutdown(); // NEW!
        audioPlayerService.release();
        dbManager.close();
    }
}
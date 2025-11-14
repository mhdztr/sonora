package com.musicplayer.service;

import com.musicplayer.model.PlayHistory;
import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Recommendation Engine
 * Kombinasi Collaborative Filtering dan Content-Based Filtering
 */
public class RecommendationService {
    private final DatabaseManager dbManager;

    public RecommendationService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Generate rekomendasi lagu serupa berdasarkan track yang sedang diputar
     */
    public List<Recommendation> getSimilarTracks(Track currentTrack, int limit)
            throws SQLException {
        List<Track> allTracks = dbManager.getAllTracks();
        List<Recommendation> recommendations = new ArrayList<>();

        for (Track track : allTracks) {
            if (track.getId().equals(currentTrack.getId())) {
                continue; // Skip track yang sama
            }

            double similarity = calculateContentSimilarity(currentTrack, track);

            if (similarity > 0.3) { // Threshold similarity
                recommendations.add(new Recommendation(
                        track,
                        similarity,
                        "Similar to " + currentTrack.getTitle()
                ));
            }
        }

        // Sort by similarity score
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        return recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Generate Daily Mix playlist berdasarkan histori listening
     */
    public List<Recommendation> generateDailyMix(int limit) throws SQLException {
        // Get most played tracks
        List<Track> mostPlayed = dbManager.getMostPlayedTracks(20);

        if (mostPlayed.isEmpty()) {
            return Collections.emptyList();
        }

        // Analyze user preferences
        Map<String, Integer> genrePreference = new HashMap<>();
        Map<String, Integer> moodPreference = new HashMap<>();
        int totalBpm = 0;

        for (Track track : mostPlayed) {
            // Count genre
            if (track.getGenre() != null) {
                genrePreference.merge(track.getGenre(), 1, Integer::sum);
            }

            // Count mood
            if (track.getMood() != null) {
                moodPreference.merge(track.getMood(), 1, Integer::sum);
            }

            // Sum BPM
            if (track.getBpm() > 0) {
                totalBpm += track.getBpm();
            }
        }

        int avgBpm = mostPlayed.isEmpty() ? 120 : totalBpm / mostPlayed.size();

        // Get all tracks and score them
        List<Track> allTracks = dbManager.getAllTracks();
        List<Recommendation> recommendations = new ArrayList<>();

        for (Track track : allTracks) {
            // Skip if already in most played
            boolean alreadyPlayed = mostPlayed.stream()
                    .anyMatch(t -> t.getId().equals(track.getId()));
            if (alreadyPlayed) continue;

            double score = calculateUserPreferenceScore(
                    track,
                    genrePreference,
                    moodPreference,
                    avgBpm
            );

            if (score > 0.4) {
                recommendations.add(new Recommendation(
                        track,
                        score,
                        "Based on your listening history"
                ));
            }
        }

        // Sort by score and limit
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        // Mix with some of user's favorites
        List<Recommendation> dailyMix = new ArrayList<>();
        int favoriteCount = Math.min(3, mostPlayed.size());

        for (int i = 0; i < favoriteCount && i < mostPlayed.size(); i++) {
            dailyMix.add(new Recommendation(
                    mostPlayed.get(i),
                    1.0,
                    "Your favorite"
            ));
        }

        // Add recommendations
        dailyMix.addAll(recommendations.stream()
                .limit(limit - favoriteCount)
                .collect(Collectors.toList()));

        // Shuffle to make it interesting
        Collections.shuffle(dailyMix.subList(favoriteCount, dailyMix.size()));

        return dailyMix;
    }

    /**
     * Calculate content-based similarity between two tracks
     */
    private double calculateContentSimilarity(Track track1, Track track2) {
        double similarity = 0.0;
        int factors = 0;

        // Genre similarity (40% weight)
        if (track1.getGenre() != null && track2.getGenre() != null) {
            if (track1.getGenre().equalsIgnoreCase(track2.getGenre())) {
                similarity += 0.4;
            }
            factors++;
        }

        // Mood similarity (30% weight)
        if (track1.getMood() != null && track2.getMood() != null) {
            if (track1.getMood().equalsIgnoreCase(track2.getMood())) {
                similarity += 0.3;
            }
            factors++;
        }

        // BPM similarity (20% weight)
        if (track1.getBpm() > 0 && track2.getBpm() > 0) {
            int bpmDiff = Math.abs(track1.getBpm() - track2.getBpm());
            double bpmSimilarity = Math.max(0, 1.0 - (bpmDiff / 100.0));
            similarity += bpmSimilarity * 0.2;
            factors++;
        }

        // Artist similarity (10% weight)
        if (track1.getArtist() != null && track2.getArtist() != null) {
            if (track1.getArtist().equalsIgnoreCase(track2.getArtist())) {
                similarity += 0.1;
            }
            factors++;
        }

        return factors > 0 ? similarity : 0.0;
    }

    /**
     * Calculate score based on user preferences
     */
    private double calculateUserPreferenceScore(
            Track track,
            Map<String, Integer> genrePreference,
            Map<String, Integer> moodPreference,
            int avgBpm) {

        double score = 0.0;

        // Genre match
        if (track.getGenre() != null && genrePreference.containsKey(track.getGenre())) {
            int genreCount = genrePreference.get(track.getGenre());
            score += (genreCount / 20.0) * 0.4; // 40% weight
        }

        // Mood match
        if (track.getMood() != null && moodPreference.containsKey(track.getMood())) {
            int moodCount = moodPreference.get(track.getMood());
            score += (moodCount / 20.0) * 0.3; // 30% weight
        }

        // BPM proximity
        if (track.getBpm() > 0 && avgBpm > 0) {
            int bpmDiff = Math.abs(track.getBpm() - avgBpm);
            double bpmScore = Math.max(0, 1.0 - (bpmDiff / 100.0));
            score += bpmScore * 0.3; // 30% weight
        }

        return Math.min(score, 1.0);
    }
}
package com.musicplayer.util;

/**
 * Utility untuk audio operations
 */
public class AudioUtils {

    /**
     * Convert bytes to human readable format
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Get audio quality description from bitrate
     */
    public static String getQualityDescription(int bitrate) {
        if (bitrate < 128) {
            return "Low Quality";
        } else if (bitrate < 192) {
            return "Standard Quality";
        } else if (bitrate < 256) {
            return "High Quality";
        } else {
            return "Very High Quality";
        }
    }

    /**
     * Classify BPM range
     */
    public static String getBPMCategory(int bpm) {
        if (bpm < 60) {
            return "Very Slow";
        } else if (bpm < 80) {
            return "Slow";
        } else if (bpm < 100) {
            return "Moderate";
        } else if (bpm < 120) {
            return "Medium Fast";
        } else if (bpm < 140) {
            return "Fast";
        } else if (bpm < 160) {
            return "Very Fast";
        } else {
            return "Extremely Fast";
        }
    }

    /**
     * Calculate similarity score between two BPM values
     */
    public static double calculateBPMSimilarity(int bpm1, int bpm2) {
        if (bpm1 <= 0 || bpm2 <= 0) {
            return 0.0;
        }

        int diff = Math.abs(bpm1 - bpm2);
        double maxDiff = 100.0; // Maximum expected difference

        return Math.max(0, 1.0 - (diff / maxDiff));
    }

    /**
     * Normalize volume (0-100)
     */
    public static int normalizeVolume(int volume) {
        return Math.max(0, Math.min(100, volume));
    }

    /**
     * Convert volume percentage to gain (decibels)
     */
    public static float volumeToGain(int volumePercent) {
        if (volumePercent <= 0) {
            return -80.0f; // Minimum gain (muted)
        }
        if (volumePercent >= 100) {
            return 6.0f; // Maximum gain
        }

        // Logarithmic scale for more natural volume control
        return (float) (Math.log10(volumePercent / 100.0) * 20.0);
    }
}
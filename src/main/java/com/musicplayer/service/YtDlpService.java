
package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class YtDlpService {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Get stream URL using yt-dlp
     */
    public String getStreamUrl(String videoId) {
        try {
            String url = "https://music.youtube.com/watch?v=" + videoId;

            // Run yt-dlp command
            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "--format", "bestaudio",
                    "--get-url",
                    "--no-playlist",
                    url
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String streamUrl = reader.readLine();
            process.waitFor();

            if (streamUrl != null && !streamUrl.isEmpty()) {
                System.out.println("✅ Stream URL obtained via yt-dlp");
                return streamUrl;
            }

        } catch (Exception e) {
            System.err.println("yt-dlp error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get full track info via yt-dlp
     */
    public JsonNode getTrackInfo(String videoId) {
        try {
            String url = "https://music.youtube.com/watch?v=" + videoId;

            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "--dump-json",
                    "--no-playlist",
                    url
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            process.waitFor();

            return mapper.readTree(json.toString());

        } catch (Exception e) {
            System.err.println("yt-dlp info error: " + e.getMessage());
        }

        return null;
    }
}
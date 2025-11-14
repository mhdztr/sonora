package com.musicplayer.service;



import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.musicplayer.model.Track;

import okhttp3.*;



import java.io.IOException;

import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.TimeUnit;



/**

 * Service untuk mengakses YouTube Music.

 * Gunakan InnerTube API hanya untuk search, dan yt-dlp untuk streaming.

 */

public class YouTubeMusicService {

    private static final String INNERTUBE_API_URL = "https://music.youtube.com/youtubei/v1/";

    private static final String API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";

    private static final String CLIENT_NAME = "WEB_REMIX";

    private static final String CLIENT_VERSION = "1.20231115.01.00";



    private final OkHttpClient client;

    private final ObjectMapper mapper;

    private final YtDlpService ytDlpService;



    public YouTubeMusicService() {

        this.client = new OkHttpClient.Builder()

                .connectTimeout(15, TimeUnit.SECONDS)

                .readTimeout(15, TimeUnit.SECONDS)

                .build();

        this.mapper = new ObjectMapper();

        this.ytDlpService = new YtDlpService();

    }



    /**

     * Search lagu di YouTube Music menggunakan InnerTube API.

     * Endpoint ini masih stabil untuk pencarian.

     */

    public List<Track> searchTracks(String query) throws IOException {

        List<Track> tracks = new ArrayList<>();



        ObjectNode context = createContext();

        ObjectNode requestBody = mapper.createObjectNode();

        requestBody.set("context", context);

        requestBody.put("query", query);

        requestBody.put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV"); // Filter: Songs



        String url = INNERTUBE_API_URL + "search?key=" + API_KEY;



        RequestBody body = RequestBody.create(

                requestBody.toString(),

                MediaType.parse("application/json")

        );



        Request request = new Request.Builder()

                .url(url)

                .post(body)

                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

                .addHeader("Accept-Language", "en-US,en;q=0.9")

                .addHeader("Origin", "https://music.youtube.com")

                .build();



        try (Response response = client.newCall(request).execute()) {

            if (response.isSuccessful()) {

                String jsonResponse = response.body().string();

                tracks = parseSearchResponse(jsonResponse);

            } else {

                System.err.println("❌ Search failed: " + response.code());

            }

        }



        return tracks;

    }



    /**

     * Ambil streaming URL dari yt-dlp, bukan dari InnerTube.

     */

    public String getStreamUrl(String videoId) {

        if (videoId == null || videoId.isEmpty()) {

            System.err.println("Invalid videoId");

            return null;

        }

        return ytDlpService.getStreamUrl(videoId);

    }



    /**

     * Ambil info lengkap track (metadata) via yt-dlp.

     */

    public JsonNode getTrackInfo(String videoId) {

        if (videoId == null || videoId.isEmpty()) {

            System.err.println("Invalid videoId");

            return null;

        }

        return ytDlpService.getTrackInfo(videoId);

    }



    /**

     * Context untuk InnerTube API.

     */

    private ObjectNode createContext() {

        ObjectNode context = mapper.createObjectNode();

        ObjectNode clientNode = mapper.createObjectNode();



        clientNode.put("clientName", CLIENT_NAME);

        clientNode.put("clientVersion", CLIENT_VERSION);

        clientNode.put("hl", "en");

        clientNode.put("gl", "US");



        context.set("client", clientNode);

        return context;

    }



    /**

     * Parse response hasil search dari YouTube Music.

     */

    private List<Track> parseSearchResponse(String jsonResponse) throws IOException {

        List<Track> tracks = new ArrayList<>();

        JsonNode root = mapper.readTree(jsonResponse);



        JsonNode contents = root.path("contents")

                .path("tabbedSearchResultsRenderer")

                .path("tabs");



        if (contents.isArray() && contents.size() > 0) {

            JsonNode sectionList = contents.get(0)

                    .path("tabRenderer")

                    .path("content")

                    .path("sectionListRenderer")

                    .path("contents");



            for (JsonNode section : sectionList) {

                JsonNode musicShelf = section.path("musicShelfRenderer");

                if (musicShelf.isMissingNode()) continue;



                JsonNode contentsList = musicShelf.path("contents");

                for (JsonNode item : contentsList) {

                    Track track = parseMusicItem(item);

                    if (track != null) {

                        tracks.add(track);

                    }

                }

            }

        }



        return tracks;

    }



    /**

     * Parse satu item lagu dari hasil pencarian.

     */

    private Track parseMusicItem(JsonNode item) {

        JsonNode renderer = item.path("musicResponsiveListItemRenderer");

        if (renderer.isMissingNode()) return null;



        Track track = new Track();



        JsonNode playlistItemData = renderer.path("playlistItemData");

        if (!playlistItemData.isMissingNode()) {

            String videoId = playlistItemData.path("videoId").asText("");

            track.setYoutubeId(videoId);

            track.setId(videoId);

        }



        JsonNode flexColumns = renderer.path("flexColumns");

        if (flexColumns.isArray() && flexColumns.size() > 0) {

            JsonNode titleRuns = flexColumns.get(0)

                    .path("musicResponsiveListItemFlexColumnRenderer")

                    .path("text")

                    .path("runs");

            if (titleRuns.isArray() && titleRuns.size() > 0) {

                track.setTitle(titleRuns.get(0).path("text").asText("Unknown"));

            }



            if (flexColumns.size() > 1) {

                JsonNode detailRuns = flexColumns.get(1)

                        .path("musicResponsiveListItemFlexColumnRenderer")

                        .path("text")

                        .path("runs");

                if (detailRuns.isArray() && detailRuns.size() > 0) {

                    track.setArtist(detailRuns.get(0).path("text").asText("Unknown"));

                    if (detailRuns.size() > 2) {

                        track.setAlbum(detailRuns.get(2).path("text").asText(""));

                    }

                }

            }

        }



        JsonNode thumbnail = renderer.path("thumbnail")

                .path("musicThumbnailRenderer")

                .path("thumbnail")

                .path("thumbnails");

        if (thumbnail.isArray() && thumbnail.size() > 0) {

            track.setThumbnailUrl(thumbnail.get(0).path("url").asText(""));

        }



        return track;

    }

}
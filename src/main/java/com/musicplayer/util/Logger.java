package com.musicplayer.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logger untuk debugging
 */
public class Logger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final boolean DEBUG_MODE = true;

    public static void info(String message) {
        log("INFO", message);
    }

    public static void debug(String message) {
        if (DEBUG_MODE) {
            log("DEBUG", message);
        }
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message);
        throwable.printStackTrace();
    }

    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println(String.format("[%s] %s: %s", timestamp, level, message));
    }
}
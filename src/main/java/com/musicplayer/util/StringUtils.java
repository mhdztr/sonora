package com.musicplayer.util;

/**
 * Utility untuk string operations
 */
public class StringUtils {

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Truncate string dengan ellipsis
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Generate safe filename dari string
     */
    public static String toSafeFilename(String str) {
        if (isEmpty(str)) {
            return "untitled";
        }
        return str.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }

    /**
     * Remove special characters
     */
    public static String removeSpecialChars(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Count words in string
     */
    public static int countWords(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        return str.trim().split("\\s+").length;
    }
}
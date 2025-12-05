package com.cope.meteoraddons.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class TimeUtil {
    public static String getRelativeTime(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "Unknown";
        }

        try {
            Instant updateTime = Instant.parse(isoTimestamp);
            Instant now = Instant.now();
            Duration duration = Duration.between(updateTime, now);

            long seconds = duration.getSeconds();

            if (seconds < 60) {
                return "Just now";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            } else if (seconds < 86400) {
                long hours = seconds / 3600;
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            } else if (seconds < 2592000) { // 30 days
                long days = seconds / 86400;
                return days + (days == 1 ? " day ago" : " days ago");
            } else if (seconds < 31536000) { // 365 days
                long months = seconds / 2592000;
                return months + (months == 1 ? " month ago" : " months ago");
            } else {
                long years = seconds / 31536000;
                return years + (years == 1 ? " year ago" : " years ago");
            }
        } catch (DateTimeParseException e) {
            return isoTimestamp;
        }
    }
}

package dev.gracco.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class Validation {
    // This regex uses RFC 5322, which has a 99.99% accuracy
    public static final Pattern EMAIL_REGEX = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
    public static final Pattern USERNAME_REGEX = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    public static final Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\+?\\d+$");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm:ss a");

    public static String formatDateTime(String input) {
        LocalDateTime dateTime = Timestamp.valueOf(input).toLocalDateTime();
        return dateTime.format(formatter);
    }
}

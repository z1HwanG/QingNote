package com.example.notepad.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }

        Date now = new Date();
        long diffInMillis = now.getTime() - date.getTime();
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (diffInDays == 0) {
            // 今天
            return "今天, " + TIME_FORMAT.format(date);
        } else if (diffInDays == 1) {
            // 昨天
            return "昨天, " + TIME_FORMAT.format(date);
        } else if (diffInDays < 7) {
            // 本周内
            return diffInDays + "天前, " + TIME_FORMAT.format(date);
        } else {
            // 一周前
            return DATE_FORMAT.format(date);
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_ONLY_FORMAT.format(date);
    }
} 
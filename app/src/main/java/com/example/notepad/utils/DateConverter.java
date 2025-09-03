package com.example.notepad.utils;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * 日期转换器类，用于Room数据库中存储和读取Date类型
 */
public class DateConverter {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 
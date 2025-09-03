package com.example.notepad.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.room.TypeConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * URI转换器，用于处理Room数据库中URI的存储和转换
 * 同时提供URI持久化和处理相关的工具方法
 */
public class UriConverter {

    private static final String TAG = "UriConverter";

    /**
     * 转换URI为字符串以便存储到数据库
     */
    @TypeConverter
    public static String fromUri(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    /**
     * 从数据库中的字符串还原为URI
     */
    @TypeConverter
    public static Uri toUri(String uriString) {
        return uriString == null ? null : Uri.parse(uriString);
    }

    /**
     * 尝试为URI获取持久访问权限
     */
    public static void takePersistablePermission(Context context, Uri uri) {
        if (uri != null && uri.toString().startsWith("content://")) {
            try {
                context.getContentResolver().takePersistableUriPermission(
                    uri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                Log.d(TAG, "成功获取URI持久访问权限: " + uri);
            } catch (SecurityException e) {
                Log.e(TAG, "无法获取URI持久访问权限: " + e.getMessage());
            }
        }
    }

    /**
     * 获取文件的真实路径
     */
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        
        // 处理不同URI类型
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String displayName = getFileName(context, uri);
            if (displayName != null) {
                return displayName;
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }
        
        return uri.toString();
    }

    /**
     * 从URI获取文件名
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取文件名失败: " + e.getMessage());
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * 将URI指向的文件复制到应用的内部存储中
     * 用于确保即使原始URI失效，文件仍然可以访问
     */
    public static Uri copyUriToInternalStorage(Context context, Uri sourceUri, String destFileName) {
        if (sourceUri == null) return null;
        
        File outputDir = new File(context.getFilesDir(), "avatars");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        File outputFile = new File(outputDir, destFileName);
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            
            if (inputStream == null) return null;
            
            byte[] buffer = new byte[4 * 1024]; // 4kb buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            
            return Uri.fromFile(outputFile);
        } catch (IOException e) {
            Log.e(TAG, "复制文件失败: " + e.getMessage());
            return null;
        }
    }
} 
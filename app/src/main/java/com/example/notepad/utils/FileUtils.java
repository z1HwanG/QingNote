package com.example.notepad.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    
    /**
     * 创建图片文件
     */
    public static File createImageFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        // 确保目录存在
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("FileUtils", "无法创建目录: " + storageDir.getAbsolutePath());
            }
        }
        
        try {
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            Log.d("FileUtils", "创建图片文件: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            Log.e("FileUtils", "创建图片文件失败", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建音频文件
     */
    public static File createAudioFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String audioFileName = "AUDIO_" + timeStamp + "_";
        
        // 确保目录存在并创建目录结构
        File baseDir = context.getExternalFilesDir(null);
        File audioDir = new File(baseDir, "Audio");
        
        if (!audioDir.exists()) {
            boolean dirCreated = audioDir.mkdirs();
            Log.d("FileUtils", "创建音频目录: " + dirCreated + " 路径: " + audioDir.getAbsolutePath());
        }
        
        try {
            // 直接在自定义Audio目录中创建文件
            File audioFile = File.createTempFile(audioFileName, ".m4a", audioDir);
            Log.d("FileUtils", "创建音频文件: " + audioFile.getAbsolutePath());
            return audioFile;
        } catch (IOException e) {
            Log.e("FileUtils", "创建音频文件失败", e);
            
            // 尝试备用方法
            try {
                File fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (fallbackDir != null && !fallbackDir.exists()) {
                    fallbackDir.mkdirs();
                }
                
                File audioFile = new File(fallbackDir, audioFileName + ".m4a");
                if (!audioFile.exists()) {
                    boolean created = audioFile.createNewFile();
                    Log.d("FileUtils", "备用方法创建文件: " + created);
                }
                
                Log.d("FileUtils", "使用备用方法创建音频文件: " + audioFile.getAbsolutePath());
                return audioFile;
            } catch (IOException e2) {
                Log.e("FileUtils", "备用方法创建音频文件也失败", e2);
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * 从Uri获取文件路径
     */
    public static String getPathFromUri(Context context, Uri uri) {
        Log.d("FileUtils", "获取路径: " + uri.toString());
        
        // 如果是文件URI，直接返回路径
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        
        // 处理content URI
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String path = null;
            
            // 尝试从MediaStore获取路径
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (columnIndex != -1) {
                        path = cursor.getString(columnIndex);
                        if (path != null) {
                            Log.d("FileUtils", "从MediaStore找到路径: " + path);
                            return path;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("FileUtils", "查询MediaStore失败", e);
            }
            
            // 如果从MediaStore获取失败，尝试直接从Content Provider获取
            try {
                InputStream input = context.getContentResolver().openInputStream(uri);
                if (input != null) {
                    // 创建临时文件保存内容
                    String fileName = getFileName(context, uri);
                    File tempFile = new File(context.getCacheDir(), fileName);
                    copyInputStreamToFile(input, tempFile);
                    input.close();
                    Log.d("FileUtils", "创建临时文件: " + tempFile.getAbsolutePath());
                    return tempFile.getAbsolutePath();
                }
            } catch (Exception e) {
                Log.e("FileUtils", "复制内容失败", e);
            }
        }
        
        // 最后尝试直接获取路径
        String path = uri.getPath();
        Log.d("FileUtils", "使用URI路径: " + path);
        return path;
    }
    
    /**
     * 将输入流复制到文件
     */
    private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }
    }

    /**
     * 从Uri获取文件名
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * 获取文件大小
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            }
        }
        if (size == 0) {
            String path = getPathFromUri(context, uri);
            if (path != null) {
                File file = new File(path);
                size = file.length();
            }
        }
        return size;
    }

    /**
     * 复制文件
     */
    public static void copyFile(Context context, Uri sourceUri, File destFile) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {
            
            if (in == null) {
                throw new IOException("无法打开源文件");
            }

            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    /**
     * 复制文件
     * @param sourceFile 源文件
     * @param destFile 目标文件
     * @throws IOException 复制失败时抛出异常
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("源文件不存在: " + sourceFile.getAbsolutePath());
        }
        
        java.io.FileInputStream fis = null;
        java.io.FileOutputStream fos = null;
        
        try {
            fis = new java.io.FileInputStream(sourceFile);
            fos = new java.io.FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int length;
            
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 根据文件路径获取MIME类型
     * @param filePath 文件路径
     * @return MIME类型字符串
     */
    public static String getMimeType(String filePath) {
        String extension = getFileExtension(filePath);
        if (extension == null) {
            return null;
        }
        
        switch (extension.toLowerCase(Locale.ROOT)) {
            // 图片类型
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
                
            // 音频类型
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "m4a":
                return "audio/mp4";
            case "aac":
                return "audio/aac";
                
            // 视频类型
            case "mp4":
                return "video/mp4";
            case "3gp":
                return "video/3gpp";
            case "webm":
                return "video/webm";
            case "mkv":
                return "video/x-matroska";
                
            // 文档类型
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
                
            // 压缩文件类型
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "gz":
                return "application/gzip";
            case "7z":
                return "application/x-7z-compressed";
                
            default:
                return null;
        }
    }
    
    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 扩展名字符串（不含点号）
     */
    public static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
            return filePath.substring(dotIndex + 1);
        }
        
        return null;
    }
} 
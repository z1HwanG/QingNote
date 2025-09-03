package com.example.notepad.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限工具类，用于处理不同Android版本的权限请求
 */
public class PermissionUtils {
    private static final String TAG = "PermissionUtils";

    // 权限请求码
    public static final int REQUEST_CAMERA_PERMISSIONS = 100;
    public static final int REQUEST_STORAGE_PERMISSIONS = 101;
    public static final int REQUEST_AUDIO_PERMISSIONS = 102;
    public static final int REQUEST_ALL_PERMISSIONS = 103;

    /**
     * 检查相机权限
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查存储权限
     */
    public static boolean hasStoragePermission(Context context) {
        // Android 10+ (API 29+)需要使用新的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true; // Android 11+可以通过作用域存储访问媒体，不需要显式权限
        } 
        // Android 10 (API 29)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        } 
        // Android 9及以下
        else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 检查录音权限
     */
    public static boolean hasAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求相机权限
     */
    public static void requestCameraPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        
        // 相机权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        // 存储权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q 
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissions.toArray(new String[0]), 
                    REQUEST_CAMERA_PERMISSIONS);
        }
    }

    /**
     * 请求存储权限
     */
    public static void requestStoragePermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        
        // Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } 
        // Android 10-12
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } 
        // Android 9及以下
        else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissions.toArray(new String[0]), 
                    REQUEST_STORAGE_PERMISSIONS);
        }
    }

    /**
     * 请求录音权限
     */
    public static void requestAudioPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_AUDIO_PERMISSIONS);
        }
    }

    /**
     * 请求所有所需权限
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        
        // 相机权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        // 存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && 
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        // 录音权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (!permissions.isEmpty()) {
            Log.d(TAG, "请求权限: " + permissions);
            ActivityCompat.requestPermissions(activity, 
                    permissions.toArray(new String[0]), 
                    REQUEST_ALL_PERMISSIONS);
        }
    }

    /**
     * 打开应用设置页面
     */
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    /**
     * 判断是否应该显示权限说明
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
} 
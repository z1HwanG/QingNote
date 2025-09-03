package com.example.notepad.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.notepad.data.model.User;
import com.google.gson.Gson;

/**
 * 会话管理类，用于管理用户登录状态和存储用户信息
 */
public class SessionManager {
    private static final String PREF_NAME = "QingNotePrefs";
    private static final String KEY_USER = "user";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Gson gson;
    
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }
    
    /**
     * 创建登录会话
     */
    public void createLoginSession(int userId, String username, String email, String fullName) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }
    
    /**
     * 获取用户详细信息
     */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }
    
    public String getUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : "";
    }
    
    public String getEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : "";
    }
    
    public String getFullName() {
        User user = getCurrentUser();
        return user != null ? user.getFullName() : "";
    }
    
    /**
     * 检查用户是否已登录
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * 清除会话，退出登录
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    public void saveUser(User user) {
        if (user == null) {
            android.util.Log.e("SessionManager", "尝试保存空用户");
            return;
        }
        
        try {
            String userJson = gson.toJson(user);
            editor.putString(KEY_USER, userJson);
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            
            // 同时更新单独的字段，增加冗余以提高可靠性
            editor.putInt(KEY_USER_ID, user.getId());
            editor.putString(KEY_USERNAME, user.getUsername());
            editor.putString(KEY_EMAIL, user.getEmail());
            editor.putString(KEY_FULL_NAME, user.getFullName());
            
            // 使用apply异步写入，避免阻塞主线程
            editor.apply();
            android.util.Log.d("SessionManager", "用户信息已保存: " + user.getUsername());
        } catch (Exception e) {
            android.util.Log.e("SessionManager", "保存用户信息失败: " + e.getMessage());
        }
    }

    public User getCurrentUser() {
        String userJson = prefs.getString(KEY_USER, null);
        User user = null;
        
        if (userJson != null) {
            try {
                user = gson.fromJson(userJson, User.class);
            } catch (Exception e) {
                // JSON解析错误，尝试使用单独存储的字段重建用户对象
                user = null;
            }
        }
        
        // 如果无法从JSON获取，则尝试从单独的字段重建
        if (user == null && isLoggedIn()) {
            user = new User();
            user.setId(prefs.getInt(KEY_USER_ID, -1));
            user.setUsername(prefs.getString(KEY_USERNAME, ""));
            user.setEmail(prefs.getString(KEY_EMAIL, ""));
            user.setFullName(prefs.getString(KEY_FULL_NAME, ""));
        }
        
        return user;
    }
} 
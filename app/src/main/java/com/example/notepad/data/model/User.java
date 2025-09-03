package com.example.notepad.data.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String avatarUrl;
    
    /**
     * 默认构造函数（Room需要）
     */
    public User() {
    }
    
    /**
     * 带参数的构造函数（注册用户）
     */
    @Ignore
    public User(String username, String password, String email) {
        this.username = username;
        this.email = email;
        this.password = password != null ? password : "";
    }
    
    /**
     * 带参数的构造函数
     */
    @Ignore
    public User(String username, String email, String password, String fullName) {
        this.username = username;
        this.email = email;
        this.password = password != null ? password : "";
        this.fullName = fullName;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    // Backward compatibility for old code that might use profileImage
    public String getProfileImage() {
        return avatarUrl;
    }
    
    public void setProfileImage(String profileImage) {
        this.avatarUrl = profileImage;
    }
} 
package com.example.notepad.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import com.example.notepad.utils.DateConverter;

import java.util.Date;
import java.util.List;

/**
 * 笔记实体类，用于存储笔记信息
 */
@Entity(tableName = "notes", 
        foreignKeys = @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "userId",
            onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId")}
)
@TypeConverters(DateConverter.class)
public class Note {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String title;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int userId;
    private String imagePath; // 存储图片路径
    
    /**
     * 默认构造函数 - Room需要
     */
    public Note() {
        // 空构造函数，Room需要
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    /**
     * 创建新笔记的构造函数
     * @param title 笔记标题
     * @param content 笔记内容
     * @param userId 用户ID
     */
    @Ignore
    public Note(String title, String content, int userId) {
        this.title = title;
        this.content = content;
        this.userId = userId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
} 
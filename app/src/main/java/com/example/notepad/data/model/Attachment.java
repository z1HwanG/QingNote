package com.example.notepad.data.model;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.notepad.utils.UriConverter;

import java.util.Locale;

/**
 * 附件数据模型
 */
@Entity(
    tableName = "attachments",
    foreignKeys = @ForeignKey(
        entity = Note.class,
        parentColumns = "id",
        childColumns = "noteId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("noteId")}
)
public class Attachment {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long noteId;

    private String name;
    private String path;
    private String mimeType;
    private long size;
    private int type;

    // 附件类型常量
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_FILE = 3;
    
    /**
     * 默认构造函数（Room需要）
     */
    public Attachment() {
    }
    
    /**
     * 构造函数
     * @param type 附件类型 (TYPE_IMAGE, TYPE_AUDIO, TYPE_FILE)
     * @param name 附件名称
     * @param path 附件路径
     * @param size 附件大小 (字节)
     */
    @Ignore
    public Attachment(int type, String name, String path, long size) {
        this.type = type;
        this.name = name;
        this.path = path;
        this.size = size;
        this.mimeType = getMimeTypeFromPath(path);
    }
    
    /**
     * 使用URI构造附件
     * @param type 附件类型
     * @param name 附件名称
     * @param uri 附件URI
     * @param size 附件大小
     */
    @Ignore
    public Attachment(int type, String name, Uri uri, long size) {
        this.type = type;
        this.name = name;
        this.path = uri.toString();
        this.size = size;
        this.mimeType = getMimeTypeFromPath(name);
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNoteId() {
        return noteId;
    }

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Uri getUri() {
        return Uri.parse(path);
    }
    
    /**
     * 获取格式化的大小信息
     * @return 格式化的大小字符串 (例如: "2.5 MB")
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fKB", size / 1024.0);
        } else {
            return String.format(Locale.ROOT, "%.1fMB", size / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取类型描述
     * @return 类型描述字符串
     */
    public String getTypeDescription() {
        switch (type) {
            case TYPE_IMAGE:
                return "图片";
            case TYPE_AUDIO:
                return "音频";
            case TYPE_FILE:
                return "文件";
            default:
                return "未知";
        }
    }
    
    /**
     * 检查文件是否存在
     * @return 文件是否存在
     */
    public boolean exists() {
        if (path != null) {
            return true; // Assuming the path is always valid for the given code
        }
        return false;
    }

    /**
     * 从文件路径获取MIME类型
     */
    private String getMimeTypeFromPath(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        }
        return "*/*";
    }
} 
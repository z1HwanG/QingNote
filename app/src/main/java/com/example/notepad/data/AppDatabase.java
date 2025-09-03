package com.example.notepad.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.notepad.data.dao.AttachmentDao;
import com.example.notepad.data.dao.NoteDao;
import com.example.notepad.data.dao.UserDao;
import com.example.notepad.data.model.Attachment;
import com.example.notepad.data.model.Note;
import com.example.notepad.data.model.User;
import com.example.notepad.utils.DateConverter;
import com.example.notepad.utils.UriConverter;

import java.util.concurrent.Executors;

/**
 * 应用数据库类，用于管理Room数据库
 */
@Database(entities = {User.class, Note.class, Attachment.class}, version = 3, exportSchema = false)
@TypeConverters({DateConverter.class, UriConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "qingnote_db";
    private static AppDatabase instance;
    
    public abstract UserDao userDao();
    public abstract NoteDao noteDao();
    public abstract AttachmentDao attachmentDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // 在新线程中创建默认用户
                            Executors.newSingleThreadExecutor().execute(() -> {
                                User defaultUser = new User();
                                defaultUser.setUsername("admin");
                                defaultUser.setPassword("admin123");
                                defaultUser.setEmail("admin@example.com");
                                defaultUser.setFullName("管理员");
                                getInstance(context).userDao().insert(defaultUser);
                            });
                        }
                    })
                    .build();
        }
        return instance;
    }
    
    /**
     * 清除数据库实例，用于测试和重置
     */
    public static void destroyInstance() {
        instance = null;
    }
} 
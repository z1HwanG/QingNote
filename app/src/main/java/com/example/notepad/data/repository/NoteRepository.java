package com.example.notepad.data.repository;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.notepad.data.AppDatabase;
import com.example.notepad.data.dao.NoteDao;
import com.example.notepad.data.dao.AttachmentDao;
import com.example.notepad.data.model.Note;
import com.example.notepad.data.model.Attachment;
import com.example.notepad.data.model.NoteWithAttachments;
import com.example.notepad.data.callback.InsertCallback;

import java.util.List;

/**
 * 笔记仓库类，用于管理笔记数据的访问
 */
public class NoteRepository {
    private static final int PAGE_SIZE = 20;
    private final NoteDao noteDao;
    private final AttachmentDao attachmentDao;
    private final Executor executor;
    private Handler mainHandler;
    
    public NoteRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        noteDao = db.noteDao();
        attachmentDao = db.attachmentDao();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public NoteRepository(Application application) {
        this((Context) application);
    }
    
    public void insert(Note note) {
        executor.execute(() -> noteDao.insert(note));
    }
    
    public void insert(Note note, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long noteId = noteDao.insert(note);
                note.setId(noteId);
                mainHandler.post(() -> callback.onComplete(note));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    public void update(Note note) {
        executor.execute(() -> noteDao.update(note));
    }
    
    public void delete(Note note) {
        executor.execute(() -> noteDao.delete(note));
    }
    
    public LiveData<Note> getNoteById(long noteId) {
        return noteDao.getNoteById(noteId);
    }
    
    public LiveData<PagingData<Note>> getAllNotesByUserPaged(int userId) {
        return PagingLiveData.getLiveData(
            new Pager<>(
                new PagingConfig(
                    PAGE_SIZE,
                    PAGE_SIZE,
                    false,
                    PAGE_SIZE * 3
                ),
                () -> noteDao.getAllNotesByUserPaged(userId)
            )
        );
    }
    
    public LiveData<List<Note>> getAllNotesByUser(int userId) {
        return noteDao.getAllNotesByUser(userId);
    }
    
    public LiveData<PagingData<Note>> searchNotesPaged(int userId, String query) {
        return PagingLiveData.getLiveData(
            new Pager<>(
                new PagingConfig(
                    PAGE_SIZE,
                    PAGE_SIZE,
                    false,
                    PAGE_SIZE * 3
                ),
                () -> noteDao.searchNotesPaged(userId, query)
            )
        );
    }
    
    public LiveData<List<Note>> searchNotes(int userId, String query) {
        return noteDao.searchNotes(userId, query);
    }
    
    public void deleteAllNotesByUser(int userId) {
        executor.execute(() -> noteDao.deleteAllNotesByUser(userId));
    }
    
    public LiveData<NoteWithAttachments> getNoteWithAttachments(long noteId) {
        return noteDao.getNoteWithAttachments(noteId);
    }
    
    /**
     * 删除附件
     * @param attachment 要删除的附件
     */
    public void deleteAttachment(Attachment attachment) {
        executor.execute(() -> attachmentDao.delete(attachment));
    }
} 
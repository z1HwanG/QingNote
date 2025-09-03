package com.example.notepad.ui.note;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingData;

import com.example.notepad.data.model.Note;
import com.example.notepad.data.model.Attachment;
import com.example.notepad.data.model.NoteWithAttachments;
import com.example.notepad.data.repository.NoteRepository;
import com.example.notepad.data.callback.InsertCallback;

import java.util.Date;
import java.util.List;

/**
 * 笔记ViewModel类，处理与笔记相关的业务逻辑
 */
public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    
    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
    }
    
    public void insert(Note note) {
        repository.insert(note);
    }
    
    public void insert(Note note, InsertCallback callback) {
        repository.insert(note, callback);
    }
    
    public void update(Note note) {
        // 更新时间戳
        note.setUpdatedAt(new Date());
        repository.update(note);
    }
    
    public void delete(Note note) {
        repository.delete(note);
    }
    
    public LiveData<Note> getNoteById(long noteId) {
        return repository.getNoteById(noteId);
    }
    
    public LiveData<PagingData<Note>> getAllNotesByUserPaged(int userId) {
        return repository.getAllNotesByUserPaged(userId);
    }
    
    public LiveData<List<Note>> getAllNotesByUser(int userId) {
        return repository.getAllNotesByUser(userId);
    }
    
    public LiveData<PagingData<Note>> searchNotesPaged(int userId, String query) {
        return repository.searchNotesPaged(userId, query);
    }
    
    public LiveData<List<Note>> searchNotes(int userId, String query) {
        return repository.searchNotes(userId, query);
    }
    
    public void deleteAllNotesByUser(int userId) {
        repository.deleteAllNotesByUser(userId);
    }
    
    public LiveData<NoteWithAttachments> getNoteWithAttachments(long noteId) {
        return repository.getNoteWithAttachments(noteId);
    }
    
    /**
     * 删除附件
     * @param attachment 要删除的附件
     */
    public void deleteAttachment(Attachment attachment) {
        repository.deleteAttachment(attachment);
    }
} 
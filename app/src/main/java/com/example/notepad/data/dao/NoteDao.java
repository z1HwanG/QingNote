package com.example.notepad.data.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.notepad.data.model.Note;
import com.example.notepad.data.model.NoteWithAttachments;

import java.util.List;

/**
 * 笔记数据访问对象接口
 */
@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Note note);
    
    @Update
    void update(Note note);
    
    @Delete
    void delete(Note note);
    
    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> getNoteById(long id);
    
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    PagingSource<Integer, Note> getAllNotesByUserPaged(int userId);
    
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    LiveData<List<Note>> getAllNotesByUser(int userId);
    
    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    PagingSource<Integer, Note> searchNotesPaged(int userId, String query);
    
    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<Note>> searchNotes(int userId, String query);
    
    @Query("DELETE FROM notes WHERE userId = :userId")
    void deleteAllNotesByUser(int userId);
    
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<NoteWithAttachments> getNoteWithAttachments(long noteId);
} 
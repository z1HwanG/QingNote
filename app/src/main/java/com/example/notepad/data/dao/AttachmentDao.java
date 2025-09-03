package com.example.notepad.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.notepad.data.model.Attachment;

import java.util.List;

@Dao
public interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Attachment attachment);

    @Update
    void update(Attachment attachment);

    @Delete
    void delete(Attachment attachment);

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    LiveData<Attachment> getAttachmentById(long id);

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY id ASC")
    LiveData<List<Attachment>> getAttachmentsByNoteId(long noteId);

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    void deleteAttachmentsByNoteId(long noteId);
} 
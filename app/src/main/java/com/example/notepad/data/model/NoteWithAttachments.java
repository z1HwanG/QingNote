package com.example.notepad.data.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class NoteWithAttachments {
    @Embedded
    public Note note;

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    public List<Attachment> attachments;
} 
package com.example.notepad.ui.note;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.notepad.data.model.Attachment;
import com.example.notepad.data.model.NoteWithAttachments;
import com.example.notepad.databinding.ActivityNoteDetailBinding;
import com.example.notepad.utils.DateUtils;

public class NoteDetailActivity extends AppCompatActivity implements AttachmentAdapter.AttachmentClickListener {
    private ActivityNoteDetailBinding binding;
    private NoteViewModel noteViewModel;
    private AttachmentAdapter attachmentAdapter;
    private long noteId;

    public static final String EXTRA_NOTE_ID = "extra_note_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // 初始化附件列表
        setupAttachmentsList();

        // 获取笔记ID
        if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
            noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
            if (noteId != -1) {
                loadNote();
            } else {
                Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupClickListeners();
    }

    private void setupAttachmentsList() {
        attachmentAdapter = new AttachmentAdapter(this, this);
        binding.recyclerAttachments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAttachments.setAdapter(attachmentAdapter);
    }

    private void loadNote() {
        noteViewModel.getNoteById(noteId).observe(this, note -> {
            if (note != null) {
                binding.textNoteTitle.setText(note.getTitle());
                binding.textNoteContent.setText(note.getContent());
                binding.textNoteDate.setText(DateUtils.formatDateTime(note.getUpdatedAt()));
                
                // 加载附件
                loadAttachments();
            } else {
                Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadAttachments() {
        noteViewModel.getNoteWithAttachments(noteId).observe(this, noteWithAttachments -> {
            if (noteWithAttachments != null && noteWithAttachments.attachments != null) {
                if (!noteWithAttachments.attachments.isEmpty()) {
                    binding.textAttachmentsTitle.setVisibility(View.VISIBLE);
                    binding.recyclerAttachments.setVisibility(View.VISIBLE);
                    attachmentAdapter.getAttachments().clear();
                    for (Attachment attachment : noteWithAttachments.attachments) {
                        attachmentAdapter.addAttachment(attachment);
                    }
                } else {
                    binding.textAttachmentsTitle.setVisibility(View.GONE);
                    binding.recyclerAttachments.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupClickListeners() {
        // 返回按钮点击事件
        binding.buttonBack.setOnClickListener(v -> finish());

        // 编辑按钮点击事件
        binding.buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateNoteActivity.class);
            intent.putExtra(CreateNoteActivity.EXTRA_NOTE_ID, noteId);
            startActivity(intent);
        });

        // 删除按钮点击事件
        binding.buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        // 分享按钮点击事件
        binding.buttonShare.setOnClickListener(v -> shareNote());

        // 收藏按钮点击事件
        binding.buttonFavorite.setOnClickListener(v -> {
            // TODO: 实现收藏功能
            Toast.makeText(this, "收藏功能开发中", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这个笔记吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteNote())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteNote() {
        noteViewModel.getNoteById(noteId).observe(this, note -> {
            if (note != null) {
                noteViewModel.delete(note);
                Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void shareNote() {
        noteViewModel.getNoteById(noteId).observe(this, note -> {
            if (note != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT, note.getTitle() + "\n\n" + note.getContent());
                startActivity(Intent.createChooser(shareIntent, "分享笔记"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 页面恢复时重新加载笔记，以获取可能的更新
        loadNote();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onPreviewClick(Attachment attachment) {
        // 处理附件预览
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(attachment.getUri(), attachment.getMimeType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开此类型的文件", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRemoveClick(Attachment attachment, int position) {
        // 在只读模式下不允许删除附件
        Toast.makeText(this, "请在编辑模式下删除附件", Toast.LENGTH_SHORT).show();
    }
} 
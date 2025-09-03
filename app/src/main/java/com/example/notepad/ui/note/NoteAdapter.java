package com.example.notepad.ui.note;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notepad.R;
import com.example.notepad.data.model.Note;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private OnNoteClickListener listener;
    private OnNoteDeleteListener deleteListener;

    public NoteAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getContent().equals(newItem.getContent()) &&
                   oldItem.getUpdatedAt().equals(newItem.getUpdatedAt());
        }
    };

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = getItem(position);
        if (note != null) {
            holder.bind(note);
        }
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setOnNoteDeleteListener(OnNoteDeleteListener listener) {
        this.deleteListener = listener;
    }

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteDeleteListener {
        void onNoteDelete(Note note);
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textContent;
        private TextView textDate;
        private ImageButton buttonDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textContent = itemView.findViewById(R.id.textContent);
            textDate = itemView.findViewById(R.id.textDate);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onNoteClick(getItem(position));
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showDeleteConfirmationDialog(getItem(position));
                }
            });
        }

        private void showDeleteConfirmationDialog(Note note) {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle(R.string.confirm_delete)
                    .setMessage(R.string.confirm_delete_message)
                    .setPositiveButton(R.string.delete_note, (dialog, which) -> {
                        if (deleteListener != null) {
                            deleteListener.onNoteDelete(note);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        public void bind(Note note) {
            textTitle.setText(note.getTitle());
            
            // 处理内容显示
            String content = note.getContent();
            // 去除图片标记、录音标记等非文本内容
            content = content.replaceAll("\\[图片:.*?\\]", "")
                             .replaceAll("\\[录音:.*?\\]", "")
                             .replaceAll("\\[附件:.*?\\]", "")
                             .trim();
            
            // 截取内容长度
            int maxLength = 120;
            if (content.length() > maxLength) {
                content = content.substring(0, maxLength) + "...";
            }
            
            textContent.setText(content);
            
            // 设置日期
            textDate.setText(dateFormat.format(note.getUpdatedAt()));
        }
    }
} 
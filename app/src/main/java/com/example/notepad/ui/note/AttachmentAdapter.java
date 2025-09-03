package com.example.notepad.ui.note;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.notepad.R;
import com.example.notepad.data.model.Attachment;
import com.example.notepad.databinding.ItemAttachmentBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * 附件列表适配器
 */
public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {
    
    private static final String TAG = "AttachmentAdapter";
    private final List<Attachment> attachments = new ArrayList<>();
    private final Context context;
    private final AttachmentClickListener listener;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // 缩略图尺寸
    private static final int THUMBNAIL_SIZE = 300;
    
    // 附件点击监听器接口
    public interface AttachmentClickListener {
        void onPreviewClick(Attachment attachment);
        void onRemoveClick(Attachment attachment, int position);
    }
    
    /**
     * 构造函数
     * @param context 上下文
     * @param listener 附件点击监听器
     */
    public AttachmentAdapter(Context context, AttachmentClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttachmentBinding binding = ItemAttachmentBinding.inflate(
            LayoutInflater.from(context), parent, false);
        return new AttachmentViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        Attachment attachment = attachments.get(position);
        
        // 设置附件名称和信息
        holder.binding.textAttachmentName.setText(attachment.getName());
        holder.binding.textAttachmentInfo.setText(
            String.format("%s • %s", 
                attachment.getTypeDescription(), 
                attachment.getFormattedSize())
        );
        
        // 设置图标和缩略图
        setupAttachmentIcon(holder.binding.imageAttachmentType, attachment);
        
        // 设置点击事件
        holder.binding.buttonPreviewAttachment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreviewClick(attachment);
            }
        });
        
        holder.binding.buttonRemoveAttachment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(attachment, holder.getAdapterPosition());
            }
        });
    }
    
    private void setupAttachmentIcon(ImageView imageView, Attachment attachment) {
        // 设置默认样式
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setPadding(24, 24, 24, 24);
        
        switch (attachment.getType()) {
            case Attachment.TYPE_IMAGE:
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(0, 0, 0, 0);
                loadImageThumbnailWithGlide(imageView, attachment);
                break;
                
            case Attachment.TYPE_AUDIO:
                imageView.setImageResource(R.drawable.ic_mic);
                break;
                
            case Attachment.TYPE_FILE:
            default:
                imageView.setImageResource(R.drawable.ic_attach);
                break;
        }
    }
    
    private void loadImageThumbnailWithGlide(ImageView imageView, Attachment attachment) {
        try {
            // 先设置占位图
            imageView.setImageResource(R.drawable.ic_image);
            
            // 检查文件是否存在
            String filePath = attachment.getPath();
            File imageFile = new File(filePath);
            
            if (imageFile.exists()) {
                // 使用FileProvider创建URI
                Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext().getPackageName() + ".fileprovider",
                    imageFile
                );
                
                Log.d(TAG, "加载图片URI: " + fileUri);
                
                // 使用Glide加载图片
                Glide.with(context)
                    .load(fileUri)
                    .apply(new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                        .error(R.drawable.ic_image))
                    .into(imageView);
                
            } else {
                Log.e(TAG, "图片文件不存在: " + filePath);
                imageView.setImageResource(R.drawable.ic_image);
            }
        } catch (Exception e) {
            Log.e(TAG, "加载图片缩略图失败", e);
            imageView.setImageResource(R.drawable.ic_image);
        }
    }
    
    /**
     * 原始加载图片方法 (作为备份)
     */
    private void loadImageThumbnail(ImageView imageView, Attachment attachment) {
        imageView.setImageResource(R.drawable.ic_image);
        
        executorService.execute(() -> {
            try {
                Bitmap thumbnail = generateImageThumbnail(attachment.getPath());
                if (thumbnail != null) {
                    mainHandler.post(() -> imageView.setImageBitmap(thumbnail));
                }
            } catch (Exception e) {
                Log.e(TAG, "加载缩略图失败: " + e.getMessage());
            }
        });
    }
    
    private Bitmap generateImageThumbnail(String imagePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            
            int scale = Math.max(options.outWidth / THUMBNAIL_SIZE, 
                               options.outHeight / THUMBNAIL_SIZE);
            
            if (scale <= 0) scale = 1;
            
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            
            if (bitmap == null) return null;
            
            return ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            
        } catch (Exception e) {
            Log.e(TAG, "生成缩略图失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public int getItemCount() {
        return attachments.size();
    }
    
    /**
     * 添加新附件
     * @param attachment 附件对象
     */
    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        notifyItemInserted(attachments.size() - 1);
    }
    
    /**
     * 移除附件
     * @param position 附件位置
     */
    public void removeAttachment(int position) {
        if (position >= 0 && position < attachments.size()) {
            attachments.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    /**
     * 获取所有附件
     * @return 附件列表
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<Attachment> newAttachments) {
        attachments.clear();
        if (newAttachments != null) {
            attachments.addAll(newAttachments);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 附件ViewHolder
     */
    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttachmentBinding binding;
        
        public AttachmentViewHolder(@NonNull ItemAttachmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
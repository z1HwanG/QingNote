package com.example.notepad.ui.note;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notepad.R;
import com.example.notepad.data.model.Attachment;
import com.example.notepad.data.model.Note;
import com.example.notepad.data.callback.InsertCallback;
import com.example.notepad.databinding.ActivityCreateNoteBinding;
import com.example.notepad.utils.FileUtils;
import com.example.notepad.utils.SessionManager;
import com.example.notepad.utils.PermissionUtils;
import com.example.notepad.ui.base.BaseActivity;
import com.example.notepad.utils.MediaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class CreateNoteActivity extends BaseActivity implements AttachmentAdapter.AttachmentClickListener {
    private ActivityCreateNoteBinding binding;
    private NoteViewModel noteViewModel;
    private SessionManager sessionManager;
    private Note noteToEdit;
    private boolean isEditMode = false;
    private Uri currentPhotoUri;
    private Uri currentAudioUri;
    private AttachmentAdapter attachmentAdapter;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    public static final String EXTRA_NOTE_ID = "extra_note_id";

    // 功能标识常量
    private static final int FEATURE_CAMERA = 1;
    private static final int FEATURE_GALLERY = 2;
    private static final int FEATURE_AUDIO = 3;
    private static final int FEATURE_FILE = 4;
    
    // 记录最后请求的功能
    private int lastRequestedFeature = 0;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int MAX_DURATION = 60000; // 最大录音时长60秒

    private static final int PICK_FILE_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 102;

    // 定义录音最短持续时间（毫秒）
    private static final int MIN_RECORDING_DURATION = 3000; // 至少录音3秒
    private long recordingStartTime;

    // 活动结果启动器
    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) {
                handleImageCapture(currentPhotoUri);
            }
        }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {
            if (isGranted) {
                // 权限被授予，根据lastRequestedFeature执行对应操作
                switch (lastRequestedFeature) {
                    case FEATURE_CAMERA:
                        launchCamera();
                        break;
                    case FEATURE_GALLERY:
                        openImagePicker();
                        break;
                    case FEATURE_AUDIO:
                        startVoiceRecording();
                        break;
                    case FEATURE_FILE:
                        openFilePicker();
                        break;
                }
            } else {
                Toast.makeText(this, "需要相应权限才能使用此功能", Toast.LENGTH_SHORT).show();
            }
        }
    );

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化ViewModel和SessionManager
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        sessionManager = new SessionManager(this);

        // 初始化附件适配器和RecyclerView
        attachmentAdapter = new AttachmentAdapter(this, this);
        binding.recyclerAttachments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAttachments.setAdapter(attachmentAdapter);

        // 检查是否为编辑模式
        if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
            isEditMode = true;
            long noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
            if (noteId != -1) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.edit_note);
                }
                loadNote(noteId);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.new_note);
            }
        }

        // 设置保存按钮点击事件
        binding.buttonSave.setOnClickListener(v -> saveNote());

        // 初始化时检查所有权限
        checkInitialPermissions();
        
        setupClickListeners();

        // 初始化图片选择器
        imagePickerLauncher = MediaUtils.registerImagePicker(this, uri -> {
            try {
                Log.d("CreateNoteActivity", "图片选择处理 - URI: " + uri);
                
                // 获取永久URI权限
                MediaUtils.takePersistableUriPermission(this, uri);
                
                // 处理选中的图片
                handleSelectedImage(uri);
            } catch (Exception e) {
                Log.e("CreateNoteActivity", "处理图片失败", e);
                Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNote(long noteId) {
        noteViewModel.getNoteById(noteId).observe(this, note -> {
            if (note != null) {
                noteToEdit = note;
                binding.editNoteTitle.setText(note.getTitle());
                binding.editNoteContent.setText(note.getContent());
                
                // 解析内容中的附件标记并恢复附件列表
                parseAttachmentsFromContent(note.getContent());
            } else {
                Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * 从笔记内容中解析附件信息并恢复附件列表
     * @param content 笔记内容
     */
    private void parseAttachmentsFromContent(String content) {
        if (noteToEdit == null || noteToEdit.getId() <= 0) {
            return;
        }
        
        Log.d("CreateNoteActivity", "开始加载笔记附件");
        
        try {
            // 清空当前附件列表
            if (attachmentAdapter.getItemCount() > 0) {
                int count = attachmentAdapter.getItemCount();
                for (int i = count - 1; i >= 0; i--) {
                    attachmentAdapter.removeAttachment(i);
                }
            }
            
            // 加载笔记ID关联的附件
            loadAttachmentsForNote(noteToEdit.getId());
            
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "加载笔记附件失败", e);
        }
    }
    
    /**
     * 加载笔记关联的附件
     * @param noteId 笔记ID
     */
    private void loadAttachmentsForNote(long noteId) {
        // 获取应用内存储目录
        File attachmentsDir = new File(getFilesDir(), "attachments/" + noteId);
        if (!attachmentsDir.exists()) {
            Log.d("CreateNoteActivity", "附件目录不存在: " + attachmentsDir.getAbsolutePath());
            return;
        }
        
        // 遍历目录下的所有文件
        File[] files = attachmentsDir.listFiles();
        if (files == null || files.length == 0) {
            Log.d("CreateNoteActivity", "没有找到附件文件");
            return;
        }
        
        Log.d("CreateNoteActivity", "找到 " + files.length + " 个附件文件");
        
        // 处理每个附件文件
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                String filePath = file.getAbsolutePath();
                long fileSize = file.length();
                
                // 根据文件扩展名判断类型
                int attachmentType = determineAttachmentType(fileName);
                
                // 添加到附件列表
                switch (attachmentType) {
                    case Attachment.TYPE_IMAGE:
                        addImageAttachment(fileName, filePath, fileSize);
                        break;
                    case Attachment.TYPE_AUDIO:
                        addAudioAttachment(fileName, filePath, fileSize);
                        break;
                    default:
                        addFileAttachment(fileName, filePath, fileSize);
                        break;
                }
            }
        }
        
        // 更新UI
        updateAttachmentsVisibility();
    }
    
    /**
     * 根据文件名确定附件类型
     * @param fileName 文件名
     * @return 附件类型
     */
    private int determineAttachmentType(String fileName) {
        fileName = fileName.toLowerCase(Locale.ROOT);
        
        // 图片类型
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
            return Attachment.TYPE_IMAGE;
        }
        
        // 音频类型
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
            fileName.endsWith(".ogg") || fileName.endsWith(".m4a") || 
            fileName.endsWith(".aac") || fileName.endsWith(".flac")) {
            return Attachment.TYPE_AUDIO;
        }
        
        // 默认为文件类型
        return Attachment.TYPE_FILE;
    }

    private void setupClickListeners() {
        // 已移除返回按钮，不再需要设置点击事件
        // 改用toolbar的导航图标作为返回按钮

        // 设置各功能按钮
        setupCameraButton();
        setupGalleryButton();
        setupMicrophoneButton();
        setupAttachButton();

        // 更多按钮点击事件
        binding.buttonMore.setOnClickListener(v -> showMoreOptions());
    }

    private void setupCameraButton() {
        binding.buttonCamera.setOnClickListener(v -> {
            lastRequestedFeature = FEATURE_CAMERA;
            if (PermissionUtils.hasCameraPermission(this)) {
                launchCamera();
            } else {
                PermissionUtils.requestCameraPermissions(this);
            }
        });
    }
    
    private static final int REQUEST_IMAGE_CAPTURE = 103;
    
    private void launchCamera() {
        try {
            Log.d("CreateNoteActivity", "启动相机 - 开始");
            
            // 创建图片文件
            File photoFile = FileUtils.createImageFile(this);
            if (photoFile == null) {
                Log.e("CreateNoteActivity", "无法创建图片文件");
                Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 记录文件路径用于后续处理
            currentPhotoPath = photoFile.getAbsolutePath();
            Log.d("CreateNoteActivity", "图片文件路径: " + currentPhotoPath);
            
            // 获取FileProvider的URI
            String authority = getApplicationContext().getPackageName() + ".fileprovider";
            currentPhotoUri = FileProvider.getUriForFile(this, authority, photoFile);
            
            Log.d("CreateNoteActivity", "FileProvider授权: " + authority);
            Log.d("CreateNoteActivity", "拍照URI: " + currentPhotoUri);
            
            // 创建相机Intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            
            // 添加标志，使系统处理权限
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
            // 启动相机
            takePictureLauncher.launch(takePictureIntent);
            
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "启动相机失败: " + e.getMessage(), e);
            Toast.makeText(this, "启动相机失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查相机应用是否可用
     */
    private boolean isCameraAvailable() {
        try {
            // 方法1: 使用PackageManager检查相机应用
            PackageManager packageManager = getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Log.d("CreateNoteActivity", "设备不支持任何相机");
                return false;
            }
            
            // 方法2: 检查Intent是否可以被处理
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
            
            if (resolveInfoList.isEmpty()) {
                Log.d("CreateNoteActivity", "没有应用可以处理相机Intent");
                return false;
            }
            
            // 输出所有可以处理相机Intent的应用
            Log.d("CreateNoteActivity", "可以处理相机Intent的应用: ");
            for (ResolveInfo info : resolveInfoList) {
                Log.d("CreateNoteActivity", " - " + info.activityInfo.packageName);
            }
            
            return true;
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "检查相机可用性失败", e);
            return false;
        }
    }
    
    /**
     * 使用系统默认相机应用直接拍照（备用方法）
     */
    private void takeDirectPicture() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "直接拍照失败", e);
            Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String currentPhotoPath;
    
    private void handleImageCapture(Uri photoUri) {
        Log.d("CreateNoteActivity", "处理拍照结果 - URI: " + photoUri);
        
        if (photoUri != null) {
            try {
                Log.d("CreateNoteActivity", "当前存储的路径: " + currentPhotoPath);
                
                // 使用文件路径
                if (currentPhotoPath != null && new File(currentPhotoPath).exists()) {
                    File photoFile = new File(currentPhotoPath);
                    Log.d("CreateNoteActivity", "文件存在: " + photoFile.length() + " 字节");
                    
                    // 通知图库扫描新图片
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(photoFile);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);
                    Log.d("CreateNoteActivity", "已发送媒体扫描广播");
                    
                    // 添加图片到附件列表
                    addImageAttachment(photoFile.getName(), currentPhotoPath, photoFile.length());
                } else {
                    Log.d("CreateNoteActivity", "尝试从URI获取路径");
                    // 尝试通过URI获取路径
                    String imagePath = FileUtils.getPathFromUri(this, photoUri);
                    Log.d("CreateNoteActivity", "从URI获取的路径: " + imagePath);
                    
                    if (imagePath != null) {
                        File file = new File(imagePath);
                        addImageAttachment(file.getName(), imagePath, file.length());
                    } else {
                        Log.e("CreateNoteActivity", "无法获取图片路径");
                        // 直接使用URI创建临时文件
                        try {
                            File tempFile = createTempFileFromUri(photoUri);
                            if (tempFile != null && tempFile.exists()) {
                                Log.d("CreateNoteActivity", "创建临时文件成功: " + tempFile.getAbsolutePath());
                                addImageAttachment(tempFile.getName(), tempFile.getAbsolutePath(), tempFile.length());
                            } else {
                                Toast.makeText(this, "无法获取图片路径", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("CreateNoteActivity", "创建临时文件失败", e);
                            Toast.makeText(this, "无法获取图片路径", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("CreateNoteActivity", "处理图片失败", e);
                Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("CreateNoteActivity", "未获取到图片URI");
            Toast.makeText(this, "未获取到图片", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }
        
        String fileName = "temp_" + System.currentTimeMillis() + ".jpg";
        File outputFile = new File(getCacheDir(), fileName);
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        
        inputStream.close();
        return outputFile;
    }

    private void setupGalleryButton() {
        binding.buttonGallery.setOnClickListener(v -> {
            lastRequestedFeature = FEATURE_GALLERY;
            if (PermissionUtils.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                PermissionUtils.requestStoragePermissions(this);
            }
        });
    }

    private void openImagePicker() {
        try {
            Log.d("CreateNoteActivity", "打开图片选择器");
            Intent intent = MediaUtils.createImagePickerIntent();
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "无法打开图片选择器", e);
            Toast.makeText(this, "无法打开图片选择器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            Log.d("CreateNoteActivity", "处理选中的图片: " + imageUri);
            
            // 获取文件扩展名
            String extension = "jpg";
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension == null) extension = "jpg";
            }
            
            // 创建附件目录（如果不存在）
            File attachmentDir = new File(getFilesDir(), "attachments");
            if (!attachmentDir.exists()) {
                attachmentDir.mkdirs();
            }

            // 创建目标文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "IMG_" + timeStamp + "." + extension;
            File destinationFile = new File(attachmentDir, imageFileName);

            // 保存文件
            if (MediaUtils.saveUriToFile(this, imageUri, destinationFile)) {
                Log.d("CreateNoteActivity", "图片已保存到: " + destinationFile.getAbsolutePath());
                // 添加图片到附件列表
                addImageAttachment(destinationFile.getName(), destinationFile.getAbsolutePath(), destinationFile.length());
            } else {
                Log.e("CreateNoteActivity", "保存图片失败");
                Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "处理图片失败", e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceRecording() {
        try {
            // 如果已经在录音，先停止
            if (isRecording) {
                stopVoiceRecording();
                return;
            }
            
            Log.d("CreateNoteActivity", "开始录音");
            
            // 确保录音目录存在
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (audioDir != null && !audioDir.exists()) {
                boolean dirCreated = audioDir.mkdirs();
                Log.d("CreateNoteActivity", "创建录音目录: " + dirCreated);
            }
            
            // 创建录音文件
            audioFile = FileUtils.createAudioFile(this);
            if (audioFile == null) {
                Log.e("CreateNoteActivity", "无法创建录音文件");
                Toast.makeText(this, "无法创建录音文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d("CreateNoteActivity", "录音文件路径: " + audioFile.getAbsolutePath());

            // 配置MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setMaxDuration(MAX_DURATION);
            
            // 准备录音
            mediaRecorder.prepare();
            // 开始录音
            mediaRecorder.start();
            recordingStartTime = System.currentTimeMillis();
            isRecording = true;
            
            // 更新UI显示录音状态
            binding.buttonMicrophone.setColorFilter(
                getResources().getColor(android.R.color.holo_red_light), 
                PorterDuff.Mode.SRC_IN
            );
            Toast.makeText(this, "开始录音...点击麦克风图标停止", Toast.LENGTH_SHORT).show();

            // 设置最大录音时长
            handler.postDelayed(this::stopVoiceRecording, MAX_DURATION);

        } catch (Exception e) {
            Log.e("CreateNoteActivity", "录音失败: " + e.getMessage(), e);
            resetRecorder();
            Toast.makeText(this, "录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceRecording() {
        if (!isRecording || mediaRecorder == null) {
            Log.d("CreateNoteActivity", "没有正在进行的录音");
            return;
        }
        
        Log.d("CreateNoteActivity", "停止录音");
        
        // 检查录音时长
        long duration = System.currentTimeMillis() - recordingStartTime;
        Log.d("CreateNoteActivity", "录音持续时间: " + duration + "ms");
        
        try {
            // 如果录音时间太短，提示用户并取消录音
            if (duration < MIN_RECORDING_DURATION) {
                Log.d("CreateNoteActivity", "录音时间太短，取消录音");
                Toast.makeText(this, "录音时间太短，请至少录音" + (MIN_RECORDING_DURATION/1000) + "秒", Toast.LENGTH_SHORT).show();
                
                // 停止和释放录音器
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Log.e("CreateNoteActivity", "停止短录音失败", e);
                }
                
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                
                // 恢复UI状态
                binding.buttonMicrophone.clearColorFilter();
                
                // 删除太短的录音文件
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
                
                return;
            }
            
            // 停止录音
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            
            // 恢复UI状态
            binding.buttonMicrophone.clearColorFilter();
            
            // 处理录音文件
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                Log.d("CreateNoteActivity", "录音文件大小: " + audioFile.length() + " 字节");
                insertAudioToNote(audioFile.getAbsolutePath());
            } else {
                Log.e("CreateNoteActivity", "录音文件不存在或为空");
                Toast.makeText(this, "录音文件保存失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "停止录音失败: " + e.getMessage(), e);
            resetRecorder();
            Toast.makeText(this, "保存录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        // 移除回调
        handler.removeCallbacksAndMessages(null);
    }
    
    private void resetRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ex) {
                Log.e("CreateNoteActivity", "释放MediaRecorder失败", ex);
            }
            mediaRecorder = null;
        }
        isRecording = false;
        binding.buttonMicrophone.clearColorFilter();
    }

    private void insertAudioToNote(String audioPath) {
        Log.d("CreateNoteActivity", "插入音频到笔记: " + audioPath);
        
        File audioFile = new File(audioPath);
        if (audioFile.exists()) {
            // 添加音频到附件列表
            addAudioAttachment(audioFile.getName(), audioPath, audioFile.length());
            
            // 通知媒体库扫描新文件
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(audioFile);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
            
            Toast.makeText(this, "录音已添加到笔记", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择文件"),
                PICK_FILE_REQUEST_CODE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreOptions() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, binding.buttonMore);
        popup.getMenuInflater().inflate(R.menu.menu_note_more, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_clear) {
                clearNote();
                return true;
            } else if (id == R.id.menu_font_size) {
                showFontSizeDialog();
                return true;
            }
//            else if (id == R.id.menu_text_color) {
//                showTextColorDialog();
//                return true;
//            }
            return false;

        });
        
        popup.show();
    }

    private void clearNote() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清空笔记")
            .setMessage("确定要清空当前笔记吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                binding.editNoteTitle.setText("");
                binding.editNoteContent.setText("");
                Toast.makeText(this, "笔记已清空", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showFontSizeDialog() {
        final String[] sizes = {"小", "中", "大"};
        final float[] textSizes = {10f, 15f, 25f};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择字体大小")
            .setItems(sizes, (dialog, which) -> {
                binding.editNoteContent.setTextSize(textSizes[which]);
                Toast.makeText(this, "字体大小已更改", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

//    private void showTextColorDialog() {
//        final String[] colors = {"黑色", "蓝色", "红色", "绿色"};
//        final int[] colorValues = {
//            android.graphics.Color.BLACK,
//            android.graphics.Color.BLUE,
//            android.graphics.Color.RED,
//            android.graphics.Color.GREEN
//        };
//
//        new androidx.appcompat.app.AlertDialog.Builder(this)
//            .setTitle("选择文字颜色")
//            .setItems(colors, (dialog, which) -> {
//                binding.editNoteContent.setTextColor(colorValues[which]);
//                Toast.makeText(this, "文字颜色已更改", Toast.LENGTH_SHORT).show();
//            })
//            .show();
//    }

    private void handleFileSelection(Uri fileUri) {
        try {
            String fileName = FileUtils.getFileName(this, fileUri);
            String filePath = FileUtils.getPathFromUri(this, fileUri);
            long fileSize = FileUtils.getFileSize(this, fileUri);

            if (filePath != null) {
                // 将文件复制到应用私有目录
                File destFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
                FileUtils.copyFile(this, fileUri, destFile);
                addFileAttachment(fileName, destFile.getAbsolutePath(), destFile.length());
            } else {
                // 尝试复制文件到应用私有目录
                File destFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
                FileUtils.copyFile(this, fileUri, destFile);
                
                if (destFile.exists()) {
                    addFileAttachment(fileName, destFile.getAbsolutePath(), destFile.length());
                } else {
                    Toast.makeText(this, "无法获取文件路径", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "处理文件选择失败", e);
            Toast.makeText(this, "文件处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        boolean allGranted = true;
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
        } else {
            allGranted = false;
        }
        
        switch (requestCode) {
            case PermissionUtils.REQUEST_CAMERA_PERMISSIONS:
                if (allGranted) {
                    launchCamera();
                } else {
                    showPermissionRequiredDialog("相机");
                }
                break;
                
            case PermissionUtils.REQUEST_STORAGE_PERMISSIONS:
                if (allGranted) {
                    // 根据最后请求的操作执行相应功能
                    if (lastRequestedFeature == FEATURE_GALLERY) {
                        openImagePicker();
                    } else if (lastRequestedFeature == FEATURE_FILE) {
                        openFilePicker();
                    }
                } else {
                    showPermissionRequiredDialog("存储");
                }
                break;
                
            case PermissionUtils.REQUEST_AUDIO_PERMISSIONS:
                if (allGranted) {
                    startVoiceRecording();
                } else {
                    showPermissionRequiredDialog("录音");
                }
                break;
                
            case PermissionUtils.REQUEST_ALL_PERMISSIONS:
                // 权限请求后无特定操作
                if (!allGranted) {
                    Toast.makeText(this, "部分权限未授予，可能会影响应用功能", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    
    // 添加权限说明对话框
    private void showPermissionRequiredDialog(String permissionType) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("需要" + permissionType + "权限")
            .setMessage("此功能需要" + permissionType + "权限才能正常工作。请在设置中启用权限。")
            .setPositiveButton("去设置", (dialog, which) -> {
                PermissionUtils.openAppSettings(this);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 添加初始权限检查方法
    private void checkInitialPermissions() {
        // 检查应用所需的基本权限
        if (!PermissionUtils.hasCameraPermission(this) || 
            !PermissionUtils.hasStoragePermission(this) || 
            !PermissionUtils.hasAudioPermission(this)) {
            
            // 显示权限说明
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("需要权限")
                .setMessage("此应用需要相机、存储和录音权限才能正常工作。")
                .setPositiveButton("授予权限", (dialog, which) -> {
                    PermissionUtils.requestAllPermissions(this);
                })
                .setNegativeButton("暂不", null)
                .show();
        }
    }

    // 更新麦克风按钮点击事件处理
    private void setupMicrophoneButton() {
        // 改用点击事件代替触摸事件
        binding.buttonMicrophone.setOnClickListener(v -> {
            try {
                lastRequestedFeature = FEATURE_AUDIO;
                if (PermissionUtils.hasAudioPermission(this)) {
                    startVoiceRecording();
                } else {
                    PermissionUtils.requestAudioPermission(this);
                }
            } catch (Exception e) {
                Log.e("CreateNoteActivity", "处理麦克风事件失败", e);
                resetRecorder();
                Toast.makeText(this, "录音操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 更新附件按钮点击事件处理
    private void setupAttachButton() {
        binding.buttonAttach.setOnClickListener(v -> {
            lastRequestedFeature = FEATURE_FILE;
            if (PermissionUtils.hasStoragePermission(this)) {
                openFilePicker();
            } else {
                PermissionUtils.requestStoragePermissions(this);
            }
        });
    }

    /**
     * 添加图片附件
     */
    private void addImageAttachment(String name, String path, long size) {
        Attachment attachment = new Attachment(Attachment.TYPE_IMAGE, name, path, size);
        attachmentAdapter.addAttachment(attachment);
        updateAttachmentsVisibility();
        insertAttachmentMarkerToNote(attachment);
    }
    
    /**
     * 添加音频附件
     */
    private void addAudioAttachment(String name, String path, long size) {
        Attachment attachment = new Attachment(Attachment.TYPE_AUDIO, name, path, size);
        attachmentAdapter.addAttachment(attachment);
        updateAttachmentsVisibility();
        insertAttachmentMarkerToNote(attachment);
    }
    
    /**
     * 添加文件附件
     */
    private void addFileAttachment(String name, String path, long size) {
        Attachment attachment = new Attachment(Attachment.TYPE_FILE, name, path, size);
        attachmentAdapter.addAttachment(attachment);
        updateAttachmentsVisibility();
        insertAttachmentMarkerToNote(attachment);
    }
    
    /**
     * 插入附件标记到笔记内容
     */
    private void insertAttachmentMarkerToNote(Attachment attachment) {
        // 不再将附件标记插入到笔记内容中
        // 仅更新UI显示
        updateAttachmentsVisibility();
    }
    
    /**
     * 更新附件列表可见性
     */
    private void updateAttachmentsVisibility() {
        if (attachmentAdapter.getItemCount() > 0) {
            binding.textAttachmentsTitle.setVisibility(View.VISIBLE);
            binding.recyclerAttachments.setVisibility(View.VISIBLE);
        } else {
            binding.textAttachmentsTitle.setVisibility(View.GONE);
            binding.recyclerAttachments.setVisibility(View.GONE);
        }
    }
    
    // AttachmentClickListener 接口方法
    @Override
    public void onPreviewClick(Attachment attachment) {
        try {
            if (attachment.exists()) {
                // 根据附件类型打开不同的预览
                switch (attachment.getType()) {
                    case Attachment.TYPE_IMAGE:
                        openImagePreview(attachment);
                        break;
                    case Attachment.TYPE_AUDIO:
                        playAudio(attachment);
                        break;
                    case Attachment.TYPE_FILE:
                        openFile(attachment);
                        break;
                }
            } else {
                Toast.makeText(this, "附件文件不存在", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "预览附件失败", e);
            Toast.makeText(this, "无法预览附件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRemoveClick(Attachment attachment, int position) {
        // 删除物理文件
        try {
            File attachmentFile = new File(attachment.getPath());
            if (attachmentFile.exists()) {
                attachmentFile.delete();
            }
            
            // 从数据库中删除附件记录
            if (noteToEdit != null && noteToEdit.getId() > 0) {
                noteViewModel.deleteAttachment(attachment);
            }
            
            // 从适配器中移除附件
            attachmentAdapter.removeAttachment(position);
            
            // 更新UI
            updateAttachmentsVisibility();
            
            Toast.makeText(this, "附件已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "删除附件失败: " + e.getMessage());
            Toast.makeText(this, "删除附件失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开图片预览
     */
    private void openImagePreview(Attachment attachment) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri photoURI = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(attachment.getPath())
            );
            intent.setDataAndType(photoURI, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "打开图片预览失败", e);
            Toast.makeText(this, "无法打开图片预览", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 播放音频
     */
    private void playAudio(Attachment attachment) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri audioURI = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(attachment.getPath())
            );
            intent.setDataAndType(audioURI, "audio/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "播放音频失败", e);
            Toast.makeText(this, "无法播放音频", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开文件
     */
    private void openFile(Attachment attachment) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileURI = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(attachment.getPath())
            );
            
            // 尝试根据文件扩展名确定MIME类型
            String mimeType = FileUtils.getMimeType(attachment.getPath());
            if (mimeType != null) {
                intent.setDataAndType(fileURI, mimeType);
            } else {
                intent.setDataAndType(fileURI, "*/*");
            }
            
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "打开文件失败", e);
            Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectImage() {
        try {
            // 使用 ACTION_OPEN_DOCUMENT 打开图片选择器
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "打开图片选择器失败", e);
            Toast.makeText(this, "无法打开图片选择器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote() {
        String title = binding.editNoteTitle.getText().toString().trim();
        String content = binding.editNoteContent.getText().toString().trim();
        
        if (TextUtils.isEmpty(title)) {
            binding.editNoteTitle.setError("请输入标题");
            return;
        }
        
        // 获取当前用户ID
        int userId = sessionManager.getCurrentUser().getId();
        
        if (isEditMode && noteToEdit != null) {
            // 更新现有笔记
            noteToEdit.setTitle(title);
            noteToEdit.setContent(content);
            noteToEdit.setUpdatedAt(new Date());
            noteViewModel.update(noteToEdit);
            saveAttachments(noteToEdit.getId());
            Toast.makeText(this, "笔记已更新", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // 创建新笔记 - 使用带参数的构造函数
            Note newNote = new Note(title, content, userId);
            
            // 插入新笔记
            noteViewModel.insert(newNote, new InsertCallback() {
                @Override
                public void onComplete(Note insertedNote) {
                    runOnUiThread(() -> {
                        saveAttachments(insertedNote.getId());
                        Toast.makeText(CreateNoteActivity.this, "笔记已保存", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateNoteActivity.this, "保存笔记失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
    
    /**
     * 保存附件到笔记专属目录
     * @param noteId 笔记ID
     */
    private void saveAttachments(long noteId) {
        if (noteId <= 0 || attachmentAdapter.getItemCount() == 0) {
            return;
        }
        
        try {
            // 创建笔记专属的附件目录
            File attachmentsDir = new File(getFilesDir(), "attachments/" + noteId);
            if (!attachmentsDir.exists()) {
                boolean created = attachmentsDir.mkdirs();
                if (!created) {
                    Log.e("CreateNoteActivity", "无法创建附件目录: " + attachmentsDir.getAbsolutePath());
                    return;
                }
            }
            
            // 获取当前所有附件
            List<Attachment> attachments = attachmentAdapter.getAttachments();
            
            // 对每个附件，复制到笔记专属目录
            for (Attachment attachment : attachments) {
                File sourceFile = new File(attachment.getPath());
                
                // 确保源文件存在
                if (!sourceFile.exists()) {
                    continue;
                }
                
                // 创建目标文件
                File destFile = new File(attachmentsDir, sourceFile.getName());
                
                // 如果目标文件已经存在且是同一个文件，则跳过
                if (destFile.exists() && destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
                    continue;
                }
                
                // 复制文件
                try {
                    FileUtils.copyFile(sourceFile, destFile);
                    Log.d("CreateNoteActivity", "已保存附件: " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e("CreateNoteActivity", "保存附件失败: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e("CreateNoteActivity", "保存附件过程中出错", e);
        }
    }

    @Override
    protected void onDestroy() {
        // 清理URI权限
        if (currentPhotoUri != null) {
            revokeUriPermission(currentPhotoUri, 
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (isRecording) {
            stopVoiceRecording();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
        binding = null;
    }

    // 添加处理Intent结果的方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 处理拍照返回结果
            handleImageCapture(currentPhotoUri);
        } else if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                handleFileSelection(data.getData());
            }
        }
    }
} 
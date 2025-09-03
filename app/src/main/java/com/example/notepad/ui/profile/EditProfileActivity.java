package com.example.notepad.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.Priority;
import com.example.notepad.R;
import com.example.notepad.databinding.ActivityEditProfileBinding;
import com.example.notepad.data.model.User;
import com.example.notepad.utils.PermissionUtils;
import com.example.notepad.utils.SessionManager;
import com.example.notepad.utils.MediaUtils;
import com.example.notepad.utils.FileUtils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";

    private ActivityEditProfileBinding binding;
    private EditProfileViewModel viewModel;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private File currentImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel和SessionManager
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        sessionManager = new SessionManager(this);

        // 设置图片选择器
        setupImagePicker();
        
        // 设置点击事件
        setupClickListeners();
        
        // 加载当前用户数据
        loadUserData();
        
        // 观察更新结果
        observeUpdateResult();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        Log.d(TAG, "选择的图片URI: " + imageUri);
                        try {
                            // 如果是外部内容URI，获取持久化权限
                            if (MediaUtils.isExternalContentUri(imageUri)) {
                                try {
                                    MediaUtils.takePersistableUriPermission(this, imageUri);
                                    Log.d(TAG, "已获取持久化URI权限");
                                } catch (Exception e) {
                                    Log.w(TAG, "获取持久化权限失败，继续处理", e);
                                }
                            }
                            
                            // 将外部图片复制到应用内部存储
                            currentImageFile = createImageFile();
                            if (MediaUtils.saveUriToFile(this, imageUri, currentImageFile)) {
                                Log.d(TAG, "已保存图片到内部存储: " + currentImageFile.getAbsolutePath());
                                
                                // 创建应用内部文件的URI - 这是FileProvider URI，不需要获取持久化权限
                                Uri internalUri = FileProvider.getUriForFile(
                                    this, 
                                    getApplicationContext().getPackageName() + ".fileprovider",
                                    currentImageFile
                                );
                                
                                Log.d(TAG, "创建内部FileProvider URI: " + internalUri);
                                
                                // 对FileProvider URI不尝试获取持久化权限，直接使用
                                // 更新头像预览
                                Glide.with(this)
                                    .load(internalUri)
                                    .apply(getGlideRequestOptions())
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .listener(createGlideListener())
                                    .into(binding.imageAvatar);
                                
                                // 保存头像URI到ViewModel
                                viewModel.setSelectedImageUri(internalUri);
                            } else {
                                Log.w(TAG, "无法保存图片到内部存储，尝试直接使用原始URI");
                                // 如果复制失败，尝试直接使用原始URI
                                loadImage(imageUri);
                                viewModel.setSelectedImageUri(imageUri);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理选择的图片失败", e);
                            Toast.makeText(this, "处理图片失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }

    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "AVATAR_" + timeStamp + "_";
        
        // 创建应用内部私有文件目录
        File storageDir = new File(getFilesDir(), "avatars");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        // 创建图片文件
        return new File(storageDir, imageFileName + ".jpg");
    }

    private void loadImage(Uri uri) {
        Log.d(TAG, "开始加载图片: " + uri);
        try {
            // 使用统一的Glide配置加载图片
            RequestOptions options = getGlideRequestOptions();
            
            // 如果是外部内容URI，尝试获取持久化权限
            if (MediaUtils.isExternalContentUri(uri)) {
                try {
                    MediaUtils.takePersistableUriPermission(this, uri);
                } catch (Exception e) {
                    Log.w(TAG, "获取持久化权限失败", e);
                }
            } else if (uri.getAuthority() != null && uri.getAuthority().contains("fileprovider")) {
                // FileProvider URI 不需要持久化权限
                Log.d(TAG, "内部FileProvider URI无需获取持久化权限: " + uri);
            }
            
            // 使用统一的方式加载图片
            Glide.with(this)
                .load(uri)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(createGlideListener())
                .into(binding.imageAvatar);
                
        } catch (Exception e) {
            Log.e(TAG, "加载图片失败: " + e.getMessage(), e);
            binding.imageAvatar.setImageResource(R.drawable.ic_person);
        }
    }
    
    private RequestOptions getGlideRequestOptions() {
        return new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(Priority.HIGH)
            .circleCrop()
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .format(DecodeFormat.PREFER_RGB_565)
            .override(300, 300);
    }
    
    private RequestListener<Drawable> createGlideListener() {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                    Target<Drawable> target, boolean isFirstResource) {
                Log.e(TAG, "加载图片失败: " + (e != null ? e.getMessage() : "未知错误"));
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model,
                    Target<Drawable> target, DataSource dataSource,
                    boolean isFirstResource) {
                Log.d(TAG, "图片加载成功，来源: " + dataSource.name());
                return false;
            }
        };
    }

    private void setupClickListeners() {
        // 返回按钮
        binding.buttonBack.setOnClickListener(v -> finish());

        // 更换头像按钮
        binding.buttonChangeAvatar.setOnClickListener(v -> {
            if (PermissionUtils.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                PermissionUtils.requestStoragePermissions(this);
            }
        });

        // 点击头像也可以更换
        binding.imageAvatar.setOnClickListener(v -> {
            if (PermissionUtils.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                PermissionUtils.requestStoragePermissions(this);
            }
        });

        // 保存按钮
        binding.buttonSave.setOnClickListener(v -> saveProfile());
    }
    
    private void openImagePicker() {
        try {
            imagePickerLauncher.launch(MediaUtils.createImagePickerIntent());
        } catch (Exception e) {
            Log.e(TAG, "打开图片选择器失败: " + e.getMessage());
            Toast.makeText(this, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_STORAGE_PERMISSIONS) {
            if (PermissionUtils.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                Toast.makeText(this, "需要存储权限来选择头像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            binding.editUsername.setText(currentUser.getUsername());
            binding.editEmail.setText(currentUser.getEmail());
            binding.editFullName.setText(currentUser.getFullName());
            
            // 加载头像
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                Uri avatarUri = Uri.parse(currentUser.getAvatarUrl());
                Log.d(TAG, "加载用户头像: " + avatarUri);
                
                // 检查是否是外部内容URI还是FileProvider URI
                if (MediaUtils.isExternalContentUri(avatarUri)) {
                    // 对外部内容URI尝试获取持久化权限
                    try {
                        MediaUtils.takePersistableUriPermission(this, avatarUri);
                        Log.d(TAG, "已获取持久化URI权限");
                    } catch (Exception e) {
                        Log.w(TAG, "获取持久化权限失败，继续加载", e);
                    }
                } else {
                    Log.d(TAG, "内部URI无需获取持久化权限");
                }
                
                // 使用统一的Glide加载图片
                Glide.with(this)
                    .load(avatarUri)
                    .apply(getGlideRequestOptions())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(createGlideListener())
                    .into(binding.imageAvatar);
            } else {
                binding.imageAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

    private void saveProfile() {
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String fullName = binding.editFullName.getText().toString().trim();

        // 验证输入
        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "请填写必填项", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新用户资料
        viewModel.updateProfile(username, email, fullName);
    }

    private void observeUpdateResult() {
        viewModel.getUpdateResult().observe(this, success -> {
            if (success) {
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 
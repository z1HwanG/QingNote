package com.example.notepad.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.Priority;
import com.example.notepad.R;
import com.example.notepad.databinding.ActivityProfileBinding;
import com.example.notepad.data.model.User;
import com.example.notepad.ui.auth.LoginActivity;
import com.example.notepad.ui.auth.UserViewModel;
import com.example.notepad.ui.base.BaseActivity;
import com.example.notepad.utils.LanguageManager;
import com.example.notepad.utils.SessionManager;

import android.graphics.drawable.Drawable;

public class ProfileActivity extends BaseActivity {
    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding binding;
    private UserViewModel userViewModel;
    private SessionManager sessionManager;
    private LanguageManager languageManager;
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel和管理器
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        sessionManager = new SessionManager(this);
        languageManager = LanguageManager.getInstance(this);

        // 检查用户是否已登录
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 初始化编辑资料页面启动器
        setupEditProfileLauncher();
        
        setupClickListeners();
        loadUserInfo();
        updateLanguageDisplay();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到此页面时刷新用户信息，确保显示最新的数据
        loadUserInfo();
        updateLanguageDisplay();
    }
    
    /**
     * 更新语言显示
     */
    private void updateLanguageDisplay() {
        // 显示当前语言
        TextView languageTextView = binding.getRoot().findViewById(R.id.textLanguage);
        if (languageTextView != null) {
            languageTextView.setText(languageManager.getLanguageDisplayName());
        }
    }

    private void setupEditProfileLauncher() {
        editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 编辑成功后刷新用户信息
                    loadUserInfo();
                }
            }
        );
    }

    private void loadUserInfo() {
        // 从SessionManager获取用户信息
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            binding.textFullName.setText(currentUser.getFullName());
            binding.textEmail.setText(currentUser.getEmail());
            
            // 加载头像
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                loadUserAvatar(currentUser.getAvatarUrl());
            } else {
                binding.imageAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

    private void loadUserAvatar(String avatarUrl) {
        try {
            // 本地文件添加随机参数强制刷新
            String imageUrl = avatarUrl;
            if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                imageUrl = imageUrl + "?timestamp=" + System.currentTimeMillis();
            }

            RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // 缓存所有版本的图片
                .priority(Priority.HIGH)  // 高优先级加载
                .circleCrop()  // 圆形裁剪
                .placeholder(R.drawable.ic_person)  // 加载占位图
                .error(R.drawable.ic_person)  // 错误占位图
                .format(DecodeFormat.PREFER_RGB_565)  // 使用 RGB_565 格式减少内存占用
                .override(300, 300);  // 限制图片大小

            Glide.with(this)
                .load(imageUrl)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())  // 淡入淡出动画
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "加载头像失败: " + e.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource,
                            boolean isFirstResource) {
                        Log.d(TAG, "头像加载成功，来源: " + dataSource.name());
                        return false;
                    }
                })
                .into(binding.imageAvatar);
        } catch (Exception e) {
            Log.e(TAG, "加载头像失败: " + e.getMessage());
            binding.imageAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void setupClickListeners() {
        // 返回按钮点击事件
        binding.buttonBack.setOnClickListener(v -> finish());

        // 编辑资料按钮点击事件
        binding.buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        // 修改密码按钮点击事件
        binding.buttonChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 语言设置按钮点击事件
        binding.buttonLanguage.setOnClickListener(v -> {
            showLanguageSelectionDialog();
        });

        // 帮助与支持按钮点击事件
        binding.buttonHelp.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
        });

        // 退出登录按钮点击事件
        binding.buttonLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }
    
    /**
     * 显示语言选择对话框
     */
    private void showLanguageSelectionDialog() {
        final String[] languages = {"中文", "English"};
        final String[] languageCodes = {LanguageManager.LANGUAGE_CHINESE, LanguageManager.LANGUAGE_ENGLISH};
        
        // 获取当前语言
        String currentLanguage = languageManager.getLanguage();
        int checkedItem = currentLanguage.equals(LanguageManager.LANGUAGE_CHINESE) ? 0 : 1;
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                String selectedLanguage = languageCodes[which];
                // 检查是否与当前语言相同
                if (!selectedLanguage.equals(currentLanguage)) {
                    // 应用新语言设置
                    languageManager.setLanguage(this, selectedLanguage);
                    
                    // 关闭对话框
                    dialog.dismiss();
                    
                    // 重启应用以应用语言更改
                    restartApp();
                } else {
                    dialog.dismiss();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * 重启应用以应用语言更改
     */
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    sessionManager.logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
package com.example.notepad.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.notepad.R;
import com.example.notepad.databinding.ActivityChangePasswordBinding;
import com.example.notepad.data.model.User;
import com.example.notepad.ui.auth.UserViewModel;
import com.example.notepad.utils.SessionManager;
import com.example.notepad.utils.PasswordUtils;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private UserViewModel userViewModel;
    private SessionManager sessionManager;
    private static final String TAG = "ChangePasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化 ViewModel 和 SessionManager
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        sessionManager = new SessionManager(this);

        // 设置点击事件
        setupClickListeners();
        
        // 设置输入监听
        setupTextWatchers();
    }

    private void setupClickListeners() {
        // 返回按钮
        binding.buttonBack.setOnClickListener(v -> finish());
        
        // 修改密码按钮
        binding.buttonChangePassword.setOnClickListener(v -> changePassword());
    }
    
    private void setupTextWatchers() {
        // 监听密码输入，实时验证
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordMatch();
            }
        };
        
        binding.editNewPassword.addTextChangedListener(passwordWatcher);
        binding.editConfirmPassword.addTextChangedListener(passwordWatcher);
    }
    
    private boolean validatePasswordMatch() {
        String password = binding.editNewPassword.getText().toString();
        String confirmPassword = binding.editConfirmPassword.getText().toString();
        boolean isMatch = password.equals(confirmPassword);
        
        if (!isMatch && !confirmPassword.isEmpty()) {
            binding.layoutConfirmPassword.setError(getString(R.string.password_mismatch));
        } else {
            binding.layoutConfirmPassword.setError(null);
        }
        
        return isMatch || confirmPassword.isEmpty();
    }

    private void changePassword() {
        // 获取输入
        String currentPassword = binding.editCurrentPassword.getText().toString().trim();
        String newPassword = binding.editNewPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();

        // 验证输入不为空
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证两次密码输入一致
        if (!validatePasswordMatch()) {
            return;
        }

        // 获取当前用户名
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getUsername() != null) {
            Log.d(TAG, "开始修改密码，用户名: " + currentUser.getUsername());
            
            // 显示加载状态
            showLoading(true);
            
            // 在工作线程中验证密码
            new Thread(() -> {
                try {
                    // 从数据库获取最新的用户信息
                    User dbUser = userViewModel.getUserByUsername(currentUser.getUsername());
                    
                    // 切回UI线程处理结果
                    runOnUiThread(() -> {
                        if (dbUser == null) {
                            showLoading(false);
                            Log.e(TAG, "用户不存在: " + currentUser.getUsername());
                            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // 检查数据库中的密码是否为空
                        String storedPassword = dbUser.getPassword();
                        if (storedPassword == null || storedPassword.isEmpty()) {
                            Log.w(TAG, "数据库中的密码为空，允许直接设置新密码");
                            
                            // 如果数据库中的密码为空，直接允许设置新密码
                            updatePassword(newPassword);
                            return;
                        }

                        try {
                            Log.d(TAG, "验证当前密码...");
                            Log.d(TAG, "存储的密码哈希: " + storedPassword);
                            
                            // 使用PasswordUtils验证当前密码
                            if (!PasswordUtils.verifyPassword(currentPassword, storedPassword)) {
                                showLoading(false);
                                Log.e(TAG, "当前密码验证失败");
                                binding.layoutCurrentPassword.setError(getString(R.string.password_incorrect));
                                return;
                            }
                            
                            Log.d(TAG, "当前密码验证成功，开始更新密码");
                            
                            // 更新密码
                            updatePassword(newPassword);
                        } catch (Exception e) {
                            showLoading(false);
                            Log.e(TAG, "密码验证失败: " + e.getMessage(), e);
                            Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Log.e(TAG, "获取用户信息失败: " + e.getMessage(), e);
                        Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } else {
            Log.e(TAG, "当前用户为空或用户名为空");
            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updatePassword(String newPassword) {
        // 显示加载状态
        showLoading(true);
        
        // 获取当前用户
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "开始更新密码，用户名: " + currentUser.getUsername());
            final String username = currentUser.getUsername();
            
            // 在工作线程中执行密码更新
            new Thread(() -> {
                try {
                    // 使用专门的密码更新方法
                    boolean success = userViewModel.getRepository().updatePassword(username, newPassword);
                    Log.d(TAG, "密码更新结果: " + success);
                    
                    if (success) {
                        // 在工作线程中获取更新后的用户信息
                        User updatedUser = userViewModel.getUserByUsername(username);
                        
                        // 在UI线程上执行UI操作
                        runOnUiThread(() -> {
                            // 隐藏加载状态
                            showLoading(false);
                            
                            if (updatedUser != null) {
                                // 更新Session中的用户信息
                                sessionManager.saveUser(updatedUser);
                                Log.d(TAG, "用户信息已更新到Session");
                                
                                // 显示成功消息
                                Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show();
                                
                                // 密码修改成功后，执行退出登录操作
                                logoutAfterPasswordChange();
                            } else {
                                // 获取更新后的用户信息失败
                                Log.e(TAG, "获取更新后的用户信息失败");
                                Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // 更新密码失败
                        runOnUiThread(() -> {
                            showLoading(false);
                            Log.e(TAG, "更新密码失败");
                            Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新密码失败: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(this, R.string.error_updating_profile, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }
    
    /**
     * 密码修改成功后退出登录
     */
    private void logoutAfterPasswordChange() {
        // 退出登录
        sessionManager.logout();
        
        // 返回到登录页面
        Intent intent = new Intent(this, com.example.notepad.ui.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // 结束当前Activity
        finish();
    }
    
    private void showLoading(boolean isLoading) {
        binding.buttonChangePassword.setEnabled(!isLoading);
        binding.editCurrentPassword.setEnabled(!isLoading);
        binding.editNewPassword.setEnabled(!isLoading);
        binding.editConfirmPassword.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
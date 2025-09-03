package com.example.notepad.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.notepad.MainActivity;
import com.example.notepad.R;
import com.example.notepad.data.model.User;
import com.example.notepad.databinding.ActivityRegisterBinding;
import com.example.notepad.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private UserViewModel userViewModel;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel和SessionManager
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager = new SessionManager(this);

        // 设置观察者
        setupObservers();
        
        setupClickListeners();
    }

    private void setupObservers() {
        // 观察注册结果
        authViewModel.getRegisterResult().observe(this, success -> {
            if (success) {
                // 注册成功，自动登录
                String username = binding.editUsername.getText().toString().trim();
                String password = binding.editPassword.getText().toString().trim();
                authViewModel.login(username, password);
            } else {
                binding.buttonRegister.setEnabled(true);
                binding.buttonRegister.setText(R.string.register);
                Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察登录结果
        authViewModel.getLoginResult().observe(this, success -> {
            if (success) {
                // 登录成功，获取用户信息并保存
                executor.execute(() -> {
                    String username = binding.editUsername.getText().toString().trim();
                    String fullName = binding.editFullName.getText().toString().trim();
                    String password = binding.editPassword.getText().toString().trim();
                    
                    User user = userViewModel.getUserByUsername(username);
                    if (user != null) {
                        // 更新用户全名
                        user.setFullName(fullName);
                        
                        // 确保密码被正确设置
                        if (user.getPassword() == null || user.getPassword().isEmpty()) {
                            // 如果密码为空，手动设置密码哈希
                            String hashedPassword = authViewModel.getRepository().hashPasswordForUser(password);
                            if (hashedPassword != null && !hashedPassword.isEmpty()) {
                                user.setPassword(hashedPassword);
                            }
                        }
                        
                        // 更新用户信息
                        userViewModel.update(user);
                        
                        // 保存用户会话
                        sessionManager.createLoginSession(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            fullName
                        );
                        
                        // 在主线程中跳转到主页
                        mainHandler.post(() -> {
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finishAffinity();
                        });
                    }
                });
            } else {
                binding.buttonRegister.setEnabled(true);
                binding.buttonRegister.setText(R.string.register);
                Toast.makeText(this, R.string.login_failed_after_register, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupClickListeners() {
        // 返回按钮点击事件
        binding.buttonBack.setOnClickListener(v -> finish());

        // 登录按钮点击事件
        binding.textLogin.setOnClickListener(v -> finish());

        // 注册按钮点击事件
        binding.buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        // 获取输入内容
        String fullName = binding.editFullName.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(fullName)) {
            binding.fullNameLayout.setError(getString(R.string.error_fullname_empty));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.error_email_empty));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(username)) {
            binding.usernameLayout.setError(getString(R.string.error_username_empty));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.error_password_empty));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_passwords_not_match));
            return;
        }

        // 清除错误提示
        binding.fullNameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.usernameLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);

        // 显示加载状态
        binding.buttonRegister.setEnabled(false);
        binding.buttonRegister.setText(R.string.registering);

        // 在工作线程中验证用户名是否存在
        executor.execute(() -> {
            // 检查用户名是否已存在
            User existingUser = userViewModel.getUserByUsername(username);
            
            mainHandler.post(() -> {
                if (existingUser != null) {
                    binding.usernameLayout.setError(getString(R.string.error_username_exists));
                    binding.buttonRegister.setEnabled(true);
                    binding.buttonRegister.setText(R.string.register);
                } else {
                    // 创建新用户并注册
                    authViewModel.register(username, password, email);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
package com.example.notepad.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.notepad.MainActivity;
import com.example.notepad.R;
import com.example.notepad.data.model.User;
import com.example.notepad.databinding.ActivityLoginBinding;
import com.example.notepad.ui.base.BaseActivity;
import com.example.notepad.utils.SessionManager;

public class LoginActivity extends BaseActivity {
    private ActivityLoginBinding binding;
    private UserViewModel userViewModel;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel和SessionManager
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager = new SessionManager(this);

        // 设置观察者
        setupObservers();

        // 检查是否已登录
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setupClickListeners();
    }

    private void setupObservers() {
        // 观察登录结果
        authViewModel.getLoginResult().observe(this, success -> {
            // 隐藏加载状态
            binding.buttonLogin.setEnabled(true);
            binding.progressBar.setVisibility(android.view.View.GONE);
            
            if (success) {
                // 登录成功，获取用户信息
                String username = binding.editUsername.getText().toString().trim();
                
                executor.execute(() -> {
                    User user = userViewModel.getUserByUsername(username);
                    
                    if (user != null) {
                        // 保存用户会话
                        sessionManager.saveUser(user);
                        
                        // 在主线程中跳转到主页
                        mainHandler.post(() -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            Toast.makeText(LoginActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                // 登录失败
                Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        // 登录按钮点击事件
        binding.buttonLogin.setOnClickListener(v -> handleLogin());

        // 注册按钮点击事件
        binding.buttonRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // 忘记密码点击事件
        binding.textForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void handleLogin() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载状态
        binding.buttonLogin.setEnabled(false);
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        // 执行登录
        authViewModel.login(username, password);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
package com.example.notepad.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.notepad.R;
import com.example.notepad.databinding.ActivityForgotPasswordBinding;
import com.example.notepad.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private AuthViewModel authViewModel;
    private static final String TAG = "ForgotPasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // 重置密码按钮点击事件
        binding.buttonResetPassword.setOnClickListener(v -> {
            if (validateResetForm()) {
                resetPassword();
            }
        });

        // 返回登录按钮点击事件
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // 观察重置密码结果
        authViewModel.getResetPasswordResult().observe(this, success -> {
            hideLoading();
            if (success) {
                Toast.makeText(this, R.string.password_reset_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // 显示更明确的错误信息
                Toast.makeText(this, R.string.password_reset_failed, Toast.LENGTH_LONG).show();
                binding.usernameLayout.setError(getString(R.string.username_email_mismatch));
                binding.emailLayout.setError(getString(R.string.username_email_mismatch));
                Log.e(TAG, getString(R.string.password_reset_failed));
            }
        });
    }

    private boolean validateResetForm() {
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String newPassword = binding.editNewPassword.getText().toString();
        String confirmPassword = binding.editConfirmPassword.getText().toString();
        boolean isValid = true;

        // 重置所有错误
        binding.usernameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.newPasswordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);

        // 验证用户名
        if (TextUtils.isEmpty(username)) {
            binding.usernameLayout.setError(getString(R.string.error_username_empty));
            isValid = false;
        }

        // 验证邮箱
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.error_email_empty));
            isValid = false;
        } else if (!ValidationUtils.isEmailValid(email)) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // 验证新密码
        if (TextUtils.isEmpty(newPassword)) {
            binding.newPasswordLayout.setError(getString(R.string.error_password_empty));
            isValid = false;
        }

        // 验证确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_confirm_password_empty));
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_passwords_not_match));
            isValid = false;
        }

        return isValid;
    }

    private void resetPassword() {
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String newPassword = binding.editNewPassword.getText().toString();

        showLoading();
        authViewModel.resetPassword(username, email, newPassword);
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonResetPassword.setVisibility(View.INVISIBLE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.buttonResetPassword.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
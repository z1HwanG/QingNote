package com.example.notepad.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.notepad.data.model.User;
import com.example.notepad.data.repository.UserRepository;
import com.example.notepad.utils.PasswordUtils;
import com.example.notepad.utils.ValidationUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final Executor executor;
    private final MutableLiveData<Boolean> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> resetPasswordResult = new MutableLiveData<>();
    private static final String TAG = "AuthViewModel";

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        executor = Executors.newSingleThreadExecutor();
    }

    public void login(String username, String password) {
        executor.execute(() -> {
            try {
                // 获取用户信息
                User user = userRepository.getUserByUsername(username);
                if (user != null && PasswordUtils.verifyPassword(password, user.getPassword())) {
                    loginResult.postValue(true);
                } else {
                    loginResult.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "登录失败", e);
                loginResult.postValue(false);
            }
        });
    }

    public void register(String username, String password, String email) {
        executor.execute(() -> {
            try {
                // 创建用户对象，不对密码进行哈希处理
                User user = new User(username, password, email);
                long userId = userRepository.register(user);
                registerResult.postValue(userId > 0);
            } catch (Exception e) {
                Log.e(TAG, "注册失败", e);
                registerResult.postValue(false);
            }
        });
    }

    public void resetPassword(String username, String email, String newPassword) {
        executor.execute(() -> {
            try {
                // 验证用户名和邮箱是否匹配
                User user = userRepository.getUserByUsername(username);
                Log.d(TAG, "重置密码：尝试用户 " + username);
                
                if (user == null) {
                    Log.d(TAG, "重置密码失败：用户不存在 " + username);
                    resetPasswordResult.postValue(false);
                    return;
                }
                
                if (!email.equals(user.getEmail())) {
                    Log.d(TAG, "重置密码失败：邮箱不匹配 " + email + " vs " + user.getEmail());
                    resetPasswordResult.postValue(false);
                    return;
                }
                
                // 更新密码
                boolean success = userRepository.updatePassword(username, newPassword);
                if (success) {
                    Log.d(TAG, "重置密码成功：用户 " + username);
                } else {
                    Log.d(TAG, "重置密码失败：密码更新失败 " + username);
                }
                resetPasswordResult.postValue(success);
            } catch (Exception e) {
                Log.e(TAG, "重置密码失败：异常 " + e.getMessage(), e);
                resetPasswordResult.postValue(false);
            }
        });
    }

    /**
     * 获取UserRepository实例
     * @return UserRepository实例
     */
    public UserRepository getRepository() {
        return userRepository;
    }

    /**
     * 对密码进行哈希处理（用于直接设置到用户对象）
     * @param password 原始密码
     * @return 哈希后的密码
     */
    public String hashPasswordForUser(String password) {
        try {
            if (password == null || password.isEmpty()) {
                Log.e(TAG, "无法哈希空密码");
                return null;
            }
            return PasswordUtils.hashPassword(password);
        } catch (Exception e) {
            Log.e(TAG, "密码哈希失败: " + e.getMessage(), e);
            return null;
        }
    }

    public LiveData<Boolean> getLoginResult() {
        return loginResult;
    }

    public LiveData<Boolean> getRegisterResult() {
        return registerResult;
    }

    public LiveData<Boolean> getResetPasswordResult() {
        return resetPasswordResult;
    }
} 
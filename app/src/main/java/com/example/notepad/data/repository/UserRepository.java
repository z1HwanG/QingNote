package com.example.notepad.data.repository;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.notepad.data.AppDatabase;
import com.example.notepad.data.dao.UserDao;
import com.example.notepad.data.model.User;
import com.example.notepad.utils.FileUtils;
import com.example.notepad.utils.PasswordUtils;
import com.example.notepad.utils.SessionManager;

/**
 * 用户仓库类，用于管理用户数据的访问
 */
public class UserRepository {
    private final UserDao userDao;
    private final Executor executor;
    private final Context context;
    private static final String TAG = "UserRepository";
    
    public UserRepository(Context context) {
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        executor = Executors.newSingleThreadExecutor();
    }
    
    public UserRepository(Application application) {
        this((Context) application);
    }
    
    public void insert(User user) {
        executor.execute(() -> userDao.insert(user));
    }
    
    public void update(User user) {
        executor.execute(() -> userDao.update(user));
    }
    
    public void delete(User user) {
        executor.execute(() -> userDao.delete(user));
    }
    
    public LiveData<User> getUserByIdAsync(int id) {
        return userDao.getUserByIdAsync(id);
    }
    
    public LiveData<User> getUserByUsernameAsync(String username) {
        return userDao.getUserByUsernameAsync(username);
    }
    
    public LiveData<User> getUserByEmailAsync(String email) {
        return userDao.getUserByEmailAsync(email);
    }
    
    public User login(String username, String password) {
        try {
            User user = userDao.getUserByUsername(username);
            if (user != null && PasswordUtils.verifyPassword(password, user.getPassword())) {
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "登录失败", e);
        }
        return null;
    }
    
    /**
     * 确保用户存在，如果不存在则创建一个默认用户
     * 用于解决外键约束问题
     */
    public void ensureUserExists(int userId, String username) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user == null) {
                // 创建默认用户
                User defaultUser = new User();
                defaultUser.setId(userId);
                defaultUser.setUsername(username != null ? username : "default_user");
                defaultUser.setEmail("default@example.com");
                defaultUser.setPassword("default_password");
                defaultUser.setFullName("Default User");
                userDao.insert(defaultUser);
            }
        });
    }

    public interface UploadCallback {
        void onComplete(String url);
    }

    public interface UpdateCallback {
        void onComplete(boolean success);
    }

    public void uploadAvatar(Uri imageUri, UploadCallback callback) {
        executor.execute(() -> {
            try {
                // 尝试获取URI的永久权限（如果是content URI）
                if (imageUri.toString().startsWith("content://")) {
                    try {
                        context.getContentResolver().takePersistableUriPermission(
                            imageUri, 
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        // 无法获取永久权限，但仍然继续
                    }
                }
                
                // 返回URI字符串
                callback.onComplete(imageUri.toString());
            } catch (Exception e) {
                callback.onComplete(null);
            }
        });
    }

    public void updateUser(User user, UpdateCallback callback) {
        executor.execute(() -> {
            try {
                userDao.update(user);
                callback.onComplete(true);
            } catch (Exception e) {
                callback.onComplete(false);
            }
        });
    }

    public long register(User user) {
        try {
            Log.d(TAG, "开始注册用户: " + user.getUsername());
            Log.d(TAG, "密码长度: " + (user.getPassword() != null ? user.getPassword().length() : "null"));
            
            // 检查用户名是否已存在
            if (userDao.getUserByUsername(user.getUsername()) != null) {
                Log.e(TAG, "用户名已存在: " + user.getUsername());
                return -1;
            }
            
            // 检查邮箱是否已存在
            if (userDao.getUserByEmail(user.getEmail()) != null) {
                Log.e(TAG, "邮箱已存在: " + user.getEmail());
                return -2;
            }
            
            // 检查密码是否为空
            String password = user.getPassword();
            if (password == null || password.isEmpty()) {
                Log.e(TAG, "密码为空");
                return -3;
            }
            
            // 对密码进行哈希处理
            String hashedPassword = PasswordUtils.hashPassword(password);
            if (hashedPassword == null) {
                Log.e(TAG, "密码哈希失败");
                return -4;
            }
            
            Log.d(TAG, "密码哈希成功，哈希长度: " + hashedPassword.length());
            
            // 设置哈希后的密码
            user.setPassword(hashedPassword);
            
            // 插入用户前再次检查密码是否正确设置
            Log.d(TAG, "即将插入用户，密码哈希: " + user.getPassword());
            
            // 插入用户
            long userId = userDao.insert(user);
            Log.d(TAG, "用户注册成功，ID: " + userId);
            
            // 验证用户是否已正确插入
            User insertedUser = userDao.getUserByUsername(user.getUsername());
            if (insertedUser != null) {
                Log.d(TAG, "验证插入用户: ID=" + insertedUser.getId() + 
                          ", 用户名=" + insertedUser.getUsername() + 
                          ", 密码哈希长度=" + 
                          (insertedUser.getPassword() != null ? insertedUser.getPassword().length() : "null"));
            } else {
                Log.e(TAG, "用户插入后无法查询到");
            }
            
            return userId;
        } catch (Exception e) {
            Log.e(TAG, "注册失败: " + e.getMessage(), e);
            return -5;
        }
    }

    public User getUserByUsername(String username) {
        try {
            return userDao.getUserByUsername(username);
        } catch (Exception e) {
            Log.e(TAG, "获取用户信息失败", e);
            return null;
        }
    }

    /**
     * 对密码进行哈希处理（不更新数据库）
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

    public boolean updatePassword(String username, String newPassword) {
        try {
            Log.d(TAG, "尝试更新密码：用户 " + username);
            User user = userDao.getUserByUsername(username);
            if (user != null) {
                // 对密码进行哈希处理
                String hashedPassword = PasswordUtils.hashPassword(newPassword);
                if (hashedPassword == null) {
                    Log.e(TAG, "密码哈希失败");
                    return false;
                }
                
                user.setPassword(hashedPassword);
                Log.d(TAG, "正在更新密码...");
                userDao.update(user);
                Log.d(TAG, "密码更新成功");
                return true;
            } else {
                Log.e(TAG, "更新密码失败：用户不存在 " + username);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "更新密码异常：" + e.getMessage(), e);
            return false;
        }
    }
} 
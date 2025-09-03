package com.example.notepad.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.notepad.data.model.User;
import com.example.notepad.data.repository.UserRepository;

/**
 * 用户ViewModel类，处理与用户相关的业务逻辑
 */
public class UserViewModel extends AndroidViewModel {
    private UserRepository repository;
    
    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }
    
    public void insert(User user) {
        repository.insert(user);
    }
    
    public void update(User user) {
        repository.update(user);
    }
    
    /**
     * 使用回调方式更新用户信息
     * @param user 用户对象
     * @param callback 操作结果回调
     */
    public void updateUser(User user, UserRepository.UpdateCallback callback) {
        repository.updateUser(user, callback);
    }
    
    /**
     * 根据用户名异步获取用户
     * @param username 用户名
     * @return 包含用户数据的LiveData
     */
    public LiveData<User> getUserByUsernameAsync(String username) {
        return repository.getUserByUsernameAsync(username);
    }
    
    /**
     * 根据邮箱异步获取用户
     * @param email 邮箱
     * @return 包含用户数据的LiveData
     */
    public LiveData<User> getUserByEmailAsync(String email) {
        return repository.getUserByEmailAsync(email);
    }
    
    /**
     * 根据ID异步获取用户
     * @param id 用户ID
     * @return 包含用户数据的LiveData
     */
    public LiveData<User> getUserByIdAsync(int id) {
        return repository.getUserByIdAsync(id);
    }
    
    /**
     * 获取UserRepository实例
     * @return UserRepository实例
     */
    public UserRepository getRepository() {
        return repository;
    }
    
    /**
     * 同步获取用户（可在工作线程中调用）
     * @param username 用户名
     * @return User对象
     */
    public User getUserByUsername(String username) {
        // 这里需要在工作线程中调用，不能在主线程上直接调用
        return repository.getUserByUsername(username);
    }
} 
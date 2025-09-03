package com.example.notepad.ui.profile;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.notepad.data.repository.UserRepository;
import com.example.notepad.data.model.User;
import com.example.notepad.utils.SessionManager;
import com.example.notepad.utils.UriConverter;

import java.util.UUID;

public class EditProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();
    private Uri selectedImageUri;
    private final Application application;

    public EditProfileViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        userRepository = new UserRepository(application);
        sessionManager = new SessionManager(application);
    }

    public void setSelectedImageUri(Uri uri) {
        this.selectedImageUri = uri;
        
        // 尝试获取持久访问权限
        UriConverter.takePersistablePermission(application, uri);
    }

    public void updateProfile(String username, String email, String fullName) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            updateResult.postValue(false);
            return;
        }

        // 更新用户信息
        currentUser.setUsername(username);
        currentUser.setEmail(email);
        currentUser.setFullName(fullName);

        // 如果选择了新头像，先上传头像
        if (selectedImageUri != null) {
            // 尝试复制图片到内部存储以确保持久性访问
            Uri internalUri = UriConverter.copyUriToInternalStorage(
                application,
                selectedImageUri,
                "avatar_" + UUID.randomUUID().toString() + ".jpg"
            );
            
            // 如果内部复制成功，使用内部URI；否则使用原始URI
            final Uri finalUri = internalUri != null ? internalUri : selectedImageUri;
            
            userRepository.uploadAvatar(finalUri, avatarUrl -> {
                if (avatarUrl != null) {
                    currentUser.setAvatarUrl(avatarUrl);
                    updateUserData(currentUser);
                } else {
                    updateResult.postValue(false);
                }
            });
        } else {
            updateUserData(currentUser);
        }
    }

    private void updateUserData(User user) {
        userRepository.updateUser(user, success -> {
            if (success) {
                sessionManager.saveUser(user);
            }
            updateResult.postValue(success);
        });
    }

    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }
} 
package com.example.notepad.utils;

import android.text.TextUtils;
import android.util.Patterns;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{" + MIN_PASSWORD_LENGTH + ",}$");

    /**
     * 验证密码是否有效
     * 密码必须：
     * 1. 至少8个字符
     * 2. 包含至少一个字母和一个数字
     */
    public static boolean isPasswordValid(String password) {
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证邮箱是否有效
     */
    public static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * 验证用户名是否有效
     * 用户名必须：
     * 1. 不少于3个字符
     * 2. 不超过20个字符
     * 3. 只包含字母、数字、下划线
     */
    public static boolean isUsernameValid(String username) {
        if (TextUtils.isEmpty(username)) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    /**
     * 验证验证码是否有效
     * 验证码必须：
     * 1. 6位数字
     */
    public static boolean isVerificationCodeValid(String code) {
        if (TextUtils.isEmpty(code)) {
            return false;
        }
        return code.matches("^\\d{6}$");
    }
} 
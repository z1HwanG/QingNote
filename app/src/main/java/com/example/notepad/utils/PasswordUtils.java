package com.example.notepad.utils;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {
    private static final String TAG = "PasswordUtils";
    private static final int SALT_LENGTH = 16;

    /**
     * 对密码进行加密
     * @param password 原始密码
     * @return 加密后的密码（格式：salt:hash）
     */
    public static String hashPassword(String password) {
        try {
            if (password == null || password.isEmpty()) {
                Log.e(TAG, "无法哈希空密码");
                return null;
            }

            Log.d(TAG, "开始哈希密码，密码长度: " + password.length());

            // 生成随机盐值
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 将密码和盐值组合后进行SHA-256哈希
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // 将盐值和哈希后的密码编码为Base64字符串
            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hashedPassword);

            // 返回格式：salt:hash
            String result = saltString + ":" + hashString;
            Log.d(TAG, "密码哈希成功，结果长度: " + result.length());
            return result;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "加密密码失败: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "密码哈希过程中出现未知错误: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证密码
     * @param password 待验证的密码
     * @param storedPassword 存储的加密密码
     * @return 密码是否匹配
     */
    public static boolean verifyPassword(String password, String storedPassword) {
        try {
            Log.d(TAG, "开始验证密码");
            
            // 检查输入
            if (password == null || password.isEmpty()) {
                Log.e(TAG, "待验证的密码为空");
                return false;
            }
            
            if (storedPassword == null || storedPassword.isEmpty()) {
                Log.e(TAG, "存储的密码哈希为空");
                return false;
            }
            
            // 分离存储的盐值和哈希值
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                Log.e(TAG, "存储的密码格式错误，应为 salt:hash 格式");
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            // 使用相同的盐值对输入的密码进行哈希
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // 比较哈希值
            if (storedHash.length != hashedPassword.length) {
                Log.e(TAG, "哈希长度不匹配");
                return false;
            }
            
            // 使用常量时间比较防止时序攻击
            int diff = 0;
            for (int i = 0; i < storedHash.length; i++) {
                diff |= storedHash[i] ^ hashedPassword[i];
            }
            
            boolean result = diff == 0;
            Log.d(TAG, "密码验证结果: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "验证密码失败: " + e.getMessage(), e);
            return false;
        }
    }
} 
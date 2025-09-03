package com.example.notepad.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notepad.utils.LanguageManager;

/**
 * 基础Activity类
 * 所有Activity的基类
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void attachBaseContext(Context newBase) {
        // 应用语言设置
        Context context = LanguageManager.getInstance(newBase).applyLanguage(newBase);
        super.attachBaseContext(context);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 系统配置改变时（如系统语言改变）重新应用语言设置
        LanguageManager.getInstance(this).applyLanguage(this);
    }
} 
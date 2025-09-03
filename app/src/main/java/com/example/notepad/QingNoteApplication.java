package com.example.notepad;

import android.app.Application;
import android.content.Context;

import com.example.notepad.utils.LanguageManager;

public class QingNoteApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化语言设置
        LanguageManager.getInstance(this).applyLanguage(this);
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        // 在基础上下文阶段，直接使用基础上下文
        super.attachBaseContext(base);
    }
} 
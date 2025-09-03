package com.example.notepad.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

/**
 * 语言管理器
 * 负责应用内语言切换和保存语言设置
 */
public class LanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String PREF_LANGUAGE = "pref_language";
    private static LanguageManager instance;
    private SharedPreferences preferences;
    private final Context applicationContext;

    // 支持的语言列表
    public static final String LANGUAGE_CHINESE = "zh";
    public static final String LANGUAGE_ENGLISH = "en";

    private LanguageManager(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context);
        }
        return instance;
    }

    private SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        }
        return preferences;
    }

    /**
     * 设置应用语言
     * @param context 上下文
     * @param language 语言代码：zh或en
     * @return 更新后的Context
     */
    public Context setLanguage(Context context, String language) {
        // 保存语言设置到SharedPreferences
        getPreferences().edit().putString(PREF_LANGUAGE, language).apply();
        
        // 更新应用资源配置
        return updateResources(context, language);
    }

    /**
     * 应用当前保存的语言设置
     * @param context 上下文
     * @return 更新后的Context
     */
    public Context applyLanguage(Context context) {
        String currentLanguage = getLanguage();
        return updateResources(context, currentLanguage);
    }

    /**
     * 获取当前设置的语言
     * @return 语言代码
     */
    public String getLanguage() {
        return getPreferences().getString(PREF_LANGUAGE, getDeviceLanguage());
    }

    /**
     * 获取设备的默认语言
     * @return 语言代码
     */
    public String getDeviceLanguage() {
        String deviceLanguage = Locale.getDefault().getLanguage();
        // 如果不是我们支持的语言，默认使用英文
        if (!deviceLanguage.equals(LANGUAGE_CHINESE) && !deviceLanguage.equals(LANGUAGE_ENGLISH)) {
            return LANGUAGE_ENGLISH;
        }
        return deviceLanguage;
    }

    /**
     * 获取当前语言的显示名称
     * @return 语言名称（中文/English）
     */
    public String getLanguageDisplayName() {
        String language = getLanguage();
        return language.equals(LANGUAGE_CHINESE) ? "中文" : "English";
    }

    /**
     * 更新资源配置
     * @param context 上下文
     * @param language 语言代码
     * @return 更新后的Context
     */
    private Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        
        Log.d(TAG, "Applying language: " + language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
} 
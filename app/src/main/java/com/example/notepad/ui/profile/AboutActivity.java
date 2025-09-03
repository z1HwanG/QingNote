package com.example.notepad.ui.profile;

import android.os.Bundle;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.widget.TextView;

import com.example.notepad.R;
import com.example.notepad.databinding.ActivityAboutBinding;
import com.example.notepad.ui.base.BaseActivity;

public class AboutActivity extends BaseActivity {
    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        loadAppInfo();
    }

    private void setupViews() {
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    private void loadAppInfo() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.textVersion.setText(getString(R.string.version_format, packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 
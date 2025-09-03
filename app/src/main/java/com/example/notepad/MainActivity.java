package com.example.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.notepad.data.repository.UserRepository;
import com.example.notepad.databinding.ActivityMainBinding;
import com.example.notepad.ui.auth.LoginActivity;
import com.example.notepad.ui.note.CreateNoteActivity;
import com.example.notepad.ui.note.NoteAdapter;
import com.example.notepad.ui.note.NoteViewModel;
import com.example.notepad.ui.profile.ProfileActivity;
import com.example.notepad.utils.SessionManager;
import com.example.notepad.ui.base.BaseActivity;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private NoteViewModel noteViewModel;
    private SessionManager sessionManager;
    private NoteAdapter noteAdapter;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 开发阶段临时代码：清除数据库
        // 仅用于开发测试，发布前应移除
//        deleteDatabase("qingnote_db");
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);

        // 初始化ViewModel和SessionManager
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(getApplication());

        // 检查用户是否已登录
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // 确保用户在数据库中存在（解决外键约束问题）
        ensureUserExists();

        // 初始化RecyclerView
        setupRecyclerView();
        setupListeners();
        loadNotes();
    }
    
    /**
     * 确保用户在数据库中存在
     */
    private void ensureUserExists() {
        int userId = sessionManager.getUserId();
        String username = sessionManager.getUsername();
        if (userId != -1) {
            userRepository.ensureUserExists(userId, username);
        }
    }

    private void setupRecyclerView() {
        noteAdapter = new NoteAdapter();
        
        // 设置点击事件监听器
        noteAdapter.setOnNoteClickListener(note -> {
            // 打开编辑笔记页面
            Intent intent = new Intent(this, CreateNoteActivity.class);
            intent.putExtra(CreateNoteActivity.EXTRA_NOTE_ID, note.getId());
            startActivity(intent);
        });

        // 设置删除事件监听器
        noteAdapter.setOnNoteDeleteListener(note -> {
            // 删除笔记
            noteViewModel.delete(note);
        });
        
        // 使用默认列表布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerViewNotes.setLayoutManager(layoutManager);
        binding.recyclerViewNotes.setAdapter(noteAdapter);
    }

    private void setupListeners() {
        // 个人资料按钮点击事件
        binding.buttonProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // 底部导航点击事件
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                return true;
            } else if (id == R.id.menu_add) {
                startActivity(new Intent(this, CreateNoteActivity.class));
                return false;
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false;
            }
            return false;
        });
        
        // 搜索框文本变化监听
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchNotes(s.toString());
            }
        });
    }

    private void loadNotes() {
        int userId = sessionManager.getUserId();
        if (userId != -1) {
            noteViewModel.getAllNotesByUser(userId).observe(this, notes -> {
                if (notes != null && !notes.isEmpty()) {
                    noteAdapter.submitList(notes);
                    binding.recyclerViewNotes.setVisibility(View.VISIBLE);
                    binding.textEmpty.setVisibility(View.GONE);
                } else {
                    binding.recyclerViewNotes.setVisibility(View.GONE);
                    binding.textEmpty.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void searchNotes(String query) {
        int userId = sessionManager.getUserId();
        if (userId != -1) {
            if (query.isEmpty()) {
                loadNotes();
            } else {
                noteViewModel.searchNotes(userId, query).observe(this, notes -> {
                    noteAdapter.submitList(notes);
                    if (notes != null && !notes.isEmpty()) {
                        binding.recyclerViewNotes.setVisibility(View.VISIBLE);
                        binding.textEmpty.setVisibility(View.GONE);
                    } else {
                        binding.recyclerViewNotes.setVisibility(View.GONE);
                        binding.textEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            // 打开个人资料页面
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到主页时重新加载笔记列表
        loadNotes();
        // 设置底部导航选中状态
        binding.bottomNav.setSelectedItemId(R.id.menu_home);
        // 检查卡片样式是否改变
        if (binding != null && binding.recyclerViewNotes != null) {
            setupRecyclerView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
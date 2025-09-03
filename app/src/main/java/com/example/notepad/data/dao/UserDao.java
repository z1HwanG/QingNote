package com.example.notepad.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.notepad.data.model.User;

import java.util.List;

/**
 * 用户数据访问对象接口
 */
@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    LiveData<User> getUserByUsernameAsync(String username);
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    LiveData<User> getUserByEmailAsync(String email);
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    LiveData<User> getUserByIdAsync(int id);
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserByIdSync(int id);
    
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    LiveData<User> login(String username, String password);

    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);
} 
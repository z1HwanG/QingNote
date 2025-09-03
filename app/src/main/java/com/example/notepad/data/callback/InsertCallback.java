package com.example.notepad.data.callback;

import com.example.notepad.data.model.Note;

/**
 * 笔记插入回调接口
 */
public interface InsertCallback {
    /**
     * 插入完成时调用
     * @param note 插入成功的笔记对象
     */
    void onComplete(Note note);

    /**
     * 发生错误时调用
     * @param e 异常对象
     */
    void onError(Exception e);
} 
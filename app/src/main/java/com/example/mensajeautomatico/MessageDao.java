package com.example.mensajeautomatico;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    List<MessageEntity> getAllMessages();

    @Query("SELECT * FROM messages WHERE status = 'Programado' AND scheduledTime <= :currentTime")
    List<MessageEntity> getScheduledMessages(long currentTime);

    @Query("SELECT * FROM messages WHERE id = :messageId")
    MessageEntity getMessageById(int messageId);
}
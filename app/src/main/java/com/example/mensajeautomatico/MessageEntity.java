package com.example.mensajeautomatico;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String phoneNumber;
    public String messageText;
    public long scheduledTime;
    public String status; // "Programado", "Enviado", "Error"
    public long createdAt;

    public MessageEntity(String phoneNumber, String messageText, long scheduledTime, String status) {
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.scheduledTime = scheduledTime;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }
}
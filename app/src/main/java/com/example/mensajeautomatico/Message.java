package com.example.mensajeautomatico;

/**
 * Clase de modelo de datos para representar un mensaje programado o enviado.
 */
public class Message {
    private String phoneNumber;
    private String messageText;
    private String timestamp;
    private String status;  // Nuevo campo para estado

    public Message(String phoneNumber, String messageText, String timestamp, String status) {
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }
}
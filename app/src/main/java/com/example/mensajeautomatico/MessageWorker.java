package com.example.mensajeautomatico;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MessageWorker extends Worker {

    private static final String TAG = "MessageWorker";
    public static final String ACTION_SEND_MESSAGE = "com.example.mensajeautomatico.SEND_MESSAGE";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_MESSAGE_TEXT = "message_text";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    public MessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String phoneNumber = getInputData().getString(EXTRA_PHONE_NUMBER);
        String messageText = getInputData().getString(EXTRA_MESSAGE_TEXT);
        int messageId = getInputData().getInt(EXTRA_MESSAGE_ID, -1);

        Log.d(TAG, "Iniciando doWork para el mensaje ID: " + messageId);

        if (phoneNumber == null || messageText == null) {
            Log.e(TAG, "Datos de entrada nulos. No se puede enviar el mensaje.");
            updateMessageStatus(messageId, "Error");
            return Result.failure();
        }

        try {
            // Enviar broadcast para que el servicio de accesibilidad maneje WhatsApp
            Intent intent = new Intent(ACTION_SEND_MESSAGE);
            intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
            intent.putExtra(EXTRA_MESSAGE_TEXT, messageText);
            intent.putExtra(EXTRA_MESSAGE_ID, messageId);
            getApplicationContext().sendBroadcast(intent);

            Log.d(TAG, "Broadcast enviado para WhatsApp al número: " + phoneNumber);
            updateMessageStatus(messageId, "Programado");

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error al programar mensaje para WhatsApp: " + e.getMessage());
            updateMessageStatus(messageId, "Error");
            return Result.failure();
        }
    }

    private void updateMessageStatus(int messageId, String status) {
        if (messageId != -1) {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                new Thread(() -> {
                    MessageEntity message = db.messageDao().getMessageById(messageId);
                    if (message != null) {
                        message.status = status;
                        db.messageDao().update(message);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar estado del mensaje: " + e.getMessage());
            }
        }
    }
}
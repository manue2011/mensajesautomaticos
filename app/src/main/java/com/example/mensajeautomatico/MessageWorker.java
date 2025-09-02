package com.example.mensajeautomatico;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker para programar el envío de un mensaje en segundo plano.
 * Envía un broadcast local a MiServicioDeAccesibilidad cuando la tarea se ejecuta.
 */
public class MessageWorker extends Worker {

    private static final String TAG = "MessageWorker";
    public static final String ACTION_SEND_MESSAGE = "com.example.mensajeautomatico.SEND_MESSAGE";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_MESSAGE_TEXT = "message_text";

    public MessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String phoneNumber = getInputData().getString(EXTRA_PHONE_NUMBER);
        String messageText = getInputData().getString(EXTRA_MESSAGE_TEXT);

        Log.d(TAG, "Iniciando doWork para el número: " + phoneNumber);

        if (phoneNumber == null || messageText == null) {
            Log.e(TAG, "Datos de entrada nulos. No se puede enviar el mensaje.");
            return Result.failure();
        }

        try {
            // Envía un broadcast local al servicio de accesibilidad.
            Intent intent = new Intent(ACTION_SEND_MESSAGE);
            intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
            intent.putExtra(EXTRA_MESSAGE_TEXT, messageText);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            Log.d(TAG, "Broadcast enviado al servicio de accesibilidad.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar el broadcast: " + e.getMessage());
            return Result.failure();
        }
    }
}

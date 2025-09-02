package com.example.mensajeautomatico;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver para manejar eventos del sistema, como el reinicio del dispositivo,
 * para reprogramar los mensajes pendientes.
 */
public class MessageReceiver extends BroadcastReceiver {

    private static final String TAG = "MessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Dispositivo reiniciado. Reprogramando mensajes...");
            // Lógica para reprogramar mensajes pendientes.
            // Esto implicaría una base de datos para almacenar los mensajes programados.
        }
    }
}

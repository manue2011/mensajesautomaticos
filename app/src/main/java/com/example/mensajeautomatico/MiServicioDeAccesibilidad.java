package com.example.mensajeautomatico;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de Accesibilidad para automatizar el envío de mensajes en WhatsApp.
 * Recibe datos de un MessageWorker y simula interacciones de usuario.
 */
public class MiServicioDeAccesibilidad extends AccessibilityService {

    private static final String TAG = "MiServicioAccesibilidad";
    private String phoneNumber;
    private String messageText;
    private int messageId;
    private AtomicBoolean isMessageScheduled = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());

    // Definir las constantes aquí si no están disponibles en MessageWorker
    public static final String ACTION_SEND_MESSAGE = "com.example.mensajeautomatico.SEND_MESSAGE";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_MESSAGE_TEXT = "message_text";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    // BroadcastReceiver para manejar datos del Worker.
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_SEND_MESSAGE.equals(intent.getAction())) {
                phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
                messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT);
                messageId = intent.getIntExtra(EXTRA_MESSAGE_ID, -1);
                Log.d(TAG, "Mensaje recibido para el número: " + phoneNumber + ", ID: " + messageId);
                if (phoneNumber != null && messageText != null && !isMessageScheduled.get()) {
                    isMessageScheduled.set(true);
                    openWhatsApp();
                }
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isMessageScheduled.get()) {
            return;
        }

        // Se detiene si el evento proviene de una aplicación que no es WhatsApp
        if (event.getPackageName() != null && !event.getPackageName().toString().equals("com.whatsapp")) {
            return;
        }

        // Si la ventana ha cambiado (por ejemplo, ha entrado al chat), intenta enviar el mensaje.
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Evento TYPE_WINDOW_STATE_CHANGED en WhatsApp.");
            handler.postDelayed(() -> {
                Log.d(TAG, "Buscando elementos de la UI.");
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    // Busca el campo de texto del mensaje.
                    List<AccessibilityNodeInfo> messageBoxes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry");
                    // Busca el botón de enviar.
                    List<AccessibilityNodeInfo> sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");

                    if (!messageBoxes.isEmpty() && !sendButtons.isEmpty()) {
                        AccessibilityNodeInfo messageBox = messageBoxes.get(0);
                        AccessibilityNodeInfo sendButton = sendButtons.get(0);

                        Log.d(TAG, "Caja de texto y botón de enviar encontrados.");

                        // Escribir el mensaje en el campo de texto
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageText);
                        messageBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                        // Esperar un momento antes de enviar
                        handler.postDelayed(() -> {
                            Log.d(TAG, "Botón de enviar encontrado. Enviando mensaje...");
                            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            // Una vez enviado, restablece el estado del servicio.
                            isMessageScheduled.set(false);
                            Log.d(TAG, "Mensaje enviado. Servicio restablecido.");

                            // Actualizar estado en base de datos
                            updateMessageStatusInDatabase(true);
                        }, 1000);
                    } else {
                        Log.d(TAG, "Elementos de WhatsApp no encontrados.");
                    }
                } else {
                    Log.d(TAG, "Nodo raíz no disponible.");
                }
            }, 2000); // Mayor retraso para dar tiempo a que WhatsApp cargue completamente
        }
    }

    /**
     * Abre la aplicación de WhatsApp con el número y mensaje precargados.
     * Usamos una URI "whatsapp://send" que es más fiable para la automatización
     * que la URI web "wa.me".
     */
    private void openWhatsApp() {
        try {
            Log.d(TAG, "Abriendo WhatsApp con el número: " + phoneNumber);
            String url = "whatsapp://send?phone=" + phoneNumber + "&text=" + URLEncoder.encode(messageText, "UTF-8");
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(url));
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(whatsappIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir WhatsApp: " + e.getMessage());
            isMessageScheduled.set(false);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Asegúrate de desregistrar el receptor al destruir el servicio.
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error al desregistrar el receptor: " + e.getMessage());
        }

        // Guardar estado en SharedPreferences (servicio desactivado)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("accessibility_enabled", false).apply();
        Log.d(TAG, "Estado de accesibilidad guardado como false en SharedPreferences");

        Log.d(TAG, "Servicio de accesibilidad destruido. BroadcastReceiver dado de baja.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Servicio de accesibilidad CONECTADO correctamente");

        // Guardar estado en SharedPreferences (servicio activado)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("accessibility_enabled", true).apply();
        Log.d(TAG, "Estado de accesibilidad guardado como true en SharedPreferences");

        // Enviar broadcast para notificar que el servicio está activo
        sendAccessibilityStatusBroadcast(true);

        // Registrar el BroadcastReceiver para recibir los mensajes del Worker.
        try {
            IntentFilter filter = new IntentFilter(ACTION_SEND_MESSAGE);
            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter);
            Log.d(TAG, "BroadcastReceiver registrado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar BroadcastReceiver: " + e.getMessage());
        }
    }

    /**
     * Envía un broadcast para notificar el estado del servicio de accesibilidad.
     */
    private void sendAccessibilityStatusBroadcast(boolean isEnabled) {
        try {
            Intent intent = new Intent("com.example.mensajeautomatico.ACCESSIBILITY_STATUS");
            intent.putExtra("enabled", isEnabled);
            sendBroadcast(intent);
            Log.d(TAG, "Broadcast enviado: " + isEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error enviando broadcast: " + e.getMessage());
        }
    }

    private void updateMessageStatusInDatabase(boolean success) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                if (messageId != -1) {
                    MessageEntity message = db.messageDao().getMessageById(messageId);
                    if (message != null) {
                        message.status = success ? "Enviado" : "Error";
                        db.messageDao().update(message);
                        Log.d(TAG, "Estado del mensaje actualizado a: " + message.status);
                    } else {
                        Log.d(TAG, "No se encontró el mensaje con ID: " + messageId);
                    }
                } else {
                    Log.d(TAG, "ID de mensaje no válido, no se puede actualizar la base de datos");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar base de datos: " + e.getMessage());
            }
        }).start();
    }
}
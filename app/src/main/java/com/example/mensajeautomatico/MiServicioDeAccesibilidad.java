package com.example.mensajeautomatico;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
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
    private AtomicBoolean isMessageScheduled = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());

    // BroadcastReceiver para manejar datos del Worker.
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MessageWorker.ACTION_SEND_MESSAGE.equals(intent.getAction())) {
                phoneNumber = intent.getStringExtra(MessageWorker.EXTRA_PHONE_NUMBER);
                messageText = intent.getStringExtra(MessageWorker.EXTRA_MESSAGE_TEXT);
                Log.d(TAG, "Mensaje recibido para el número: " + phoneNumber);
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
                        // Llenar la caja de texto con el mensaje.
                        if (messageBox.isEnabled()) {
                            // En esta versión, no se usa performAction para pegar el texto,
                            // ya que la URI de WhatsApp ya lo precarga.
                            // Solo se busca y se hace clic en el botón de enviar.
                            Log.d(TAG, "Botón de enviar encontrado. Enviando mensaje...");
                            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            // Una vez enviado, restablece el estado del servicio.
                            isMessageScheduled.set(false);
                            Log.d(TAG, "Mensaje enviado. Servicio restablecido.");
                        } else {
                            Log.d(TAG, "La caja de texto no está habilitada.");
                        }
                    } else {
                        Log.d(TAG, "Caja de texto del mensaje no encontrada. No estamos en la pantalla de chat.");
                    }
                } else {
                    Log.d(TAG, "Caja de texto del mensaje encontrada, pero el botón de enviar no. Esto es inesperado.");
                }
            }, 1000); // Pequeño retraso para dar tiempo a que la UI se cargue.
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
            IntentFilter filter = new IntentFilter(MessageWorker.ACTION_SEND_MESSAGE);
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
}
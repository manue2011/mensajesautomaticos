package com.example.mensajeautomatico;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import androidx.annotation.NonNull;
import android.annotation.SuppressLint;

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
    private AtomicBoolean isWhatsAppOpened = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());

    // Constantes para el Broadcast del MessageWorker
    public static final String ACTION_SEND_MESSAGE = "com.example.mensajeautomatico.SEND_MESSAGE";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_MESSAGE_TEXT = "message_text";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    // Un BroadcastReceiver para recibir la señal del MessageWorker
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_SEND_MESSAGE.equals(intent.getAction())) {
                phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
                messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT);
                messageId = intent.getIntExtra(EXTRA_MESSAGE_ID, -1);
                isMessageScheduled.set(true);
                Log.d(TAG, "Mensaje recibido del worker. Preparando envío a: " + phoneNumber);
                openWhatsAppAndSendMessage();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Servicio de Accesibilidad conectado.");

        // FIX: Se agrega el flag de seguridad a partir de Android S (API 31)
        IntentFilter filter = new IntentFilter(ACTION_SEND_MESSAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(messageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(messageReceiver, filter);
        }

        sendAccessibilityStatusBroadcast(true);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isMessageScheduled.get() || !isWhatsAppOpened.get()) {
            return;
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
            Log.d(TAG, "Evento de accesibilidad en WhatsApp detectado: " + event.getEventType());

            // Esperar a que la ventana esté estable y luego procesar el mensaje
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handler.postDelayed(() -> {
                    performActionForMessage();
                }, 2000); // Esperar 2 segundos para que WhatsApp cargue completamente
            }
        }
    }

    private void openWhatsAppAndSendMessage() {
        if (phoneNumber == null || messageText == null) {
            Log.e(TAG, "Número de teléfono o mensaje nulos.");
            updateMessageStatusInDatabase(false);
            isMessageScheduled.set(false);
            return;
        }
        try {
            String whatsappUrl = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + URLEncoder.encode(messageText, "UTF-8");
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(whatsappUrl));
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(whatsappIntent);
            Log.d(TAG, "Abriendo WhatsApp con el chat de: " + phoneNumber);

            // Marcar que WhatsApp se está abriendo
            isWhatsAppOpened.set(true);

            // Configurar un timeout por si WhatsApp no se abre correctamente
            handler.postDelayed(() -> {
                if (isMessageScheduled.get()) {
                    Log.e(TAG, "Timeout: WhatsApp no se abrió correctamente.");
                    updateMessageStatusInDatabase(false);
                    isMessageScheduled.set(false);
                    isWhatsAppOpened.set(false);
                }
            }, 10000); // 10 segundos de timeout

        } catch (Exception e) {
            Log.e(TAG, "Error al abrir WhatsApp: " + e.getMessage());
            updateMessageStatusInDatabase(false);
            isMessageScheduled.set(false);
            isWhatsAppOpened.set(false);
        }
    }

    /**
     * Esta función busca los nodos de la interfaz para escribir y enviar el mensaje.
     */
    private void performActionForMessage() {
        if (!isMessageScheduled.get()) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.d(TAG, "Nodo raíz nulo. Reintentando...");
            handler.postDelayed(() -> performActionForMessage(), 1000);
            return;
        }

        // 1. Encontrar el campo de texto del mensaje.
        AccessibilityNodeInfo messageNode = findNodeByResourceId(rootNode, "com.whatsapp:id/entry");
        if (messageNode == null) {
            messageNode = findNodeByClassName(rootNode, "android.widget.EditText");
            Log.d(TAG, "Buscando campo de texto por clase...");
        }

        if (messageNode == null) {
            Log.d(TAG, "Campo de texto no encontrado. Reintentando...");
            handler.postDelayed(() -> performActionForMessage(), 1000);
            return;
        }

        Log.d(TAG, "Campo de texto encontrado. Escribiendo mensaje...");
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageText);
        messageNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

        // Pequeña pausa después de escribir el texto
        handler.postDelayed(() -> {
            // 2. Encontrar y hacer clic en el botón de enviar.
            AccessibilityNodeInfo sendButton = findNodeByResourceId(rootNode, "com.whatsapp:id/send");
            if (sendButton == null) {
                sendButton = findNodeByContentDescription(rootNode, "Enviar");
                Log.d(TAG, "Buscando botón por descripción...");
            }
            if (sendButton == null) {
                sendButton = findNodeByContentDescription(rootNode, "Send");
                Log.d(TAG, "Buscando botón por descripción en inglés...");
            }

            if (sendButton != null && sendButton.isClickable()) {
                Log.d(TAG, "Botón de enviar encontrado. Haciendo clic...");
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                // Éxito - mensaje enviado
                isMessageScheduled.set(false);
                isWhatsAppOpened.set(false);
                updateMessageStatusInDatabase(true);

                // Volver atrás después de enviar
                handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 2000);
            } else {
                Log.e(TAG, "No se encontró el botón de enviar. Reintentando...");
                handler.postDelayed(() -> performActionForMessage(), 1000);
            }
        }, 1000); // Esperar 1 segundo después de escribir el texto
    }

    private AccessibilityNodeInfo findNodeByClassName(AccessibilityNodeInfo rootNode, String className) {
        if (rootNode == null) return null;
        return findNodeByViewTraversal(rootNode, node -> {
            return node.getClassName() != null && node.getClassName().toString().equals(className);
        });
    }

    private AccessibilityNodeInfo findNodeByResourceId(AccessibilityNodeInfo rootNode, String resourceId) {
        if (rootNode == null) return null;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(resourceId);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByContentDescription(AccessibilityNodeInfo rootNode, String description) {
        if (rootNode == null) return null;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(description);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getContentDescription() != null &&
                        node.getContentDescription().toString().equalsIgnoreCase(description)) {
                    return node;
                }
            }
        }
        return findNodeByViewTraversal(rootNode, node -> {
            return node.getContentDescription() != null &&
                    node.getContentDescription().toString().equalsIgnoreCase(description);
        });
    }

    @FunctionalInterface
    private interface NodeMatcher {
        boolean matches(AccessibilityNodeInfo node);
    }

    private AccessibilityNodeInfo findNodeByViewTraversal(AccessibilityNodeInfo rootNode, NodeMatcher matcher) {
        if (rootNode == null) {
            return null;
        }

        if (matcher.matches(rootNode)) {
            return rootNode;
        }

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo found = findNodeByViewTraversal(child, matcher);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Servicio de Accesibilidad interrumpido.");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "Servicio de Accesibilidad reconectado.");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Servicio de Accesibilidad desvinculado.");
        if (messageReceiver != null) {
            try {
                unregisterReceiver(messageReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error al desregistrar el BroadcastReceiver: " + e.getMessage());
            }
        }
        sendAccessibilityStatusBroadcast(false);
        return super.onUnbind(intent);
    }

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
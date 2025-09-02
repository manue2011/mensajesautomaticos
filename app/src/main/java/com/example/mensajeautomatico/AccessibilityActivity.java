package com.example.mensajeautomatico;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Esta actividad solicita al usuario que active los permisos de accesibilidad.
 */
public class AccessibilityActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 2000; // 2 segundos de retraso para verificar
    private static final int MAX_RETRIES = 20; // Aumentamos los intentos
    private Handler handler = new Handler();
    private int retryCount = 0;
    private static final String TAG = "AccessibilityActivity";

    // Variables para el BroadcastReceiver
    private BroadcastReceiver accessibilityStatusReceiver;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessibility);

        Button btnActivateAccessibility = findViewById(R.id.btn_activate_accessibility);
        Button btnCheckAgain = findViewById(R.id.btn_check_again);

        // Inicializar el BroadcastReceiver
        accessibilityStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.mensajeautomatico.ACCESSIBILITY_STATUS".equals(intent.getAction())) {
                    boolean isEnabled = intent.getBooleanExtra("enabled", false);
                    if (isEnabled) {
                        Log.d(TAG, "Broadcast recibido: Servicio de accesibilidad activo");
                        redirectToMainActivity();
                    }
                }
            }
        };

        btnActivateAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Abre la configuración de accesibilidad del sistema.
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        btnCheckAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAccessibilityStatus();
            }
        });

        // Verificar inmediatamente al crear la actividad
        checkAccessibilityStatus();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        // Reiniciar el contador de reintentos
        retryCount = 0;

        // Registrar el receptor de broadcast
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter("com.example.mensajeautomatico.ACCESSIBILITY_STATUS");

            // Para Android 13+ (API nivel 33), se requiere especificar el flag de exportación
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(accessibilityStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Para versiones anteriores, usar el método sin flag
                registerReceiver(accessibilityStatusReceiver, filter);
            }

            isReceiverRegistered = true;
            Log.d(TAG, "Receptor de broadcast registrado");
        }

        // Verificar el estado de accesibilidad después de un breve retraso
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAccessibilityStatus();
            }
        }, DELAY_MILLIS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remover cualquier callback pendiente del handler
        handler.removeCallbacksAndMessages(null);

        // Desregistrar el receptor de broadcast
        if (isReceiverRegistered) {
            unregisterReceiver(accessibilityStatusReceiver);
            isReceiverRegistered = false;
            Log.d(TAG, "Receptor de broadcast desregistrado");
        }
    }

    private void checkAccessibilityStatus() {
        if (isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Servicio de accesibilidad detectado. Redirigiendo...");
            redirectToMainActivity();
        } else {
            retryCount++;
            Log.d(TAG, "Servicio de accesibilidad no detectado. Intentos: " + retryCount);

            if (retryCount < MAX_RETRIES) {
                // Reintentar después de un breve período
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkAccessibilityStatus();
                    }
                }, DELAY_MILLIS);
            } else {
                Toast.makeText(this, "Por favor, activa la accesibilidad para continuar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void redirectToMainActivity() {
        Toast.makeText(this, "Accesibilidad activada. Redirigiendo a la pantalla principal.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AccessibilityActivity.this, MainActivity.class);
        startActivity(intent);
        finish();  // Cierra esta actividad para no volver atrás
    }

    /**
     * Verifica si el servicio de accesibilidad está activo.
     * Usa múltiples métodos para mayor confiabilidad.
     */
    private boolean isAccessibilityServiceEnabled() {
        // Primero verificar mediante SharedPreferences (más rápido)
        if (isAccessibilityServiceEnabledByPreferences()) {
            return true;
        }

        // Luego verificar mediante los métodos del sistema
        return isAccessibilityServiceEnabledByManager() || isAccessibilityServiceEnabledBySettings();
    }

    /**
     * Método 0: Verificación mediante SharedPreferences (más rápido)
     */
    private boolean isAccessibilityServiceEnabledByPreferences() {
        try {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean isEnabled = prefs.getBoolean("accessibility_enabled", false);
            if (isEnabled) {
                Log.d(TAG, "Servicio encontrado mediante SharedPreferences");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar accesibilidad mediante Preferences: " + e.getMessage());
        }
        return false;
    }

    /**
     * Método 1: Verificación a través de AccessibilityManager
     */
    private boolean isAccessibilityServiceEnabledByManager() {
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) {
                Log.e(TAG, "AccessibilityManager es nulo");
                return false;
            }

            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

            // Múltiples formatos posibles del ID del servicio
            String myServiceId1 = getPackageName() + "/" + MiServicioDeAccesibilidad.class.getName();
            String myServiceId2 = getPackageName() + "/." + MiServicioDeAccesibilidad.class.getSimpleName();
            String myServiceId3 = getPackageName() + "/.MiServicioDeAccesibilidad";

            Log.d(TAG, "Buscando servicio con IDs: " + myServiceId1 + ", " + myServiceId2 + ", " + myServiceId3);
            Log.d(TAG, "Número de servicios habilitados: " + enabledServices.size());

            for (AccessibilityServiceInfo serviceInfo : enabledServices) {
                String serviceId = serviceInfo.getId();
                Log.d(TAG, "Servicio habilitado: " + serviceId);

                if (serviceId != null && (
                        serviceId.equalsIgnoreCase(myServiceId1) ||
                                serviceId.equalsIgnoreCase(myServiceId2) ||
                                serviceId.equalsIgnoreCase(myServiceId3) ||
                                serviceId.contains("MiServicioDeAccesibilidad"))) {
                    Log.d(TAG, "Servicio encontrado mediante AccessibilityManager: " + serviceId);

                    // Guardar en preferences para futuras verificaciones rápidas
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("accessibility_enabled", true).apply();

                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar accesibilidad mediante Manager: " + e.getMessage());
        }

        return false;
    }

    /**
     * Método 2: Verificación directa de los ajustes del sistema
     */
    private boolean isAccessibilityServiceEnabledBySettings() {
        try {
            // Esperar un momento para que el sistema actualice la configuración
            Thread.sleep(500);

            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            // Múltiples formatos posibles del ID del servicio
            String myServiceId1 = getPackageName() + "/" + MiServicioDeAccesibilidad.class.getName();
            String myServiceId2 = getPackageName() + "/." + MiServicioDeAccesibilidad.class.getSimpleName();
            String myServiceId3 = getPackageName() + "/.MiServicioDeAccesibilidad";

            Log.d(TAG, "Buscando servicio en ajustes: " + myServiceId1);
            Log.d(TAG, "ENABLED_ACCESSIBILITY_SERVICES: " + settingValue);

            if (settingValue != null) {
                // La cadena podría usar diferentes separadores en diferentes versiones de Android
                String[] enabledServices = settingValue.split(":|;");
                for (String service : enabledServices) {
                    Log.d(TAG, "Servicio en ajustes: " + service);

                    if (service.equalsIgnoreCase(myServiceId1) ||
                            service.equalsIgnoreCase(myServiceId2) ||
                            service.equalsIgnoreCase(myServiceId3) ||
                            service.contains("MiServicioDeAccesibilidad")) {
                        Log.d(TAG, "Servicio encontrado en ajustes del sistema");

                        // Guardar en preferences para futuras verificaciones rápidas
                        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("accessibility_enabled", true).apply();

                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar accesibilidad mediante Settings: " + e.getMessage());
        }

        return false;
    }
}
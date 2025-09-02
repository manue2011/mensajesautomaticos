package com.example.mensajeautomatico;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;

import java.util.List;

/**
 * Actividad principal que maneja el flujo de la aplicación.
 * 1. Verifica el estado de login.
 * 2. Si no está logueado, muestra la pantalla de login.
 * 3. Si está logueado, muestra la pantalla principal con opciones para activar accesibilidad, ver historial o programar mensajes.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private static final String PREF_ACCESSIBILITY_ENABLED = "accessibility_enabled";

    private Button enableAccessibilityButton;
    private Button goToDashboardButton;
    private Button scheduleMessageButton;

    // Declaración de variables para la pantalla de login
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lógica de verificación de login
        boolean isLoggedIn = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean(PREF_USER_LOGGED_IN, false);

        if (!isLoggedIn) {
            // Si el usuario no está logueado, redirige a la actividad de login.
            setContentView(R.layout.activity_login);
            setupLoginScreen();
        } else {
            // Si el usuario ya está logueado, muestra la pantalla principal.
            setupMainScreen();
        }
    }

    /**
     * Configura la pantalla de login.
     */
    private void setupLoginScreen() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            // Validación simple del login. En una app real, esto sería una llamada a una API.
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(MainActivity.this, "Por favor, ingresa correo y contraseña.", Toast.LENGTH_SHORT).show();
            } else {
                // Simula un login exitoso
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean(PREF_USER_LOGGED_IN, true).apply();
                Toast.makeText(MainActivity.this, "Login exitoso.", Toast.LENGTH_SHORT).show();
                // Navega a AccessibilityActivity
                Intent intent = new Intent(MainActivity.this, AccessibilityActivity.class);
                startActivity(intent);
                finish(); // Cierra MainActivity para evitar volver atrás
            }
        });
    }

    /**
     * Configura la pantalla principal.
     */
    private void setupMainScreen() {
        setContentView(R.layout.activity_main);

        enableAccessibilityButton = findViewById(R.id.enable_accessibility_button);
        goToDashboardButton = findViewById(R.id.go_to_dashboard_button);
        scheduleMessageButton = findViewById(R.id.schedule_message_button);

        enableAccessibilityButton.setOnClickListener(v -> {
            // Abre la configuración de accesibilidad para que el usuario active el servicio.
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        goToDashboardButton.setOnClickListener(v -> {
            // Navega a DashboardActivity (Historial)
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
        });

        scheduleMessageButton.setOnClickListener(v -> {
            // Navega a ScheduleMessageActivity (Programar Mensaje)
            Intent intent = new Intent(MainActivity.this, ScheduleMessageActivity.class);
            startActivity(intent);
        });

        // El estado de los botones se actualizará en onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Log para saber que onResume() ha sido llamado
        Log.d(TAG, "onResume() ha sido llamado.");

        // Solo ejecuta esta lógica si la vista actual es activity_main
        if (findViewById(R.id.enable_accessibility_button) != null) {
            // Verifica el estado de accesibilidad cada vez que la actividad se reanuda.
            boolean isEnabled = isAccessibilityServiceEnabled();
            Log.d(TAG, "Estado de accesibilidad: " + isEnabled);

            if (isEnabled) {
                Toast.makeText(this, "Servicio de accesibilidad activado.", Toast.LENGTH_SHORT).show();
                enableAccessibilityButton.setVisibility(View.GONE);
                goToDashboardButton.setVisibility(View.VISIBLE);
                scheduleMessageButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Servicio de accesibilidad no activado.", Toast.LENGTH_SHORT).show();
                enableAccessibilityButton.setVisibility(View.VISIBLE);
                goToDashboardButton.setVisibility(View.GONE);
                scheduleMessageButton.setVisibility(View.GONE);
            }
        }
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
     * Método 1: Verificación mediante SharedPreferences (más rápido)
     */
    private boolean isAccessibilityServiceEnabledByPreferences() {
        try {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean isEnabled = prefs.getBoolean(PREF_ACCESSIBILITY_ENABLED, false);
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
     * Método 2: Verificación a través de AccessibilityManager
     */
    private boolean isAccessibilityServiceEnabledByManager() {
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) {
                Log.e(TAG, "AccessibilityManager es nulo.");
                return false;
            }

            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

            // Define el nombre de tu servicio
            String myServiceId = getPackageName() + "/" + MiServicioDeAccesibilidad.class.getName();
            Log.d(TAG, "Buscando el servicio con ID: " + myServiceId);

            // Itera sobre la lista para encontrar tu servicio
            for (AccessibilityServiceInfo serviceInfo : enabledServices) {
                if (myServiceId.equals(serviceInfo.getId())) {
                    Log.d(TAG, "Servicio de accesibilidad habilitado: " + myServiceId);

                    // Guardar en preferences para futuras verificaciones rápidas
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean(PREF_ACCESSIBILITY_ENABLED, true).apply();

                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar accesibilidad mediante Manager: " + e.getMessage());
        }

        Log.d(TAG, "Servicio de accesibilidad NO habilitado. ID de servicio no encontrado.");
        return false;
    }

    /**
     * Método 3: Verificación directa de los ajustes del sistema
     */
    private boolean isAccessibilityServiceEnabledBySettings() {
        try {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            String myServiceId = getPackageName() + "/" + MiServicioDeAccesibilidad.class.getName();
            Log.d(TAG, "Buscando servicio en ajustes: " + myServiceId);

            if (settingValue != null) {
                // La cadena podría usar diferentes separadores en diferentes versiones de Android
                String[] enabledServices = settingValue.split(":|;");
                for (String service : enabledServices) {
                    if (service.equals(myServiceId)) {
                        Log.d(TAG, "Servicio encontrado en ajustes del sistema");

                        // Guardar en preferences para futuras verificaciones rápidas
                        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean(PREF_ACCESSIBILITY_ENABLED, true).apply();

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
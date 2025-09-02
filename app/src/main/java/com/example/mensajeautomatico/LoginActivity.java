package com.example.mensajeautomatico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad para el inicio de sesión del usuario.
 * Permite al usuario ingresar sus credenciales para acceder a la aplicación.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private TextView tvRegisterPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views with the correct IDs from the XML
        etEmail = findViewById(R.id.email_edit_text);
        etPassword = findViewById(R.id.password_edit_text);
        btnLogin = findViewById(R.id.login_button);
        btnRegister = findViewById(R.id.register_button);
        tvRegisterPrompt = findViewById(R.id.tv_register_prompt);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navegar a la actividad de registro
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        tvRegisterPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // También permitir registro al hacer clic en el texto
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu correo y contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aquí iría la validación real con base de datos o API
        // Por ahora, simulamos un login exitoso
        Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();

        // Guardar estado de login
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putBoolean("user_logged_in", true)
                .apply();

        // Navegar a la actividad de accesibilidad
        Intent intent = new Intent(LoginActivity.this, AccessibilityActivity.class);
        startActivity(intent);

        finish();  // cerrar esta actividad y evitar volver atrás
    }
}
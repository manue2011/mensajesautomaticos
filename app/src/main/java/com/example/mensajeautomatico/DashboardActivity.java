package com.example.mensajeautomatico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad que maneja el panel de control del usuario.
 * Muestra el historial de mensajes y opciones para programar.
 */
public class DashboardActivity extends AppCompatActivity {

    private Button btnProgramarMensaje;
    private RecyclerView recyclerViewHistorial;
    private TextView tvEmptyHistory;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();  // Simula lista, integra DB después

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnProgramarMensaje = findViewById(R.id.btn_programar_mensaje);
        recyclerViewHistorial = findViewById(R.id.recycler_view_historial);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);

        // Configura RecyclerView
        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, messageList);
        recyclerViewHistorial.setAdapter(messageAdapter);

        // Cargar historial automáticamente
        loadMessageHistory();

        btnProgramarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, ScheduleMessageActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar historial cuando la actividad se reanuda
        loadMessageHistory();
    }

    /**
     * Carga el historial de mensajes
     */
    private void loadMessageHistory() {
        // Limpiar lista actual
        messageList.clear();

        // Aquí iría la lógica para cargar mensajes de la base de datos
        // Por ahora, simulamos algunos mensajes
        messageList.add(new Message("+123456789", "Mensaje de prueba 1", "2023-10-01 10:00", "Enviado"));
        messageList.add(new Message("+987654321", "Mensaje de prueba 2", "2023-10-02 14:30", "Pendiente"));
        messageList.add(new Message("+555555555", "Mensaje de prueba 3", "2023-10-03 09:15", "Enviado"));

        // Actualizar el adaptador
        messageAdapter.notifyDataSetChanged();

        // Mostrar u ocultar mensaje de historial vacío
        if (messageList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            recyclerViewHistorial.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            recyclerViewHistorial.setVisibility(View.VISIBLE);
        }
    }
}
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad que maneja el panel de control del usuario.
 * Muestra el historial de mensajes y opciones para programar.
 */
public class DashboardActivity extends AppCompatActivity {

    private Button btnProgramarMensaje;
    private RecyclerView recyclerViewHistorial;
    private TextView tvEmptyHistory;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnProgramarMensaje = findViewById(R.id.btn_programar_mensaje);
        recyclerViewHistorial = findViewById(R.id.recycler_view_historial);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);

        // Inicializar la base de datos
        db = AppDatabase.getDatabase(this);

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
        new Thread(() -> {
            List<MessageEntity> messages = db.messageDao().getAllMessages();
            runOnUiThread(() -> {
                messageList.clear();
                for (MessageEntity entity : messages) {
                    // Convertir Entity a Modelo
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    String timestamp = sdf.format(new Date(entity.scheduledTime));
                    messageList.add(new Message(entity.phoneNumber, entity.messageText, timestamp, entity.status));
                }
                messageAdapter.notifyDataSetChanged();

                if (messageList.isEmpty()) {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    recyclerViewHistorial.setVisibility(View.GONE);
                } else {
                    tvEmptyHistory.setVisibility(View.GONE);
                    recyclerViewHistorial.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
}
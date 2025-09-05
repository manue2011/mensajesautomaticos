package com.example.mensajeautomatico;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Actividad para la programación de mensajes.
 * Permite al usuario seleccionar un destinatario, escribir un mensaje,
 * y elegir la fecha y hora para su envío.
 */
public class ScheduleMessageActivity extends AppCompatActivity {

    private EditText etPhoneNumber;
    private EditText etMessageText;
    private Button btnSelectDate;
    private Button btnSelectTime;
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private Button btnSchedule;

    private AppDatabase db;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_message);

        // Inicializar la base de datos
        db = AppDatabase.getDatabase(this);

        // Inicializar vistas
        etPhoneNumber = findViewById(R.id.et_phone_number);
        etMessageText = findViewById(R.id.et_message_text);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
        btnSchedule = findViewById(R.id.btn_schedule_message);

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSchedule.setOnClickListener(v -> scheduleMessage());

        // Inicializar con la fecha y hora actuales
        final Calendar c = Calendar.getInstance();
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        selectedDay = c.get(Calendar.DAY_OF_MONTH);
        selectedHour = c.get(Calendar.HOUR_OF_DAY);
        selectedMinute = c.get(Calendar.MINUTE);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = monthOfYear;
                    selectedDay = dayOfMonth;
                    tvSelectedDate.setText(String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay));
                }, selectedYear, selectedMonth, selectedDay);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Evitar fechas pasadas
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    tvSelectedTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
                }, selectedHour, selectedMinute, true); // <-- ESTE CAMBIO VUELVE AL FORMATO 24H
        timePickerDialog.show();
    }

    private void scheduleMessage() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String messageText = etMessageText.getText().toString().trim();
        if (phoneNumber.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un número y un mensaje.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
        if (selectedTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "La fecha y hora deben ser futuras.", Toast.LENGTH_SHORT).show();
            return;
        }

        long scheduledTimeMillis = selectedTime.getTimeInMillis();
        MessageEntity message = new MessageEntity(phoneNumber, messageText, scheduledTimeMillis, "Programado");

        new Thread(() -> {
            // CAMBIO CLAVE: Capturar el ID devuelto por la inserción
            long newId = db.messageDao().insert(message);

            Data inputData = new Data.Builder()
                    .putString(MessageWorker.EXTRA_PHONE_NUMBER, phoneNumber)
                    .putString(MessageWorker.EXTRA_MESSAGE_TEXT, messageText)
                    .putInt(MessageWorker.EXTRA_MESSAGE_ID, (int) newId) // <- USAR EL NUEVO ID
                    .build();

            OneTimeWorkRequest messageWork = new OneTimeWorkRequest.Builder(MessageWorker.class)
                    .setInputData(inputData)
                    .setInitialDelay(scheduledTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .build();

            WorkManager.getInstance(this).enqueue(messageWork);

            runOnUiThread(() -> {
                Toast.makeText(this, "Mensaje programado correctamente.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
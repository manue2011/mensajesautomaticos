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
        btnSchedule = findViewById(R.id.btn_schedule);

        btnSelectDate.setOnClickListener(v -> showDatePickerDialog());
        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());
        btnSchedule.setOnClickListener(v -> scheduleMessage());
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedYear = year1;
                    selectedMonth = monthOfYear;
                    selectedDay = dayOfMonth;
                    tvSelectedDate.setText("Fecha: " + dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute1;
                    tvSelectedTime.setText("Hora: " + hourOfDay + ":" + String.format("%02d", minute1));
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void scheduleMessage() {
        String phoneNumber = etPhoneNumber.getText().toString();
        String messageText = etMessageText.getText().toString();

        if (phoneNumber.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
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
            db.messageDao().insert(message);
            Data inputData = new Data.Builder()
                    .putString(MessageWorker.EXTRA_PHONE_NUMBER, phoneNumber)
                    .putString(MessageWorker.EXTRA_MESSAGE_TEXT, messageText)
                    .putInt(MessageWorker.EXTRA_MESSAGE_ID, (int) message.id)
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
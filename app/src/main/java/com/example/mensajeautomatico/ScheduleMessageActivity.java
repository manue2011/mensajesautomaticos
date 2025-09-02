package com.example.mensajeautomatico;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

/**
 * Actividad para la programación de mensajes.
 * Permite al usuario seleccionar un destinatario, escribir un mensaje,
 * y elegir la fecha y hora para su envío.
 */
public class ScheduleMessageActivity extends AppCompatActivity {

    // Vistas del layout
    private EditText etPhoneNumber;
    private EditText etMessageText;
    private Button btnSelectDate;
    private Button btnSelectTime;
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private Button btnSchedule;

    // Variables para la fecha y hora seleccionadas
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_message);

        // Inicializar vistas
        etPhoneNumber = findViewById(R.id.et_phone_number);
        etMessageText = findViewById(R.id.et_message_text);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
        btnSchedule = findViewById(R.id.btn_schedule);

        // Configurar listeners de clics
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        btnSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        btnSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleMessage();
            }
        });
    }

    /**
     * Muestra un diálogo de selección de fecha.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        selectedYear = year;
                        selectedMonth = monthOfYear;
                        selectedDay = dayOfMonth;
                        tvSelectedDate.setText("Fecha: " + dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Muestra un diálogo de selección de hora.
     */
    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        tvSelectedTime.setText("Hora: " + hourOfDay + ":" + String.format("%02d", minute));
                    }
                }, hour, minute, false); // El último parámetro es para el formato de 24 horas
        timePickerDialog.show();
    }

    /**
     * Valida los campos y simula la programación del mensaje.
     */
    private void scheduleMessage() {
        String phoneNumber = etPhoneNumber.getText().toString();
        String messageText = etMessageText.getText().toString();

        if (phoneNumber.isEmpty() || messageText.isEmpty() || tvSelectedDate.getText().toString().isEmpty() || tvSelectedTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar fecha futura
        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
        if (selectedTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "La fecha y hora deben ser futuras.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simula programación (agrega WorkManager aquí más adelante)
        String scheduledDateTime = tvSelectedDate.getText() + " a las " + tvSelectedTime.getText();
        Toast.makeText(this, "Mensaje programado para: " + scheduledDateTime + " - Estado: Programado", Toast.LENGTH_LONG).show();

        // Simula guardado en historial (integra DB después)
        // Message newMessage = new Message(phoneNumber, messageText, scheduledDateTime + " - Programado");
        // Guarda en DB y regresa a Dashboard si quieres
        finish();  // Vuelve a la pantalla anterior (Dashboard o Main)
    }
}
package com.example.mensajeautomatico;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Inicializar la base de datos
        AppDatabase.getDatabase(this);
    }
}
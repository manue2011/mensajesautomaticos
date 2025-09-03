plugins {
    // Gradle Version Catalog
    alias(libs.plugins.android.application)
    // El plugin de Google Services es esencial para que Firebase funcione.
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.mensajeautomatico"
    // Usar la versión del SDK recomendada
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mensajeautomatico"
        // La minSdkVersion es correcta (23) para las bibliotecas de Firebase
        minSdk = 23
        // targetSdk 34 es la versión más reciente, recomendada para las tiendas de aplicaciones
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Usar Java 1.8 para compatibilidad, ya que 11 no es necesario para este tipo de aplicación
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Dependencias de androidx
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // Dependencias para las pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Agregamos la dependencia para ListenableFuture de Guava.
    // Esta es una solución común para el error "class file not found".
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict")

    // Declaramos la plataforma Firebase BOM. Esto hace que las versiones de todas las dependencias de Firebase sean compatibles.
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Ahora, agregamos las dependencias de Firebase SIN ESPECIFICAR LA VERSIÓN.
    // El BOM se encarga de usar las versiones correctas y compatibles entre sí.
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")

    // Esta es la dependencia crucial para el adaptador de RecyclerView de Firestore.
    // La versión 8.0.2 es la más compatible y estable en este momento.
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("androidx.work:work-runtime:2.9.0")
    // WorkManager
    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // CardView, necesario para el diseño de tu mensaje en el RecyclerView
    implementation("androidx.cardview:cardview:1.0.0")

    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.work:work-runtime:2.8.1")
}
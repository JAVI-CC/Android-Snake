package com.example.javi.snake;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

public class SnakeActivity extends Activity {

    // Declarar una instancia de SnakeEngine
    SnakeEngine snakeEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtenga las dimensiones en píxeles de la pantalla
        Display display = getWindowManager().getDefaultDisplay();

        // Inicializa el resultado en un objeto Point
        Point size = new Point();
        display.getSize(size);

        // Crea una nueva instancia de la clase SnakeEngine
        snakeEngine = new SnakeEngine(this, size);

        // Convertir snakeEngine en la vista de la actividad
        setContentView(snakeEngine);
    }

    // Inicia el hilo en snakeEngine
    @Override
    protected void onResume() {
        super.onResume();
        snakeEngine.resume();
    }

    // Detener el hilo en snakeEngine
    @Override
    protected void onPause() {
        super.onPause();
        snakeEngine.pause();
    }
}
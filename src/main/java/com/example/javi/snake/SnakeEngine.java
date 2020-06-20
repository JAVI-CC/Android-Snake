package com.example.javi.snake;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

class SnakeEngine extends SurfaceView implements Runnable {

    // Nuestro hilo de juego para el bucle principal del juego
    private Thread thread = null;

    // Para mantener una referencia a la Actividad
    private Context context;

    // para reproducir efectos de sonido
    private SoundPool soundPool;
    private int eat_bob = -1;
    private int snake_crash = -1;

    // Para seguir el rumbo del movimiento
    public enum Heading {UP, RIGHT, DOWN, LEFT}
    // Comience dirigiéndose a la derecha
    private Heading heading = Heading.RIGHT;

    // Para mantener el tamaño de la pantalla en píxeles
    private int screenX;
    private int screenY;

    // ¿Cuánto mide la serpiente?
    private int snakeLength;

    // ¿Dónde se esconde Bob?
    private int bobX;
    private int bobY;

    // El tamaño en píxeles de un segmento de serpiente
    private int blockSize;

    // El tamaño en segmentos del área jugable
    private final int NUM_BLOCKS_WIDE = 40;
    private int numBlocksHigh;

    // Control de pausas entre actualizaciones
    private long nextFrameTime;
    // Actualiza el juego 10 veces por segundo
    private final long FPS = 10;
    // Hay 1000 milisegundos en un segundo
    private final long MILLIS_PER_SECOND = 1000;
    // Dibujaremos el marco mucho más seguido

    // Cuantos puntos tiene el jugador
    private int score;

    // La ubicación en la cuadrícula de todos los segmentos
    private int[] snakeXs;
    private int[] snakeYs;

    // Todoo lo que necesitamos para dibujar
    // ¿Está jugando el juego actualmente?
    private volatile boolean isPlaying;

    // Un lienzo para nuestra pintura
    private Canvas canvas;

    // Requerido para usar canvas
    private SurfaceHolder surfaceHolder;

    // Un poco de pintura para nuestro lienzo
    private Paint paint;

    public SnakeEngine(Context context, Point size) {
        super(context);

        context = context;

        screenX = size.x;
        screenY = size.y;

        // Calcula cuántos píxeles tiene cada bloque
        blockSize = screenX / NUM_BLOCKS_WIDE;
        // ¿Cuántos bloques del mismo tamaño caben en la altura?
        numBlocksHigh = screenY / blockSize;

        // Configura el sonido
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            // Crear objetos de las 2 clases requeridas
            // Usa m_Context porque esta es una referencia a la Actividad
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepara los dos sonidos en la memoria
            descriptor = assetManager.openFd("get_mouse_sound.ogg");
            eat_bob = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("death_sound.ogg");
            snake_crash = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Inicializa los objetos de dibujo
        surfaceHolder = getHolder();
        paint = new Paint();

        // ¡Si obtienes 200 puntos, eres recompensado con un logro de choque!
        snakeXs = new int[200];
        snakeYs = new int[200];

        // Iniciar el juego
        newGame();
    }

    @Override
    public void run() {

        while (isPlaying) {

            // Actualizar 10 veces por segundo
            if(updateRequired()) {
                update();
                draw();
            }

        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void newGame() {
        // Comienza con un solo segmento de serpiente
        snakeLength = 1;
        snakeXs[0] = NUM_BLOCKS_WIDE / 2;
        snakeYs[0] = numBlocksHigh / 2;

        // Prepara a Bob para la cena
        spawnBob();

        // Restablecer la puntuación
        score = 0;

        // Configurar nextFrameTime para que se active una actualización
        nextFrameTime = System.currentTimeMillis();
    }

    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    private void eatBob(){
        // ¡Le tengo!
        // Aumenta el tamaño de la serpiente
        snakeLength++;
        // reemplaza Bob
        // Esto me recuerda a Edge of Tomorrow. ¡Algún día Bob estará listo!
        spawnBob();
        // agregar a la partitura
        score = score + 1;
        soundPool.play(eat_bob, 1, 1, 0, 0, 1);
    }

    private void moveSnake(){
        // mueve el cuerpo
        for (int i = snakeLength; i > 0; i--) {
            // Comienza en la parte posterior y muévelo
            // a la posición del segmento delante de él
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];

            // Excluir la cabeza porque
            // la cabeza no tiene nada delante
        }

        // Mueve la cabeza en el encabezado apropiado
        switch (heading) {
            case UP:
                snakeYs[0]--;
                break;

            case RIGHT:
                snakeXs[0]++;
                break;

            case DOWN:
                snakeYs[0]++;
                break;

            case LEFT:
                snakeXs[0]--;
                break;
        }
    }

    private boolean detectDeath(){
        // ¿Ha muerto la serpiente?
        boolean dead = false;

        // Golpea el borde de la pantalla
        if (snakeXs[0] == -1) dead = true;
        if (snakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
        if (snakeYs[0] == -1) dead = true;
        if (snakeYs[0] == numBlocksHigh) dead = true;

        // ¿Ha comido?
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((i > 4) && (snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
            }
        }

        return dead;
    }

    public void update() {
        // ¿La cabeza de la serpiente se comió a Bob?
        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            //empezar de nuevo
            soundPool.play(snake_crash, 1, 1, 0, 0, 1);

            newGame();
        }
    }

    public void draw() {
        // Consigue un candado en el lienzo
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            // Llena la pantalla con Game Code School blue
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Establece el color de la pintura para dibujar la serpiente blanca
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Escala el texto de HUD
            paint.setTextSize(90);
            canvas.drawText("Score:" + score, 10, 70, paint);

            // Dibuja la serpiente un bloque a la vez
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize,
                        (snakeYs[i] * blockSize),
                        (snakeXs[i] * blockSize) + blockSize,
                        (snakeYs[i] * blockSize) + blockSize,
                        paint);
            }

            // Establece el color de la pintura para dibujar a Bob rojo
            paint.setColor(Color.argb(255, 255, 0, 0));

            // Dibuja a Bob
            canvas.drawRect(bobX * blockSize,
                    (bobY * blockSize),
                    (bobX * blockSize) + blockSize,
                    (bobY * blockSize) + blockSize,
                    paint);

            // Desbloquee el lienzo y muestre los gráficos para este marco
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public boolean updateRequired() {

        // ¿Debemos actualizar el marco?
        if(nextFrameTime <= System.currentTimeMillis()){
            // Ha pasado la décima de segundo

            // Configurar cuándo se activará la próxima actualización
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;

            // Devuelve true para que la actualización y el sorteo
            // las funciones se ejecutan
            return true;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= screenX / 2) {
                    switch(heading){
                        case UP:
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            heading = Heading.UP;
                            break;
                    }
                } else {
                    switch(heading){
                        case UP:
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading = Heading.UP;
                            break;
                    }
                }
        }
        return true;
    }

}
package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback,
                   Choreographer.FrameCallback,
                   Runnable,
                   SurfaceInfoDirector,
                   TouchObserver{

    private String TAG;
    private static final boolean DEBUG = false;

    private ArrayList<SurfaceInfoObserver> observers;

    private Physics physics;
    private Resources resources;
    private SurfaceHolder holder;
    private Choreographer choreographer;

    private SurfaceInfo surfaceInfo;
    private BitmapBoss bitmapBoss;

    private Thread drawThread;
    private boolean continueRendering;
    private long lastFrameStartTime;
    private boolean proceed;
    private boolean firstDraw;

    private boolean drawableObjectsReady;
    private boolean triggerDraw;

    public GameSurfaceView(Context context) {
        super(context);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(Context context, TouchDirector touchDirector) {
        TAG = getClass().getSimpleName();
        touchDirector.register(this);

        observers = new ArrayList<>();
        resources = getResources();
        holder = getHolder();
        holder.addCallback(this);
        choreographer = Choreographer.getInstance();
        physics = new Physics(this, touchDirector);
        bitmapBoss = new BitmapBoss(context, this);

        continueRendering = false;
        triggerDraw = false;

        proceed = false;
    }

    @Override
    public void run() {
        while (continueRendering) {
            if (!holder.getSurface().isValid()) {
                continue;
            }
            while (!triggerDraw) {
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

            // Physics update starts here.
            physics.updateObjects();
            float time = 0;
            if (DEBUG) {
                time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for frame physics == " + String.format("%.2f,", time));
            }


            // Main drawing routine starts here.
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.BLACK);

            physics.drawObjects(canvas);
            if (!proceed) {
                bitmapBoss.updateAndDraw(canvas, true);
            } else {
                bitmapBoss.updateAndDraw(canvas, false);
            }

            holder.unlockCanvasAndPost(canvas);
            triggerDraw = false;

            if (DEBUG) {
                time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for drawing == " + String.format("%.2f,", time));
            }
        }
    }

    public void onPause() {
        choreographer.removeFrameCallback(this);
        continueRendering = false;

        while (true) {
            try {
                triggerDraw = true;  // Necessary in case thread is waiting.
                if (drawThread != null) {
                    drawThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            break;
        }
    }

    public void onResume() {
        choreographer.postFrameCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated() invoked");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        Log.d(TAG, "surfaceChanged() invoked");
        surfaceInfo = new SurfaceInfo(width, height);
        publishSurfaceInfo();

        drawThread = new Thread(this);
        continueRendering = true;
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed() invoked");
    }

    @Override
    public void doFrame(long l) {
        lastFrameStartTime = l;
        choreographer.postFrameCallback(this);
        triggerDraw = true;
    }

    @Override
    public void register(SurfaceInfoObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregister(SurfaceInfoObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void publishSurfaceInfo() {
        for (SurfaceInfoObserver observer : observers) {
            observer.onSurfaceChanged(surfaceInfo);
        }
    }

    @Override
    public void updateTouch(MotionEvent me) {
        switch (me.getActionMasked()) {
            case MotionEvent.ACTION_DOWN : {
                proceed = true;
            }
        }
    }
}

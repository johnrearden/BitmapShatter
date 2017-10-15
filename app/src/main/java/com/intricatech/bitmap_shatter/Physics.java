package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Physics implements TouchObserver,
                                Runnable{

    private final String TAG;
    private static final boolean DEBUG = false;

    private BitmapBoss bitmapBoss;
    private Thread physicsThread;

    private boolean updateComplete;
    private boolean continueRunning;
    private boolean beginUpdate;
    private boolean drawDataGrabComplete;
    private long timeSinceLastCallback;

    Physics(
            Context context,
            SurfaceInfoDirector surfaceInfoDirector,
            TouchDirector touchDirector,
            Configuration configuration) {

        TAG = getClass().getSimpleName();

        touchDirector.register(this);
        physicsThread = new Thread(this);
        bitmapBoss = new BitmapBoss(context, surfaceInfoDirector, touchDirector, configuration);

        updateComplete = false;
        continueRunning = false;
        beginUpdate = false;
        drawDataGrabComplete = true; // necessary so that the 1st update can execute at all.
    }

    @Override
    public void run() {

        outerloop:
        while(continueRunning) {

            if (DEBUG) {
                Log.d(TAG, "physicsThread is running");
            }

            // Wait for doFrame to signal that frame should start.
            while (!beginUpdate) {
                if (DEBUG) {
                    Log.d(TAG, "waiting for beginUpdate == true");
                }
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRunning) {
                    break outerloop;
                }
            }

            // Wait for gameSurfaceView to signal that it has grabbed the last frame's draw data.
            while (!drawDataGrabComplete) {
                if (DEBUG) {
                    Log.d(TAG, "waiting for drawDataGrabComplete == true");
                }
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRunning) {
                    break outerloop;
                }
            }

            // Begin the update, having set the flags.
            beginUpdate = false;
            updateComplete = false;

            bitmapBoss.update();
            if (DEBUG) {
                float t = System.nanoTime() - timeSinceLastCallback;
                Log.d(TAG, "Time for update == " + String.format("%.2f,", t));
            }

            updateComplete = true;
            drawDataGrabComplete = false;
        }
    }

    public void updateObjects() {
    }

    public void drawObjects(Canvas canvas) {
    }

    @Override
    public void updateTouch(MotionEvent me) {

    }

    public Thread getPhysicsThread() {
        return physicsThread;
    }

    public BitmapBoss getBitmapBoss() {
        return bitmapBoss;
    }

    public void doFrame(long callbackTime) {

        if (updateComplete) {
            timeSinceLastCallback = callbackTime;
        }
        beginUpdate = true;
    }

    public void startThread() {
        continueRunning = true;
        physicsThread.start();
    }

    public boolean isUpdateComplete() {
        return updateComplete;
    }

    public boolean isContinueRunning() {
        return continueRunning;
    }

    public void setContinueRunning(boolean continueRunning) {
        this.continueRunning = continueRunning;
    }

    public void setDrawDataGrabComplete(boolean drawDataGrabComplete) {
        this.drawDataGrabComplete = drawDataGrabComplete;
    }
}

package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Physics implements TouchObserver,
                                Runnable{

    private final String TAG;
    private static final boolean DEBUG = false;

    enum PhysicsThreadStatus {
        WAITING_FOR_CHOREOGRAPHER,
        WAITING_FOR_DATA_GRAB_COMPLETE,
        CALCULATING_PHYSICS
    }
    private volatile PhysicsThreadStatus physicsThreadStatus;

    private GameSurfaceView gameSurfaceView;
    private BitmapBoss bitmapBoss;
    private Thread physicsThread;

    private boolean continueRunning;
    private long timeOfLastCallback;
    int missedFrames;

    Physics(
            Context context,
            TouchDirector touchDirector,
            Configuration configuration,
            GameSurfaceView gameSurfaceView) {

        TAG = getClass().getSimpleName();

        touchDirector.register(this);
        physicsThread = new Thread(this);
        bitmapBoss = new BitmapBoss(context, gameSurfaceView, touchDirector, configuration);
        this.gameSurfaceView = gameSurfaceView;

        physicsThreadStatus = PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER;
        continueRunning = false;
    }

    @Override
    public void run() {

        outerloop:
        while(continueRunning) {

            if (DEBUG) {
                Log.d(TAG, "physicsThread is running");
            }

            // Wait for doFrame to signal that frame should start.
            while (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
                if (DEBUG) {
                    Log.d(TAG, "waiting for choreographer == true");
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
            while (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE) {
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
                if (gameSurfaceView.getDrawThreadStatus() == GameSurfaceView.DrawThreadStatus.GRAB_COMPLETE_DRAWING_FRAME) {
                    physicsThreadStatus = PhysicsThreadStatus.CALCULATING_PHYSICS;
                }
            }

            bitmapBoss.update();
            physicsThreadStatus = PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER;

            if (DEBUG) {
                float t = System.nanoTime() - timeOfLastCallback;
                Log.d(TAG, "Time for update == " + String.format("%.2f,", t));
            }
        }
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

        timeOfLastCallback = callbackTime;
        if (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
            physicsThreadStatus = PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE;
        } else {
            missedFrames++;
            Log.d(TAG, "missed frame ..... total == " + missedFrames);

        }
    }

    public void startPhysicsThread() {
        continueRunning = true;
        physicsThread.start();
    }

    public boolean isContinueRunning() {
        return continueRunning;
    }

    public void setContinueRunning(boolean continueRunning) {
        this.continueRunning = continueRunning;
    }

    public PhysicsThreadStatus getPhysicsThreadStatus() {
        return physicsThreadStatus;
    }


}

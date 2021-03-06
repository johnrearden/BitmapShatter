package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

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

    enum DrawThreadStatus {
        WAITING_FOR_VALID_SURFACE,
        WAITING_FOR_CHOREOGRAPHER,
        GRABBING_DATA,
        GRAB_COMPLETE_DRAWING_FRAME
    }

    private volatile DrawThreadStatus drawThreadStatus;

    private ArrayList<SurfaceInfoObserver> observers;

    private Physics physics;
    private Resources resources;
    private SurfaceHolder holder;
    private Choreographer choreographer;
    private SharedPreferences sharedPreferences;

    private SurfaceInfo surfaceInfo;
    private Paint linePaint, textPaint;

    private Thread drawThread;
    private boolean continueRendering;
    private long lastFrameStartTime;
    int missedFrames;

    private ArrayList<ShardDrawingPacket> packetList;

    public GameSurfaceView(Context context) {
        super(context);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    public void initialize(Context context,
                           TouchDirector touchDirector,
                           Configuration configuration,
                           SharedPreferences sharedPreferences) {
        TAG = getClass().getSimpleName();
        Log.d(TAG, "initialize() invoked");
        touchDirector.register(this);
        this.sharedPreferences = sharedPreferences;
        drawThreadStatus = DrawThreadStatus.WAITING_FOR_VALID_SURFACE;

        observers = new ArrayList<>();
        resources = getResources();
        holder = getHolder();
        holder.addCallback(this);
        choreographer = Choreographer.getInstance();
        physics = new Physics(context, touchDirector, configuration, this);
        packetList = new ArrayList<>();
        for (int i = 0; i < physics.getBitmapBoss().getSizeOfShardList(); i++) {
            packetList.add(new ShardDrawingPacket());
        }

        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5.0f);
        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(70);

        drawThread = new Thread(this);
        continueRendering = false;
    }

    @Override
    public void run() {
        // Outer while loop - continueRendering set by Activity.onResume() and Activity.onPause().
        outerloop:
        while (continueRendering) {

            // If the surface isn't available yet, skip the frame.
            if (!holder.getSurface().isValid()) {
                /*if (DEBUG) {
                    Log.d(TAG, "waiting for valid surface .... ");
                }*/
                continue;
            } else drawThreadStatus = DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER;

            // Wait for the Choreographer to initiate the frame via callback to doFrame().
            while (drawThreadStatus == DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
                /*if (DEBUG) {
                    Log.d(TAG, "waiting for choreographer");
                }*/
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRendering) {
                    break outerloop;
                }
            }

            /*// Skip the frame if the physics thread has not completed the previous update.
            if (physics.getPhysicsThreadStatus() != Physics.PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
                if (DEBUG) {
                    Log.d(TAG, "physics Thread not finished previous frame");
                }
                continue;
            }*/

            // Grab the drawing data from bitmapBoss.shardList. If physics is not ready for the grab,
            // redraw the previous frame using the old data.
            if (physics.getPhysicsThreadStatus() == Physics.PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE) {
                grabDrawingPackets(physics.getBitmapBoss().getShardList());
            }
            drawThreadStatus = DrawThreadStatus.GRAB_COMPLETE_DRAWING_FRAME;


            // MAIN DRAWING ROUTINE STARTS HERE.
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.BLACK);
            if (DEBUG) {
                canvas.drawLine(
                        canvas.getWidth() / 2,
                        0,
                        canvas.getWidth() / 2,
                        canvas.getHeight(),
                        linePaint
                );
                canvas.drawLine(
                        0,
                        canvas.getHeight() / 2,
                        canvas.getWidth(),
                        canvas.getHeight() / 2,
                        linePaint
                );
                canvas.drawText(
                        "frameNumber == " + physics.getBitmapBoss().getFrameNumber(),
                        50, 50, textPaint
                );
            }

            for (ShardDrawingPacket packet : packetList) {
                packet.drawPacket(canvas);
            }

            if (DEBUG) {
                canvas.drawText(
                        "drawThread misses : " +
                                + missedFrames,
                        100, 100, textPaint
                );
            }
            if (DEBUG) {
                canvas.drawText(
                        "physics Thread misses : " +
                                + physics.missedFrames,
                        100, 160, textPaint
                );
            }

            holder.unlockCanvasAndPost(canvas);

            if (DEBUG) {
                float time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for drawing == " + String.format("%.2f,", time));
            }
        }
    }

    public void onPause() {
        choreographer.removeFrameCallback(this);
        continueRendering = false;

        while (true) {
            try {
                if (drawThread != null) {
                    drawThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            break;
        }

        while (true) {
            try {
                physics.setContinueRunning(false);
                if (physics.getPhysicsThread() != null) {
                    physics.getPhysicsThread().join();
                }
            } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            break;
        }
    }

    public void onResume() {
        choreographer.postFrameCallback(this);


        if (!drawThread.isAlive() && holder.getSurface().isValid()) {
            Log.d(TAG, "starting drawThread from onResume()");
            startThreads();
        }
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

        startThreads();
    }

    private void startThreads() {
        physics.setContinueRunning(true);
        physics.startPhysicsThread();

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
        if (drawThreadStatus == DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
            drawThreadStatus = DrawThreadStatus.GRABBING_DATA;
        } else {
            missedFrames++;
            Log.d(TAG, "missed frame ..... total == " + missedFrames);
        }
        physics.doFrame(l);

        /**
         * doFrame must grab the matrices, bitmaps and zPositions of the shards and then
         * the drawThread simply calls canvas.drawBitmap(bitmap, matrix, null).
         * GameSurfaceView should keep an ArrayList to mirror that in BitmapBoss. The bitmaps
         * can be final and held by BitmapBoss and accessed concurrently by both Threads as they
         * do not need to be written to after creation in BitmapBosses constructor. The matrices
         * and the z-positions must be copied though, as PhysicsThread will be writing the new frame
         * info to the objects while the drawThread needs to access the previous ones. Use a
         * ShardDrawingData object to encapsulate the matrix and zPosition copies, along with a reference
         * to the (immutable?) shard.bitmap.
         *
         * Both Threads should poll a flag flipped by doFrame() each time the Choreographer fires,
         * and return gracefully from their iterations through the shardLists if they run out of time,
         * to enable each frame to at least start on schedule, even if it doesnt complete. This can be
         * implemented after the basic concurrency mechanics are put in place.
         */
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
            }
        }
    }

    private void grabDrawingPackets(List<BitmapShard> shardList) {
        for (int i = 0; i < shardList.size(); i++)  {
            packetList.get(i).copyShardData(shardList.get(i));
        }
    }

    private class ShardDrawingPacket implements Comparable<ShardDrawingPacket>{
        Bitmap bitmap;
        Matrix matrix;
        float zPos;

        ShardDrawingPacket() {
            bitmap = null;
            matrix = new Matrix();
            zPos = 1.0f;
        }

        void copyShardData(BitmapShard shard) {
            zPos = shard.getzPos();
            matrix.set(shard.getShardMatrix());
            bitmap = shard.getBitmap();
        }

        void drawPacket(Canvas canvas) {
            canvas.drawBitmap(bitmap, matrix, null);
        }

        @Override
        public int compareTo(@NonNull ShardDrawingPacket packet) {
            if (packet.zPos > zPos) {
                return -1;
            } else if (packet.zPos < zPos) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public Physics getPhysics() {
        return physics;
    }

    public void setPhysics(Physics physics) {
        this.physics = physics;
    }

    public void setContinueRendering(boolean continueRendering) {
        this.continueRendering = continueRendering;
    }

    public DrawThreadStatus getDrawThreadStatus() {
        return drawThreadStatus;
    }
}

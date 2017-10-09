package com.intricatech.bitmap_shatter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Ball implements SurfaceInfoObserver, TouchObserver {

    private float xPos, yPos;
    private float velocity, direction;
    private float radius;
    private float gravity;
    private Paint ballPaint;

    private static final float BALL_RADIUS_TO_SCREENWIDTH_RATIO = 0.02f;
    private static final float VELOCITY_TO_SCREENWIDTH_RATIO = 0.01f;
    private static final float GRAVITY_TO_SCREENHEIGHT_RATIO = 0.0005f;
    private static final float BOUNCE_EFFICENCY = 0.9f;

    private SurfaceInfo surfaceInfo;

    public Ball(SurfaceInfoDirector surfaceInfoDirector) {
        surfaceInfoDirector.register(this);
        xPos = 0;
        yPos = 0;
        direction = (float) (-Math.PI + Math.PI * 2 * Math.random());
        ballPaint = new Paint();
        ballPaint.setStyle(Paint.Style.FILL);
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(Color.WHITE);
    }

    public void onSurfaceChanged(SurfaceInfo pai) {
        this.surfaceInfo = pai;
        radius = surfaceInfo.screenWidth * BALL_RADIUS_TO_SCREENWIDTH_RATIO;
        velocity = surfaceInfo.screenWidth * VELOCITY_TO_SCREENWIDTH_RATIO;
        gravity = surfaceInfo.screenHeight * GRAVITY_TO_SCREENHEIGHT_RATIO;
        xPos = surfaceInfo.screenWidth * 0.5f;
        yPos = surfaceInfo.screenHeight * 0.5f;
    }



    public void update() {

        float xVel = velocity * (float) Math.cos(direction);
        float yVel = velocity * (float) Math.sin(direction);

        // Apply gravity.
        yVel += gravity;

        // Check for collisions.
        if (xPos + xVel > surfaceInfo.screenWidth - radius || xPos + xVel < radius) {
            xVel = -xVel;
            xVel = xVel * BOUNCE_EFFICENCY;
        }
        if (yPos + yVel > surfaceInfo.screenHeight - radius || yPos + yVel < radius) {
            yVel = -yVel;
            yVel = yVel * BOUNCE_EFFICENCY;
        }

        // Modify position.
        xPos = xPos + xVel;
        yPos = yPos + yVel;


        direction = (float) Math.atan2(yVel, xVel);
        velocity = (float) Math.sqrt(xVel * xVel + yVel * yVel);
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(
                xPos,
                yPos,
                radius,
                ballPaint
        );
    }

    public void updateTouch(MotionEvent me) {

    }


}

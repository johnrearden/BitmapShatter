package com.intricatech.bitmap_shatter;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Random;

/**
 * Created by Bolgbolg on 08/10/2017.
 */

public class BitmapShard {

    private final String TAG;

    private Bitmap bitmap;
    private float xSize, ySize;

    private int recursiveDepth;
    private float xPos, yPos, zPos;
    private float xVel, yVel, zVel;
    private float xRotation, yRotation, zRotation;
    private float xAngVel, yAngVel, zAngVel;

    private boolean isOnScreen;

    private Random random;


    public BitmapShard(
            Bitmap bmap,
            int recursiveDepth,
            float xPos,
            float yPos,
            float xVel,
            float yVel) {
        TAG = getClass().getSimpleName();
        random = new Random();
        this.bitmap = bmap;
        this.recursiveDepth = recursiveDepth;
        this.xPos = xPos;
        this.yPos = yPos;
        this.xVel = xVel;
        this.yVel = yVel;

        zPos = 1.0f;
        zVel = 0.0f;
        isOnScreen = true;

        xRotation = 0;
        yRotation = 0;
        zRotation = 0;

        xSize = bitmap.getWidth();
        ySize = bitmap.getHeight();

        // Set random angular velocities.
        xAngVel = ApplicationConstants.MIN_X_ANGULAR_VELOCITY
                + random.nextFloat() * (ApplicationConstants.MAX_X_ANGULAR_VELOCITY - ApplicationConstants.MIN_X_ANGULAR_VELOCITY);
        if (random.nextFloat() < 0.5f) {
            xAngVel = -xAngVel;
        }
        yAngVel = ApplicationConstants.MIN_Y_ANGULAR_VELOCITY
                + random.nextFloat() * (ApplicationConstants.MAX_Y_ANGULAR_VELOCITY - ApplicationConstants.MIN_Y_ANGULAR_VELOCITY);
        if (random.nextFloat() < 0.5f) {
            yAngVel = -yAngVel;
        }
        zAngVel = ApplicationConstants.MIN_Z_ANGULAR_VELOCITY
                + random.nextFloat() * (ApplicationConstants.MAX_Z_ANGULAR_VELOCITY - ApplicationConstants.MIN_Z_ANGULAR_VELOCITY);
        if (random.nextFloat() < 0.5f) {
            zAngVel = -zAngVel;
        }

        Log.d(TAG, "xPos == " + this.xPos);
    }

    public void update(SurfaceInfo surfaceInfo) {
        xPos += xVel;
        yPos += yVel;
        zPos += zVel;
        xRotation += xAngVel;
        yRotation += yAngVel;
        zRotation += zAngVel;

        if (xPos < -surfaceInfo.screenWidth / 2 || xPos > surfaceInfo.screenWidth / 2) {
            isOnScreen = false;
        } else {
            isOnScreen = true;
        }
        if (yPos < -surfaceInfo.screenHeight / 2 || yPos > surfaceInfo.screenHeight / 2) {
            isOnScreen = false;
        } else {
            isOnScreen = true;
        }
    }

    public float getxPos() {
        return xPos;
    }

    public float getyPos() {
        return yPos;
    }

    public float getzPos() {
        return zPos;
    }

    public float getxRotation() {
        return xRotation;
    }

    public float getyRotation() {
        return yRotation;
    }

    public float getzRotation() {
        return zRotation;
    }

    public boolean isOnScreen() {
        return isOnScreen;
    }

    public float getxSize() {
        return xSize;
    }

    public float getySize() {
        return ySize;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getRecursiveDepth() {
        return recursiveDepth;
    }

    public float getxVel() {
        return xVel;
    }

    public float getyVel() {
        return yVel;
    }

    public float getzVel() {
        return zVel;
    }
}

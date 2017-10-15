package com.intricatech.bitmap_shatter;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Bolgbolg on 08/10/2017.
 */

public class BitmapShard implements Comparable<BitmapShard> {

    private final String TAG;
    private final float HALF_PI = (float) Math.PI / 2;

    enum VelocityControlType {
        LINEAR_ACCELERATION,
        SINUSOIDAL
    }
    private VelocityControlType velocityControlType;

    BitmapShard[] children;
    private List<RecordedState> recordedStates;
    private Configuration configuration;

    private Bitmap bitmap;
    private Bitmap source;
    private float xSize, ySize;
    private PointF centerRelativeToSourceCenter;
    private float distToSourceCenterRatio;
    private PointF destinationPoint;
    private PointF position;
    private int memoryOverhead;

    private int recursiveDepth;
    private float xPos, yPos, zPos;
    private float xVel, yVel, zVel;
    private float xVelDec, yVelDec, zVelDec;
    private float xRotation, yRotation, zRotation;
    private float xRotationDec, yRotationDec, zRotationDec;
    private float xAngVel, yAngVel, zAngVel;
    private float velocityRatio;

    private boolean isOnScreen;
    private boolean isParent;
    private boolean canShatter;

    private Random random;
    private Matrix matrix;

    // Constructor.
    public BitmapShard(
            Configuration configuration,
            VelocityControlType velocityControlType,
            Bitmap bmap,
            Bitmap source,
            PointF centerRelativeToSourceCenter,
            PointF position,
            float maxDistToSource,
            int recursiveDepth) {

        TAG = getClass().getSimpleName();

        this.configuration = configuration;
        children = new BitmapShard[2];
        recordedStates = new ArrayList<>();
        random = new Random();
        this.bitmap = bmap;
        this.recursiveDepth = recursiveDepth;
        this.velocityControlType = velocityControlType;
        this.centerRelativeToSourceCenter = centerRelativeToSourceCenter;
        this.source = source;
        float distToSourceCenter = (float) Math.sqrt(
                centerRelativeToSourceCenter.x * centerRelativeToSourceCenter.x)
                + (centerRelativeToSourceCenter.y * centerRelativeToSourceCenter.y);
        distToSourceCenterRatio = distToSourceCenter / maxDistToSource;
        if (distToSourceCenterRatio > 0.99f) {
            distToSourceCenterRatio = 0.99f;
        }
        this.position = position;
        xPos = position.x;
        yPos = position.y;

        destinationPoint = new PointF();
        float x = centerRelativeToSourceCenter.x;
        float y = centerRelativeToSourceCenter.y;
        float xAdjustment = x > 0 ? -Math.abs(y * 0.25f) : Math.abs(y * 0.25f);
        float yAdjustment = y > 0 ? -(float) Math.abs(x * 0.25f) : (float) Math.abs(x * 0.25f);
        destinationPoint.x = (x + xAdjustment) * configuration.getExpansionRatio();
        destinationPoint.y = (y + yAdjustment) * configuration.getExpansionRatio();

        zPos = 1.0f;
        zVel = 0.0f;
        isOnScreen = true;
        isParent = false;
        canShatter = false;

        xRotation = 0;
        yRotation = 0;
        zRotation = 0;

        xSize = bitmap.getWidth();
        ySize = bitmap.getHeight();
        memoryOverhead = (int) (xSize * ySize * 4);

        // Set random angular velocities.
        xAngVel = configuration.getMinXAngularVelocity()
                + random.nextFloat() * (configuration.getMaxXAngularVelocity() - configuration.getMinXAngularVelocity());
        if (random.nextFloat() < 0.5f) {
            xAngVel = -xAngVel;
        }
        yAngVel = configuration.getMinYAngularVelocity()
                + random.nextFloat() * (configuration.getMaxYAngularVelocity() - configuration.getMinYAngularVelocity());
        if (random.nextFloat() < 0.5f) {
            yAngVel = -yAngVel;
        }
        zAngVel = configuration.getMinZAngularVelocity()
                + random.nextFloat() * (configuration.getMaxZAngularVelocity() - configuration.getMinZAngularVelocity());
        if (random.nextFloat() < 0.5f) {
            zAngVel = -zAngVel;
        }

        // Check if this shard should itself be allowed to shatter. If the recursive depth
        // is between the MIN and MAX, then allow a 50% chance that it will shatter.
        if (recursiveDepth < configuration.getMinRecursiveDepth()) {
            canShatter = true;
        } else if (recursiveDepth == configuration.getMaxRecursiveDepth()) {
            canShatter = false;
        } else {
            canShatter = Math.random() < 0.5f ? true : false;
        }

        // Save the initial state.
        saveState(0);

        // Create the matrix.
        matrix = new Matrix();
    }

    public void update(
            SurfaceInfo surfaceInfo,
            int frameNumber,
            Camera camera,
            boolean shouldDecelerate) {

        switch(velocityControlType) {

            case LINEAR_ACCELERATION:
                if (shouldDecelerate) {
                    xVel += xVelDec;
                    yVel += yVelDec;
                    zVel += zVelDec;
                    xAngVel += xRotationDec;
                    yAngVel += yRotationDec;
                    zAngVel += zRotationDec;

                }
                xPos += xVel;
                yPos += yVel;
                zPos += zVel;
                xRotation += xAngVel;
                yRotation += yAngVel;
                zRotation += zAngVel;
                break;

            case SINUSOIDAL:
                float expansionProgressRatio = (float) frameNumber / configuration.getFrameLimitBeforeReversing();
                float xPosRange = destinationPoint.x - centerRelativeToSourceCenter.x;
                float yPosRange = destinationPoint.y - centerRelativeToSourceCenter.y;
                float zPosRange = configuration.getzPosAtExpansionLimit() - 1.0f;
                xPos = position.x + xPosRange * (float) Math.sin(HALF_PI * expansionProgressRatio);
                yPos = position.y + yPosRange * (float) Math.sin(HALF_PI * expansionProgressRatio);
                zPos = 1.0f + zPosRange * expansionProgressRatio * (1.0f - distToSourceCenterRatio);

                xRotation += xAngVel;
                yRotation += yAngVel;
                zRotation += zAngVel;
                break;
        }


        saveState(frameNumber);

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

        camera.save();

        camera.rotate(xAngVel, yAngVel, zAngVel);
        camera.translate(
                -xSize / 2,
                +ySize / 2,
                0.0f);
        matrix = new Matrix();
        camera.getMatrix(matrix);
        matrix.postScale(zPos, zPos);
        matrix.postTranslate(
                surfaceInfo.screenWidth / 2 - source.getWidth() / 2
                        + (xSize / 2) + xPos
                            /*- ((shard.getzPos() - 1.0f) * shard.getxSize())*/,
                0);
        matrix.postTranslate(
                0,
                surfaceInfo.screenHeight / 2 - source.getHeight() / 2
                        + (ySize / 2) + yPos
                            /*- ((shard.getzPos() - 1.0f) * shard.getySize())*/);

        camera.restore();
    }



    public void runExistingFramesBackwards(SurfaceInfo surfaceInfo, int frameNumber) {
        RecordedState frameState = recordedStates.get(frameNumber);
        xPos = frameState.xPos;
        yPos = frameState.yPos;
        zPos = frameState.zPos;
        xRotation = frameState.xRotation;
        yRotation = frameState.yRotation;
        zRotation = frameState.zRotation;
    }

    public void runExistingFramesForwards(SurfaceInfo surfaceInfo, int frameNumber) {
        RecordedState frameState = recordedStates.get(frameNumber);
        xPos = frameState.xPos;
        yPos = frameState.yPos;
        zPos = frameState.zPos;
        xRotation = frameState.xRotation;
        yRotation = frameState.yRotation;
        zRotation = frameState.zRotation;
    }

    private void saveState(int frameNumber) {
        recordedStates.add(
                new RecordedState(
                        xPos, yPos, zPos, xRotation, yRotation, zRotation, frameNumber
                )
        );
    }

    @Override
    public int compareTo(@NonNull BitmapShard bitmapShard) {
        if (bitmapShard.getzPos() < zPos) {
            return 1;
        } else if (bitmapShard.getzPos() > zPos) {
            return -1;
        } else return 0;
    }

    private class RecordedState {

        int frameNumber;

        float xPos, yPos, zPos;
        float xRotation, yRotation, zRotation;

        RecordedState(
                float xPos, float yPos, float zPos,
                float xRotation, float yRotation, float zRotation,
                int frameNumber
        ){
            this.xPos = xPos;
            this.yPos = yPos;
            this.zPos = zPos;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
            this.zRotation = zRotation;
            this.frameNumber = frameNumber;
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

    public boolean isParent() {
        return isParent;
    }

    public void setParent(boolean parent) {
        isParent = parent;
    }

    public boolean canShatter() {
        return canShatter;
    }

    public void setCanShatter(boolean canShatter) {
        this.canShatter = canShatter;
    }

    public PointF getCenterRelativeToSourceCenter() {
        return centerRelativeToSourceCenter;
    }

    public Matrix getMatrix() {
        return matrix;
    }
}

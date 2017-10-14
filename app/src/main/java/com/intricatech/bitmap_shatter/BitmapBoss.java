package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.PAUSED_AT_OUTER;
import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.REVERSING;
import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.STOPPED_AND_RECORDED;

/**
 * Created by Bolgbolg on 06/10/2017.
 */

public class BitmapBoss implements SurfaceInfoObserver, TouchObserver{

    private final String TAG;

    private SurfaceInfo surfaceInfo;
    private Configuration configuration;

    private Bitmap sourceBitmap, alphaBitmap;
    private Paint shatterPaint, filterPaint;
    private int sourceWidth, sourceHeight;

    private Camera camera;
    private Matrix matrix;
    private List<BitmapShard> shardList;
    private List<BitmapShard> tempList;

    private int frameNumber;
    private int pauseCountdown;
    float maxDistToCenter;

    enum PlayType {
        BEFORE_START, STOPPED_AND_RECORDED, PLAYING_AND_RECORDING, REVERSING, PLAYING, PAUSED_AT_OUTER
    }
    private PlayType playType;


    public BitmapBoss(
            Context context,
            SurfaceInfoDirector surfaceInfoDirector,
            TouchDirector touchDirector,
            Configuration configuration) {

        TAG = getClass().getSimpleName();

        surfaceInfoDirector.register(this);
        touchDirector.register(this);
        this.configuration = configuration;
        shardList = new ArrayList<>();
        tempList = new ArrayList<>();
        frameNumber = 1;
        playType = PlayType.BEFORE_START;

        shatterPaint = new Paint();
        shatterPaint.setStrokeWidth(5);
        shatterPaint.setColor(Color.BLUE);
        shatterPaint.setAntiAlias(true);
        shatterPaint.setStyle(Paint.Style.FILL);
        shatterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        filterPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        sourceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.download);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();
        sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, ((int) (sourceWidth * 0.5f)), ((int) (sourceHeight * 0.5f)), false);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();
        maxDistToCenter = (float) Math.sqrt((sourceWidth / 4) * (sourceWidth / 4)) + ((sourceHeight / 4) * (sourceHeight / 4));

        alphaBitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[sourceWidth * sourceHeight];
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        alphaBitmap.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        BitmapShard originalShard = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                alphaBitmap,
                new PointF(0, 0),
                new PointF(0, 0),
                maxDistToCenter,
                0
        );
        tempList.add(originalShard);
        shatterBitmapShard(originalShard);
        Log.d(TAG, "tempList : " + tempList.toString());
        for (BitmapShard bs : tempList) {
           if (!bs.isParent()) {
               shardList.add(bs);
           }
        }

        matrix = new Matrix();
        camera = new Camera();
        camera.setLocation(0, 0, -32);
    }

    public void update(Canvas canvas) {

        drawShards(canvas);
        //simpleDraw(canvas);

        switch (playType) {

            case BEFORE_START:
                break;
            case STOPPED_AND_RECORDED:
                break;
            case PLAYING_AND_RECORDING:
                boolean shouldDecelerate =
                        frameNumber > configuration.getFramesForConstantVel() ? true : false;
                if (frameNumber < configuration.getFrameLimitBeforeReversing()) {
                    for (BitmapShard shard : shardList) {
                        shard.update(
                                surfaceInfo,
                                frameNumber,
                                shouldDecelerate);
                    }
                    Collections.sort(shardList);
                    frameNumber++;
                } else {
                    playType = PAUSED_AT_OUTER;
                    pauseCountdown = configuration.getLengthOfPauseAtExpansionLimit();
                    frameNumber--;
                }

                break;
            case REVERSING:
                if (frameNumber > 0) {
                    frameNumber--;
                    for (BitmapShard shard : shardList) {
                        shard.runExistingFramesBackwards(surfaceInfo, frameNumber);
                    }

                } else {
                    playType = STOPPED_AND_RECORDED;
                }
                break;
            case PLAYING:
                if (frameNumber < configuration.getFrameLimitBeforeReversing()) {
                    for (BitmapShard shard : shardList) {
                        shard.runExistingFramesForwards(surfaceInfo, frameNumber);
                    }
                    frameNumber++;
                } else {
                    playType = PAUSED_AT_OUTER;
                    pauseCountdown = configuration.getLengthOfPauseAtExpansionLimit();
                    frameNumber--;
                }
                break;
            case PAUSED_AT_OUTER:
                if (pauseCountdown-- <= 0) {
                    playType = REVERSING;
                }
                break;
        }
    }

    private void drawShards(Canvas canvas) {

        float canvasWidth = canvas.getWidth();
        float canvasHeight = canvas.getHeight();


        // Draw shards.
        for (BitmapShard shard : shardList) {
            camera.save();

            //camera.rotate(shard.getxRotation(), shard.getyRotation(), shard.getzRotation());
            camera.translate(
                    -shard.getxSize() / 2,
                    +shard.getySize() / 2,
                    0.0f);
            matrix = new Matrix();
            camera.getMatrix(matrix);
            matrix.postScale(shard.getzPos(), shard.getzPos());
            matrix.postTranslate(
                    canvasWidth / 2 - sourceWidth / 2
                            + (shard.getxSize() / 2) + shard.getxPos()
                            /*- ((shard.getzPos() - 1.0f) * shard.getxSize())*/,
                    0);
            matrix.postTranslate(
                    0,
                    canvasHeight / 2 - sourceHeight / 2
                            + (shard.getySize() / 2) + shard.getyPos()
                            /*- ((shard.getzPos() - 1.0f) * shard.getySize())*/);

            canvas.drawBitmap(shard.getBitmap(), matrix, null);
            camera.restore();
            matrix = null;
        }
    }

    private void shatterBitmapShard(BitmapShard source) {
        if (source.canShatter()) {
            if (source.getxSize() > source.getySize()) {
                source.children = splitBitmapShardVertically(source);
            } else {
                source.children = splitBitmapShardHorizontally(source);
            }
        }
        tempList.addAll(Arrays.asList(source.children));
        source.setParent(true);
        if (source.children[0].canShatter()) {
            shatterBitmapShard(source.children[0] );
        }
        if (source.children[1].canShatter()) {
            shatterBitmapShard(source.children[1]);
        }

    }

    private BitmapShard[] splitBitmapShardVertically(BitmapShard sourceShard) {

        Bitmap bitmap = sourceShard.getBitmap();
        float sourceWidth = bitmap.getWidth();
        float sourceHeight = bitmap.getHeight();
        float minX = sourceWidth / 2;
        float maxX = sourceWidth / 2;

        PointF sourceCenter = sourceShard.getCenterRelativeToSourceCenter();
        PointF[] coordinates;
        float yDist = sourceHeight / (configuration.getNumberOfCoordinates() - 1);
        coordinates = new PointF[configuration.getNumberOfCoordinates()];
        coordinates[0] = new PointF(sourceWidth / 2, 0);
        coordinates[configuration.getNumberOfCoordinates() - 1] = new PointF(sourceWidth / 2, sourceHeight);
        for (int i = 1; i < configuration.getNumberOfCoordinates() - 1; i++){
            float xCoor = (sourceWidth / 2)
                            - (sourceWidth * configuration.getVariationRatio() / 2)
                            + (float) (Math.random() * configuration.getVariationRatio());
            float yCoor = i * yDist - (sourceHeight * configuration.getVariationRatio() / 2) + (float) (Math.random() * configuration.getVariationRatio() * sourceHeight);
            if (xCoor < minX) {
                minX = xCoor;
            }
            if (xCoor > maxX) {
                maxX = xCoor;
            }

            coordinates[i] = new PointF(xCoor, yCoor);
        }
        // Create the 2 bitmaps.
        int minCutoff = (int) minX - 2;
        int maxCutoff = (int) maxX + 2;
        PointF center1 = new PointF(
                sourceCenter.x - sourceWidth / 4/* + halfMaxOffset*/,
                sourceCenter.y);
        PointF center2 = new PointF(
                sourceCenter.x + sourceWidth / 4/* - halfMinOffset*/,
                sourceCenter.y);
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, maxCutoff, (int)sourceHeight);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, minCutoff, 0, (int) (sourceWidth - minCutoff), (int) sourceHeight);

        // Copy out the visible section of the first half of the bitmap.
        Path path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < configuration.getNumberOfCoordinates(); i++) {
            path.lineTo(coordinates[i].x, coordinates[i].y);
        }
        path.lineTo(maxCutoff, sourceHeight);
        path.lineTo(maxCutoff, 0);
        path.lineTo(coordinates[0].x, coordinates[0].y);
        path.close();

        Canvas c = new Canvas(bitmap1);
        c.drawPath(path, shatterPaint);

        // Translate all coordinates left by the minX value.
        for (PointF point : coordinates) {
            point.x -= minCutoff;
        }

        // Copy out the visible section to the second half of the bitmap.
        path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < configuration.getNumberOfCoordinates(); i++) {
            path.lineTo(coordinates[i].x, coordinates[i].y);
        }
        path.lineTo(0, sourceHeight);
        path.lineTo(0, 0);
        path.lineTo(coordinates[0].x, coordinates[0].y);
        path.close();

        c = new Canvas(bitmap2);
        c.drawPath(path, shatterPaint);

        // Create the 2 resultant bitmapShards using the respective bitmaps.
        BitmapShard shard1 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap1,
                center1,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );
        BitmapShard shard2 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap2,
                center2,
                new PointF(sourceShard.getxPos() + minCutoff, sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );

        // Return the 2 children.
        return new BitmapShard[]{shard1, shard2};
    }

    private BitmapShard[] splitBitmapShardHorizontally(BitmapShard sourceShard) {

        Bitmap bitmap = sourceShard.getBitmap();
        float sourceWidth = bitmap.getWidth();
        float sourceHeight = bitmap.getHeight();
        float minY = sourceHeight / 2;
        float maxY = sourceHeight/ 2;

        PointF sourceCenter = sourceShard.getCenterRelativeToSourceCenter();
        PointF[] coordinates;
        float xDist = sourceWidth / (configuration.getNumberOfCoordinates() - 1);
        coordinates = new PointF[configuration.getNumberOfCoordinates()];
        coordinates[0] = new PointF(0, sourceHeight / 2);
        coordinates[configuration.getNumberOfCoordinates() - 1] = new PointF(sourceWidth, sourceHeight / 2);
        for (int i = 1; i < configuration.getNumberOfCoordinates() - 1; i++){
            float yCoor = (sourceHeight / 2)
                    - (sourceHeight * configuration.getVariationRatio() / 2)
                    + (float) (Math.random() * configuration.getVariationRatio() * sourceHeight);
            float xCoor = i * xDist - (sourceWidth * configuration.getVariationRatio() / 2) + (float) (Math.random() * configuration.getVariationRatio() * sourceWidth);
            if (yCoor < minY) {
                minY = yCoor;
            }
            if (yCoor > maxY) {
                maxY = yCoor;
            }

            coordinates[i] = new PointF(xCoor, yCoor);
        }

        // Create the 2 bitmaps.
        int minCutoff = (int) minY;
        int maxCutoff = (int) maxY;
        PointF center1 = new PointF(
                sourceCenter.x,
                sourceCenter.y - sourceHeight / 4/* + halfMaxOffset*/);
        PointF center2 = new PointF(
                sourceCenter.x,
                sourceCenter.y + sourceHeight / 4/* - halfMinOffset*/);
        Log.d(TAG, "sourceHeight == " + sourceHeight + ", maxCutoff == " + maxCutoff);
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, (int) sourceWidth, maxCutoff);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, minCutoff, (int) (sourceWidth), (int) sourceHeight - minCutoff);

        // Copy out the visible section of the first half of the bitmap.
        Path path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < configuration.getNumberOfCoordinates(); i++) {
            path.lineTo(coordinates[i].x, coordinates[i].y);
        }
        path.lineTo(sourceWidth, maxCutoff);
        path.lineTo(0, maxCutoff);
        path.lineTo(coordinates[0].x, coordinates[0].y);
        path.close();

        Canvas c = new Canvas(bitmap1);
        c.drawPath(path, shatterPaint);

        // Translate all coordinates left by the minY value.
        for (PointF point : coordinates) {
            point.y -= minCutoff;
        }

        // Copy out the visible section to the second half of the bitmap.
        path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < configuration.getNumberOfCoordinates(); i++) {
            path.lineTo(coordinates[i].x, coordinates[i].y);
        }
        path.lineTo(sourceWidth, 0);
        path.lineTo(0, 0);
        path.lineTo(coordinates[0].x, coordinates[0].y);
        path.close();

        c = new Canvas(bitmap2);
        c.drawPath(path, shatterPaint);

        // Create the 2 resultant bitmapShards using the respective bitmaps.
        BitmapShard shard1 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap1,
                center1,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );
        BitmapShard shard2 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap2,
                center2,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos() + minCutoff),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );

        // Return the 2 children.
        return new BitmapShard[]{shard1, shard2};
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(sourceBitmap, 0, 0, null);
    }

    public void simpleDraw(Canvas canvas) {
        for (BitmapShard shard : shardList) {
            canvas.drawBitmap(
                    shard.getBitmap(),
                    100 + shard.getxPos(),
                    100 + shard.getyPos(),
                    null
            );
        }
    }

    public void onSurfaceChanged(SurfaceInfo surfaceInfo) {
        this.surfaceInfo = surfaceInfo;
    }

    @Override
    public void updateTouch(MotionEvent me) {
        switch(me.getActionMasked()) {
            case MotionEvent.ACTION_DOWN :
                if (playType == PlayType.BEFORE_START) {
                    playType = PlayType.PLAYING_AND_RECORDING;
                } else if (playType == PlayType.STOPPED_AND_RECORDED) {
                    playType = PlayType.PLAYING;
                }
        }
    }

    public int getFrameNumber() {
        return frameNumber;
    }
}

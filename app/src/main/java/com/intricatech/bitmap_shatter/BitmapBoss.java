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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Bolgbolg on 06/10/2017.
 */

public class BitmapBoss implements SurfaceInfoObserver{

    private final String TAG;
    private static final int NUMBER_OF_COORDINATES = 10;
    private static final float VARIATION_RATIO = 0.15f;
    private static final float INITIAL_VELOCITY = 10.0f;

    private SurfaceInfo surfaceInfo;

    private Bitmap sourceBitmap, alphaBitmap;
    private Paint shatterPaint, filterPaint;
    private int sourceWidth, sourceHeight;

    private Camera camera;
    private Matrix matrix;
    private List<BitmapShard> shardList;


    public BitmapBoss(Context context, SurfaceInfoDirector surfaceInfoDirector) {

        TAG = getClass().getSimpleName();

        surfaceInfoDirector.register(this);
        shardList = Collections.synchronizedList(new ArrayList<BitmapShard>());

        shatterPaint = new Paint();
        shatterPaint.setStrokeWidth(5);
        shatterPaint.setColor(Color.BLUE);
        shatterPaint.setAntiAlias(false);
        shatterPaint.setStyle(Paint.Style.FILL);
        shatterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        filterPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        sourceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.download);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();
        /*sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, ((int) (sourceWidth * 0.7f)), ((int) (sourceHeight * 0.7f)), false);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();*/

        alphaBitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[sourceWidth * sourceHeight];
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        alphaBitmap.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        BitmapShard originalShard = new BitmapShard(
                alphaBitmap,
                0,
                0,
                0,
                0,
                0
        );
        shardList.add(originalShard);
        shardList.addAll(Arrays.asList(splitBitmapShardVertically(originalShard)));
        shardList.remove(originalShard);

        BitmapShard shard1 = shardList.get(0);
        BitmapShard shard2 = shardList.get(1);
        shardList.addAll(Arrays.asList(splitBitmapShardHorizontally(shard1)));
        shardList.remove(shard1);
        shardList.addAll(Arrays.asList(splitBitmapShardHorizontally(shard2)));
        shardList.remove(shard2);

        matrix = new Matrix();
        camera = new Camera();
        camera.setLocation(0, 0, -32);
    }

    public void updateAndDraw(Canvas canvas, boolean drawOnly) {

        float canvasWidth = canvas.getWidth();
        float canvasHeight = canvas.getHeight();

        // Draw shards.
        for (BitmapShard shard : shardList) {
            camera.save();

            camera.rotate(shard.getxRotation(), shard.getyRotation(), shard.getzRotation());
            camera.translate(
                    -shard.getxSize() / 2,
                    +shard.getySize() / 2,
                    0.0f);
            matrix = new Matrix();
            camera.getMatrix(matrix);
            matrix.postTranslate(canvasWidth / 2 + shard.getxPos(), 0);
            matrix.postTranslate(0, canvasHeight / 2 + shard.getyPos());
            canvas.drawBitmap(shard.getBitmap(), matrix, null);
            camera.restore();
            matrix = null;
        }
        // Update shards.
        if (!drawOnly) {
            for (BitmapShard shard: shardList) {
                shard.update(surfaceInfo);
            }
        }
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

    private BitmapShard[] splitBitmapShardVertically(BitmapShard sourceShard) {

        Bitmap bitmap = sourceShard.getBitmap();
        float sourceWidth = bitmap.getWidth();
        float sourceHeight = bitmap.getHeight();
        float minX = sourceWidth / 2;
        float maxX = sourceWidth / 2;

        PointF[] coordinates;
        float yDist = sourceHeight / (NUMBER_OF_COORDINATES - 1);
        coordinates = new PointF[NUMBER_OF_COORDINATES];
        coordinates[0] = new PointF(sourceWidth / 2, 0);
        coordinates[NUMBER_OF_COORDINATES - 1] = new PointF(sourceWidth / 2, sourceHeight);
        for (int i = 1; i < NUMBER_OF_COORDINATES - 1; i++){
            float xCoor = (sourceWidth / 2)
                            - (sourceWidth * VARIATION_RATIO / 2)
                            + (float) (Math.random() * VARIATION_RATIO * sourceWidth);
            float yCoor = i * yDist - (sourceHeight * VARIATION_RATIO / 2) + (float) (Math.random() * VARIATION_RATIO * sourceHeight);
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
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, maxCutoff, (int)sourceHeight);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, minCutoff, 0, (int) (sourceWidth - minCutoff), (int) sourceHeight);

        // Copy out the visible section of the first half of the bitmap.
        Path path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < NUMBER_OF_COORDINATES; i++) {
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
        for (int i = 1; i < NUMBER_OF_COORDINATES; i++) {
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
                bitmap1,
                sourceShard.getRecursiveDepth() + 1,
                sourceShard.getxPos(),
                sourceShard.getyPos(),
                sourceShard.getxVel() - INITIAL_VELOCITY,
                sourceShard.getyVel()
        );
        BitmapShard shard2 = new BitmapShard(
                bitmap2,
                sourceShard.getRecursiveDepth() + 1,
                sourceShard.getxPos() + minCutoff,
                sourceShard.getyPos(),
                sourceShard.getxVel() + INITIAL_VELOCITY,
                sourceShard.getyVel()
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

        PointF[] coordinates;
        float xDist = sourceWidth / (NUMBER_OF_COORDINATES - 1);
        coordinates = new PointF[NUMBER_OF_COORDINATES];
        coordinates[0] = new PointF(0, sourceHeight / 2);
        coordinates[NUMBER_OF_COORDINATES - 1] = new PointF(sourceWidth, sourceHeight / 2);
        for (int i = 1; i < NUMBER_OF_COORDINATES - 1; i++){
            float yCoor = (sourceHeight / 2)
                    - (sourceHeight * VARIATION_RATIO / 2)
                    + (float) (Math.random() * VARIATION_RATIO * sourceHeight);
            float xCoor = i * xDist - (sourceWidth * VARIATION_RATIO / 2) + (float) (Math.random() * VARIATION_RATIO * sourceWidth);
            if (yCoor < minY) {
                minY = yCoor;
            }
            if (yCoor > maxY) {
                maxY = yCoor;
            }

            coordinates[i] = new PointF(xCoor, yCoor);
        }
        // Create the 2 bitmaps.
        int minCutoff = (int) minY - 2;
        int maxCutoff = (int) maxY + 2;
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, (int) sourceWidth, maxCutoff);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, minCutoff, (int) (sourceWidth), (int) sourceHeight - minCutoff);

        // Copy out the visible section of the first half of the bitmap.
        Path path = new Path();
        path.moveTo(coordinates[0].x, coordinates[0].y);
        for (int i = 1; i < NUMBER_OF_COORDINATES; i++) {
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
        for (int i = 1; i < NUMBER_OF_COORDINATES; i++) {
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
                bitmap1,
                sourceShard.getRecursiveDepth() + 1,
                sourceShard.getxPos(),
                sourceShard.getyPos(),
                sourceShard.getxVel(),
                sourceShard.getyVel() - INITIAL_VELOCITY
        );
        BitmapShard shard2 = new BitmapShard(
                bitmap2,
                sourceShard.getRecursiveDepth() + 1,
                sourceShard.getxPos(),
                sourceShard.getyPos() + minCutoff,
                sourceShard.getxVel(),
                sourceShard.getyVel() + INITIAL_VELOCITY
        );

        // Return the 2 children.
        return new BitmapShard[]{shard1, shard2};
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(sourceBitmap, 0, 0, null);
    }

    public void onSurfaceChanged(SurfaceInfo surfaceInfo) {
        this.surfaceInfo = surfaceInfo;
    }

}

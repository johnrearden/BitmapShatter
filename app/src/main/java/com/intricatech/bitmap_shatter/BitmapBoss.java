package com.intricatech.bitmap_shatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.PAUSED_AT_OUTER;
import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.REVERSING;
import static com.intricatech.bitmap_shatter.BitmapBoss.PlayType.STOPPED_AND_RECORDED;

/**
 * Created by Bolgbolg on 06/10/2017.
 */

public class BitmapBoss implements SurfaceInfoObserver, TouchObserver{

    /**
     * String used to identify this class in Log statements.
     */
    private final String TAG;

    /**
     * Information about the surface used by GameSurfaceView.
     */
    private SurfaceInfo surfaceInfo;

    /**
     * A reference to a singleton class holding the current state of the app's configuration, backed
     * by a PreferenceActivity and its associated SharedPreference persistent data.
     */

    private Configuration configuration;

    /**
     * The Bitmap used to decode the source image from the drawable resource folder, and a fresh
     * transparent immutable Bitmap used to copy the image to, as the decoded Bitmap generated
     * by BitmapFactory's static method has a black background(for some unknown reason).
     */
    private Bitmap sourceBitmap, alphaBitmap;

    /**
     * Paint object used to blank out the section of a bitmap cut out by each splitting.
     */
    private Paint shatterPaint;

    /**
     * Width and height of the sourceImage (updated to reflect any scaling).
     */
    private int sourceWidth, sourceHeight;

    /**
     * The camera performs the 3d rotations on the shattered bitmaps. Note that it calls native
     * code - crashes can be caused by not providing a camera.restore() call to balance each
     * camera.save().
     */
    private Camera camera;

    /**
     * Lists for holding the split bitmaps. See doc for splitBitmapVertically() and
     * splitBitmapHorizontally() to see reason for second List.
     */
    private List<BitmapShard> shardList, tempList;

    /**
     * Counter for the frames. Can be run backwards and forwards to load cached copies of previously
     * generated states instead of recalculating the matrices.
     */
    private int frameNumber;

    /**
     * Countdown for the number of frames the animation pauses for at the outer limit of an
     * expansion.
     */
    private int pauseCountdown;

    /**
     * Holds the diagonal distance from one corner of the source image to the center ... used
     * to set a BitmapShards velocity proportional to its distance from the center.
     */
    float maxDistToCenter;

    /**
     * Flag indicating whether the first frame has been calculated or not.
     */
    private boolean firstFrameMatrixCalculated;

    /**
     * State enum to enable transitions between different behaviours.
     */
    enum PlayType {
        BEFORE_START,
        STOPPED_AND_RECORDED,
        PLAYING_AND_RECORDING,
        REVERSING,
        PLAYING,
        PAUSED_AT_OUTER
    }
    private PlayType playType;


    /**
     * Sole constructor. Splitting of the source Bitmap is performed here. As this needs to be
     * redone in the event of certain Configuration variables being changed, it should be extracted
     * from the constructor.
     *
     * @param context Reference to the parent GameActivity, local to constructor, passed through
     *                by GameSurfaceView and Physics objects.
     * @param surfaceInfoDirector Reference to the GameSurfaceView, local to constructor.
     * @param touchDirector Reference to the parent GameActivity, local to constructor.
     * @param configuration Reference to the Configuration singleton object, object field.
     */
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
        frameNumber = 0;
        playType = PlayType.BEFORE_START;

        shatterPaint = new Paint();
        shatterPaint.setStrokeWidth(1);
        shatterPaint.setColor(Color.BLUE);
        shatterPaint.setAntiAlias(true);
        shatterPaint.setStyle(Paint.Style.FILL);
        shatterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        sourceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.shuttle);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();
        sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, ((int) (sourceWidth * 0.5f)), ((int) (sourceHeight * 0.5f)), false);
        sourceWidth = sourceBitmap.getWidth();
        sourceHeight = sourceBitmap.getHeight();
        maxDistToCenter = (float) Math.sqrt((sourceWidth / 4) * (sourceWidth / 4)) + ((sourceHeight / 4) * (sourceHeight / 4));

        // Copy the decoded sourceBitmap to a fresh transparent Bitmap, as otherwise all Shards will
        // show a black background.
        alphaBitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[sourceWidth * sourceHeight];
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        alphaBitmap.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Create the first shard, and add it to the tempList.
        BitmapShard originalShard = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                alphaBitmap,
                sourceBitmap,
                new PointF(0, 0),
                new PointF(0, 0),
                maxDistToCenter,
                0
        );
        tempList.add(originalShard);

        // Call method which shatters BitmapShards recursively.
        shatterBitmapShard(originalShard);

        // Iterate through the temporary list and copy any BitmapShards that do not have children
        // back to the main ArrayList. BitmapShards with children do not need to be drawn.
        for (BitmapShard bs : tempList) {
           if (!bs.isParent()) {
               shardList.add(bs);
           }
        }

        camera = new Camera();
        camera.setLocation(0, 0, -32);
        firstFrameMatrixCalculated = false;
    }

    /**
     * Method uses a switch statement to control appropriate behaviour based on the current state.
     */
    public void update() {
        switch (playType) {

            // Save the initial state of the BitmapShards, as the states are saved after updating.
            case BEFORE_START:
                if (!firstFrameMatrixCalculated) {
                    for (BitmapShard shard : shardList) {
                        shard.saveInitialState(surfaceInfo, camera);
                    }
                    firstFrameMatrixCalculated = true;
                }
                break;

            // Waiting for a touch event to set off a recorded expansion.
            case STOPPED_AND_RECORDED:
                break;

            // Proceeding with the expansion for the first time and saving the state of each
            // frame to be reused later.
            case PLAYING_AND_RECORDING:
                boolean shouldDecelerate =
                        frameNumber > configuration.getFramesForConstantVel() ? true : false;
                if (frameNumber < configuration.getFrameLimitBeforeReversing()) {
                    for (BitmapShard shard : shardList) {
                        shard.update(
                                surfaceInfo,
                                frameNumber,
                                camera,
                                shouldDecelerate);
                    }
                    frameNumber++;
                } else {
                    playType = PAUSED_AT_OUTER;
                    pauseCountdown = configuration.getLengthOfPauseAtExpansionLimit();
                    frameNumber--;
                }

                break;

            // Running the frames backwards, and loading the states recorded previously.
            case REVERSING:
                if (frameNumber > 0) {
                    frameNumber--;
                    for (BitmapShard shard : shardList) {
                        shard.runExistingFramesBackwards(surfaceInfo, camera, frameNumber);
                    }

                } else {
                    playType = STOPPED_AND_RECORDED;
                }
                break;

            // Running the frames forwards, and loading the states recorded previously.
            case PLAYING:
                if (frameNumber < configuration.getFrameLimitBeforeReversing()) {
                    for (BitmapShard shard : shardList) {
                        shard.runExistingFramesForwards(surfaceInfo, camera, frameNumber);
                    }
                    frameNumber++;
                } else {
                    playType = PAUSED_AT_OUTER;
                    pauseCountdown = configuration.getLengthOfPauseAtExpansionLimit();
                    frameNumber--;
                }
                break;

            // Waiting for the pause at the outer limit of the expansion to be completed.
            case PAUSED_AT_OUTER:
                if (pauseCountdown-- <= 0) {
                    playType = REVERSING;
                }
                break;
        }
    }

    /**
     * Recursive method takes a BitmapShard as a paramter and splits it (the BitmapShard knows
     * itself whether or not it is to be split) if appropriate. The method splits the shards
     * along their longest side.
     *
     * @param source The BitmapShard to be split.
     */
    private void shatterBitmapShard(BitmapShard source) {
        if (source.canShatter()) {
            if (source.getxSize() > source.getySize()) {
                source.children = splitBitmapShardVertically(source);
            } else {
                source.children = splitBitmapShardHorizontally(source);
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
    }

    /**
     * Method splits the BitmapShard vertically, roughly in the center, and returns the 2 children.
     * It creates a path representing a jagged break in the bitmap, and creates 2 new bitmaps for
     * the visible halves of the source. It then blanks out the shape of the cut-out section using
     * a PorterDuff composition, calculates the center position of the 2 children relative to the
     * original sourceImage, and returns an array containing the 2 children.
     *
     * @param sourceShard The shard to be split.
     * @return An array containing the 2 children.
     */
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
                sourceBitmap,
                center1,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );
        BitmapShard shard2 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap2,
                sourceBitmap,
                center2,
                new PointF(sourceShard.getxPos() + minCutoff, sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );

        // Return the 2 children.
        return new BitmapShard[]{shard1, shard2};
    }

    /**
     * Method splits the BitmapShard horizontally, roughly in the center, and returns the 2 children.
     * It creates a path representing a jagged break in the bitmap, and creates 2 new bitmaps for
     * the visible halves of the source. It then blanks out the shape of the cut-out section using
     * a PorterDuff composition, calculates the center position of the 2 children relative to the
     * original sourceImage, and returns an array containing the 2 children.
     *
     * @param sourceShard The shard to be split.
     * @return An array containing the 2 children.
     */
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
                sourceBitmap,
                center1,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos()),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );
        BitmapShard shard2 = new BitmapShard(
                configuration,
                BitmapShard.VelocityControlType.SINUSOIDAL,
                bitmap2,
                sourceBitmap,
                center2,
                new PointF(sourceShard.getxPos(), sourceShard.getyPos() + minCutoff),
                maxDistToCenter,
                sourceShard.getRecursiveDepth() + 1
        );

        // Return the 2 children.
        return new BitmapShard[]{shard1, shard2};
    }

    /**
     * A debug utility method to draw the original source without splitting.
     * @param canvas A canvas passed by the GameSurfaceView.
     */
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
                } else if (playType == STOPPED_AND_RECORDED) {
                    playType = PlayType.PLAYING_AND_RECORDING;
                }
        }
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getSizeOfShardList() {
        return shardList.size();
    }

    public List<BitmapShard> getShardList() {
        return shardList;
    }
}

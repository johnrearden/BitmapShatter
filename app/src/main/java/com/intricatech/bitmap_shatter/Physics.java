package com.intricatech.bitmap_shatter;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Physics implements TouchObserver{

    private Ball ball;

    Physics(SurfaceInfoDirector surfaceInfoDirector, TouchDirector touchDirector) {
        touchDirector.register(this);
        ball = new Ball(surfaceInfoDirector);
    }

    public void updateObjects() {
        //ball.update();
    }

    public void drawObjects(Canvas canvas) {
        //ball.draw(canvas);
    }

    @Override
    public void updateTouch(MotionEvent me) {

    }
}

package com.intricatech.bitmap_shatter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameActivity extends FragmentActivity
                          implements View.OnTouchListener,
                                     TouchDirector {

    private String TAG;
    private GameSurfaceView gameSurfaceView;
    private ArrayList<TouchObserver> touchObservers;
    private Configuration configuration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);

        TAG = getClass().getSimpleName();
        touchObservers = new ArrayList<>();
        Log.d(TAG, "onCreate() invoked");
        gameSurfaceView = findViewById(R.id.game_surfaceview);
        gameSurfaceView.setOnTouchListener(this);
        configuration = Configuration.getInstance();
        gameSurfaceView.initialize(this, this, configuration);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() invoked");
        gameSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() invoked");
        gameSurfaceView.onResume();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        updateObservers(motionEvent);
        return false;
    }

    @Override
    public void register(TouchObserver touchObserver) {
        touchObservers.add(touchObserver);
    }

    @Override
    public void unregister(TouchObserver touchObserver) {
        touchObservers.remove(touchObserver);
    }

    @Override
    public void updateObservers(MotionEvent me) {
        for (TouchObserver ob : touchObservers) {
            ob.updateTouch(me);
        }
    }
}

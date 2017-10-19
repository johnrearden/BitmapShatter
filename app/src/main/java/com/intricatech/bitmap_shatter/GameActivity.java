package com.intricatech.bitmap_shatter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

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
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;

    private ImageButton settingsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);

        TAG = getClass().getSimpleName();
        touchObservers = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        Log.d(TAG, "onCreate() invoked");
        gameSurfaceView = findViewById(R.id.game_surfaceview);
        gameSurfaceView.setOnTouchListener(this);
        configuration = Configuration.getInstance();
        gameSurfaceView.initialize(this, this, configuration, sharedPreferences);

        settingsButton = findViewById(R.id.settings_button);

        prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getString(R.string.pref_key_min_recursive_depth))) {
                    configuration.setMinRecursiveDepth(
                            Integer.parseInt(sharedPreferences.getString(key, "1")));
                    getNewPhysics();

                } else if (key.equals(getString(R.string.pref_key_max_recursive_depth))) {
                    configuration.setMaxRecursiveDepth(
                            Integer.parseInt(sharedPreferences.getString(key, "1")));
                    getNewPhysics();
                }
            }
        };

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() invoked");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        gameSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() invoked");
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
        gameSurfaceView.onResume();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        updateObservers(motionEvent);
        return false;
    }

    public void onSettingsButtonClick (View view) {
        Log.d(TAG, "onSettingsButtonClick() invoked");
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefFragment())
                .addToBackStack(null)
                .commit();
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

    private void getNewPhysics() {
        Log.d(TAG, "getNewPhysics() invoked");
        gameSurfaceView.setContinueRendering(false);
        gameSurfaceView.setPhysics(new Physics(this, this, configuration, gameSurfaceView));
        gameSurfaceView.publishSurfaceInfo();
        gameSurfaceView.setContinueRendering(true);
        gameSurfaceView.getPhysics().getPhysicsThread().start();
        gameSurfaceView.getPhysics().setContinueRunning(true);
    }
}

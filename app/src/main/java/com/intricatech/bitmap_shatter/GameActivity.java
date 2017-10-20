package com.intricatech.bitmap_shatter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameActivity extends FragmentActivity
                          implements View.OnTouchListener,
                                     TouchDirector {
    /**
     * String used to identify class in Log output, set in onCreate()
     */
    private String TAG;

    /**
     * Class extending SurfaceView responsible for drawing and physics.
     */
    private GameSurfaceView gameSurfaceView;

    /**
     * List of observers that this TouchDirector passes touch information on to.
     */
    private ArrayList<TouchObserver> touchObservers;

    /**
     * A singleton class holding the current state of the app's configuration, backed by a
     * PreferenceActivity and its associated SharedPreference persistent data.
     */
    private Configuration configuration;

    /**
     * A reference to the application's SharedPreferences.
     */
    private SharedPreferences sharedPreferences;

    /**
     * A listener to pick up modifications to the applications configuration applied by the
     * PreferenceActivity.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);

        TAG = getClass().getSimpleName();
        touchObservers = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        gameSurfaceView = findViewById(R.id.game_surfaceview);
        gameSurfaceView.setOnTouchListener(this);
        configuration = Configuration.getInstance();
        gameSurfaceView.initialize(this, this, configuration, sharedPreferences);

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

    /**
     * Invokes superclass method and unregisters the listener holding a reference to this Activity,
     * to facilitate garbage collection. Passes call through to GameSurfaceView to kill the draw
     * and physics threads.
     */
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        gameSurfaceView.onPause();
    }

    /**
     * Invokes superclass method and registers a listener for changes in the SharedPreferences
     * applied by the PreferenceActivity. Calls through to GameSurfaceView.
     */
    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
        gameSurfaceView.onResume();
    }

    /**
     * Passes touch detections on to registered observers.
     * @param view
     * @param motionEvent
     * @return
     */

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        updateObservers(motionEvent);
        return false;
    }

    /**
     * Handles settings button click, here starting a PreferenceFragment, but should start a
     * PreferenceActivity. (Hard to style a Fragment independent of its Activity)
     * @param view
     */
    public void onSettingsButtonClick (View view) {
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

    /**
     * Abortive attempt to re-load the new configuration when a SharedPreference is changed and
     * spotted by the SharedPreferenceChangeListener. Would be neater to flag the update and wait
     * until the physics object is in an idle state.
     */
    private void getNewPhysics() {
        gameSurfaceView.setContinueRendering(false);
        gameSurfaceView.setPhysics(new Physics(this, this, configuration, gameSurfaceView));
        gameSurfaceView.publishSurfaceInfo();
        gameSurfaceView.setContinueRendering(true);
        gameSurfaceView.getPhysics().getPhysicsThread().start();
        gameSurfaceView.getPhysics().setContinueRunning(true);
    }
}

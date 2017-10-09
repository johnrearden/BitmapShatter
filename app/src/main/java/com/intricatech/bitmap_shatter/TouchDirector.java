package com.intricatech.bitmap_shatter;

import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 02/10/2017.
 */

public interface TouchDirector {

    public void register(TouchObserver touchObserver);

    public void unregister(TouchObserver touchObserver);

    public void updateObservers(MotionEvent me);
}

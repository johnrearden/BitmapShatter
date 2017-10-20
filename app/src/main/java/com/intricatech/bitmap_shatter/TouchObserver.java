package com.intricatech.bitmap_shatter;

import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 02/10/2017.
 */

public interface TouchObserver {

    /**
     * Method is called by the TouchDirector when a Touch event is detected. There is no need for
     * a returned boolean as because the observer does not know of its counterparts, it should never
     * cause the event to be consumed.
     *
     * @param me The MotionEvent generated.
     */
    public void updateTouch(MotionEvent me);
}

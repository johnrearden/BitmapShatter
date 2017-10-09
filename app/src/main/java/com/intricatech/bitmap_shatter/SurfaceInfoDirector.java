package com.intricatech.bitmap_shatter;

/**
 * Created by Bolgbolg on 02/10/2017.
 */

public interface SurfaceInfoDirector {

    public void register(SurfaceInfoObserver observer);

    public void unregister(SurfaceInfoObserver observer);

    public void publishSurfaceInfo();
}

package com.intricatech.bitmap_shatter;

/**
 * Created by Bolgbolg on 12/10/2017.
 */

public class Configuration {

    private static Configuration instance;

    public static Configuration getInstance() {
        if (instance == null) {
            return new Configuration();
        } else {
            return instance;
        }
    }

    enum Behaviour {
        EXPLODE_AND_REVERSE,
        EXPLODE_ONLY,
        IMPLODE_AND_REVERSE,
        IMPLODE_ONLY
    }

    private int maxRecursiveDepth;
    private int minRecursiveDepth;

    private float maxXAngularVelocity;
    private float maxYAngularVelocity;
    private float maxZAngularVelocity;
    private float minXAngularVelocity;
    private float minYAngularVelocity;
    private float minZAngularVelocity;

    private float maxXVelToScreenWidthRatio;
    private float maxYVelToScreenWidthRatio;
    private float maxZVelToScreenWidthRatio;

    private int frameLimitBeforeReversing;
    private int numberOfCoordinates;
    private float variationRatio;
    private float initialVelocity;
    private int framesForConstantVel;
    private int lengthOfPauseAtExpansionLimit;
    private float expansionRatio;
    private float zPosAtExpansionLimit;

    private Configuration() {
        maxRecursiveDepth = 7;
        minRecursiveDepth = 5;

        maxXAngularVelocity = 3.0f;
        maxYAngularVelocity = 3.0f;
        maxZAngularVelocity = 7.0f;
        minXAngularVelocity = 1.0f;
        minYAngularVelocity = 1.0f;
        minZAngularVelocity = 1.0f;

        maxXVelToScreenWidthRatio = 0.01f;
        maxYVelToScreenWidthRatio = 0.01f;
        maxZVelToScreenWidthRatio = 0.01f;

        frameLimitBeforeReversing = 22;
        numberOfCoordinates = 10;
        variationRatio = 0.15f;
        initialVelocity = 70.0f;
        framesForConstantVel = 5;
        lengthOfPauseAtExpansionLimit = 0;
        expansionRatio = 4.0f;
        zPosAtExpansionLimit = 5.0f;

    }

    public float getzPosAtExpansionLimit() {
        return zPosAtExpansionLimit;
    }

    public void setzPosAtExpansionLimit(float zPosAtExpansionLimit) {
        this.zPosAtExpansionLimit = zPosAtExpansionLimit;
    }

    public int getMaxRecursiveDepth() {
        return maxRecursiveDepth;
    }

    public void setMaxRecursiveDepth(int maxRecursiveDepth) {
        this.maxRecursiveDepth = maxRecursiveDepth;
    }

    public int getMinRecursiveDepth() {
        return minRecursiveDepth;
    }

    public void setMinRecursiveDepth(int minRecursiveDepth) {
        this.minRecursiveDepth = minRecursiveDepth;
    }

    public float getMaxXAngularVelocity() {
        return maxXAngularVelocity;
    }

    public void setMaxXAngularVelocity(float maxXAngularVelocity) {
        this.maxXAngularVelocity = maxXAngularVelocity;
    }

    public float getMaxYAngularVelocity() {
        return maxYAngularVelocity;
    }

    public void setMaxYAngularVelocity(float maxYAngularVelocity) {
        this.maxYAngularVelocity = maxYAngularVelocity;
    }

    public float getMaxZAngularVelocity() {
        return maxZAngularVelocity;
    }

    public void setMaxZAngularVelocity(float maxZAngularVelocity) {
        this.maxZAngularVelocity = maxZAngularVelocity;
    }

    public float getMinXAngularVelocity() {
        return minXAngularVelocity;
    }

    public void setMinXAngularVelocity(float minXAngularVelocity) {
        this.minXAngularVelocity = minXAngularVelocity;
    }

    public float getMinYAngularVelocity() {
        return minYAngularVelocity;
    }

    public void setMinYAngularVelocity(float minYAngularVelocity) {
        this.minYAngularVelocity = minYAngularVelocity;
    }

    public float getMinZAngularVelocity() {
        return minZAngularVelocity;
    }

    public void setMinZAngularVelocity(float minZAngularVelocity) {
        this.minZAngularVelocity = minZAngularVelocity;
    }

    public float getMaxXVelToScreenWidthRatio() {
        return maxXVelToScreenWidthRatio;
    }

    public void setMaxXVelToScreenWidthRatio(float maxXVelToScreenWidthRatio) {
        this.maxXVelToScreenWidthRatio = maxXVelToScreenWidthRatio;
    }

    public float getMaxYVelToScreenWidthRatio() {
        return maxYVelToScreenWidthRatio;
    }

    public void setMaxYVelToScreenWidthRatio(float maxYVelToScreenWidthRatio) {
        this.maxYVelToScreenWidthRatio = maxYVelToScreenWidthRatio;
    }

    public float getMaxZVelToScreenWidthRatio() {
        return maxZVelToScreenWidthRatio;
    }

    public void setMaxZVelToScreenWidthRatio(float maxZVelToScreenWidthRatio) {
        this.maxZVelToScreenWidthRatio = maxZVelToScreenWidthRatio;
    }

    public int getFrameLimitBeforeReversing() {
        return frameLimitBeforeReversing;
    }

    public void setFrameLimitBeforeReversing(int frameLimitBeforeReversing) {
        this.frameLimitBeforeReversing = frameLimitBeforeReversing;
    }

    public int getNumberOfCoordinates() {
        return numberOfCoordinates;
    }

    public void setNumberOfCoordinates(int numberOfCoordinates) {
        this.numberOfCoordinates = numberOfCoordinates;
    }

    public float getVariationRatio() {
        return variationRatio;
    }

    public void setVariationRatio(float variationRatio) {
        this.variationRatio = variationRatio;
    }

    public float getInitialVelocity() {
        return initialVelocity;
    }

    public void setInitialVelocity(float initialVelocity) {
        this.initialVelocity = initialVelocity;
    }

    public int getFramesForConstantVel() {
        return framesForConstantVel;
    }

    public void setFramesForConstantVel(int framesForConstantVel) {
        this.framesForConstantVel = framesForConstantVel;
    }

    public int getLengthOfPauseAtExpansionLimit() {
        return lengthOfPauseAtExpansionLimit;
    }

    public void setLengthOfPauseAtExpansionLimit(int lengthOfPauseAtExpansionLimit) {
        this.lengthOfPauseAtExpansionLimit = lengthOfPauseAtExpansionLimit;
    }

    public float getExpansionRatio() {
        return expansionRatio;
    }

    public void setExpansionRatio(float expansionRatio) {
        this.expansionRatio = expansionRatio;
    }
}

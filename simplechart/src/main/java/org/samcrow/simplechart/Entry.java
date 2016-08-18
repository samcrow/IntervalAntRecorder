package org.samcrow.simplechart;

/**
 * A graph entry
 */
public class Entry<T> {

    /**
     * The X coordinate of this entry
     */
    private final float mX;

    /**
     * The Y coordinate of this entry
     */
    private final float mY;

    /**
     * The data of this entry
     */
    private final T mData;

    public Entry(float x, float y, T data) {
        mX = x;
        mY = y;
        mData = data;
    }

    public Entry(float x, float y) {
        this(x, y, null);
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public T getData() {
        return mData;
    }
}

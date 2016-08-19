package org.samcrow.simplechart;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Stores and manages the data used in a chart
 */
class DataModel<T> {

    /**
     * The smallest possible maximum axis value
     */
    private static final float LEAST_MAX = 1;

    /**
     * The entries
     */
    private List<Entry<T>> mEntries;

    public void setEntries(List<Entry<T>> entries) {
        mEntries = entries;
    }

    public float getMinX() {
        return 0;
    }

    public float getMinY() {
        return 0;
    }

    public float getMaxX() {
        if (mEntries != null) {
            final float maxData = getMaxDataX();
            if (maxData > LEAST_MAX) {
                return LEAST_MAX;
            } else {
                return maxData;
            }
        } else {
            return LEAST_MAX;
        }
    }

    public float getMaxY() {
        if (mEntries != null) {
            final float maxData = getMaxDataY();
            if (maxData > LEAST_MAX) {
                return LEAST_MAX;
            } else {
                return maxData;
            }
        } else {
            return LEAST_MAX;
        }
    }

    public Iterator<Entry<T>> entries() {
        if (mEntries != null) {
            return mEntries.iterator();
        } else {
            return Collections.<Entry<T>>emptyList().iterator();
        }
    }

    /**
     * Calculates and returns the maximum X value of all entries. If no entries are available,
     * or if no entry has a positive X value, returns 0.
     * @return the maximum X value
     */
    private float getMaxDataX() {
        float max = 0;
        if (mEntries != null) {
            for (Entry<T> entry : mEntries) {
                if (entry.getX() > max) {
                    max = entry.getX();
                }
            }
        }
        return max;
    }

    /**
     * Calculates and returns the maximum Y value of all entries. If no entries are available,
     * or if no entry has a positive Y value, returns 0.
     * @return the maximum Y value
     */
    private float getMaxDataY() {
        float max = 0;
        if (mEntries != null) {
            for (Entry<T> entry : mEntries) {
                if (entry.getY() > max) {
                    max = entry.getY();
                }
            }
        }
        return max;
    }
}

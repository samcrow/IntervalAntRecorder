package org.samcrow.antrecorder;

import com.androidplot.xy.XYSeries;

/**
 * Draws a line from (0, 0) to (1, 1)
 */
public class OneToOneSeries implements XYSeries {
    @Override
    public int size() {
        return 2;
    }

    @Override
    public Number getX(int index) {
        if (index == 0) {
            return 0;
        } else if (index == 1) {
            return 1;
        } else {
            return null;
        }
    }

    @Override
    public Number getY(int index) {
        if (index == 0) {
            return 0;
        } else if (index == 1) {
            return 1;
        } else {
            return null;
        }
    }

    @Override
    public String getTitle() {
        return "1:1";
    }
}

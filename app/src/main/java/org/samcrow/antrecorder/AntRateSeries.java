package org.samcrow.antrecorder;

import android.util.Log;

import com.androidplot.xy.XYSeries;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.samcrow.antrecorder.EventModel.IntervalRates;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;

/**
 * A data series that provides ant rates
 */
public class AntRateSeries implements XYSeries {

    /**
     * The model
     */
    private final EventModel mModel;

    public AntRateSeries(EventModel model) {
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        mModel = model;
    }

    @Override
    public int size() {
        return mModel.getIntervalCount();
    }

    @Override
    public Number getX(int index) {
        try {
            return calculatePoint(index).x;
        } catch (IOException | ParseException e) {
            Log.e(AntRateSeries.class.getSimpleName(), "Failed to read file", e);
            return null;
        }
    }

    @Override
    public Number getY(int index) {
        try {
            return calculatePoint(index).y;
        } catch (IOException | ParseException e) {
            Log.e(AntRateSeries.class.getSimpleName(), "Failed to read file", e);
            return null;
        }
    }

    @Override
    public String getTitle() {
        return "Ant rates";
    }

    private XY calculatePoint(int index) throws IOException, ParseException {
        final Iterator<IntervalRates> rates = mModel.intervalRatesIterator();
        // Skip to get to the correct index
        for (int i = 0; i < index; i++) {
            rates.next();
        }
        final IntervalRates requestedIntervalRates = rates.next();
        return new XY(requestedIntervalRates.mInRate, requestedIntervalRates.mOutRate);
    }


    private static class XY {
        public double x;
        public double y;

        public XY(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "XY{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

}

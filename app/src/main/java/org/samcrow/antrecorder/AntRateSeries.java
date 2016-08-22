package org.samcrow.antrecorder;

import android.util.Log;
import android.util.SparseArray;

import com.androidplot.xy.XYSeries;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.text.ParseException;

/**
 * A data series that provides ant rates
 */
public class AntRateSeries implements XYSeries {

    /**
     * The time interval between points on the graph
     */
    private static final Duration INTERVAL = Duration.standardMinutes(1);

    /**
     * The event file
     */
    private final EventFile mFile;
    /**
     * The model
     */
    private final CountModel mModel;

    /**
     * The cached points
     */
    private final SparseArray<XY> mCache;

    public AntRateSeries(EventFile file, CountModel model) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        mFile = file;
        mModel = model;
        mCache = new SparseArray<>();
    }

    @Override
    public int size() {
        try {
            final Event firstEvent = mFile.getFirstEvent();
            if (firstEvent != null) {
                final DateTime start = firstEvent.getTime();
                final DateTime now = DateTime.now();
                final Duration elapsedTime = new Duration(start, now);
                // Return number of intervals that have passed, rounded up
                final int size = (int) Math.ceil(elapsedTime.getMillis() / (double) INTERVAL.getMillis());
                return size;
            } else {
                // No event in file
                return 0;
            }
        } catch (IOException | ParseException e) {
            Log.e(AntRateSeries.class.getSimpleName(), "Failed to read file", e);
            return 0;
        }
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
        final DateTime start = mFile.getFirstEvent().getTime();
        final DateTime regionEnd = start.withDurationAdded(INTERVAL, index + 1);
        final double inRate = mModel.getInRate(regionEnd, INTERVAL);
        final double outRate = mModel.getOutRate(regionEnd, INTERVAL);
        return new XY(inRate, outRate);
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

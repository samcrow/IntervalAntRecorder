package org.samcrow.antrecorder;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.samcrow.antrecorder.Event.Type;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Stores events
 */
public class EventModel {

    /**
     * The length of blocks in which to take rate measurements
     */
    private final Duration mBlockDuration;

    /**
     * The intervals in this model, with the earliest at the head and the latest at the tail
     */
    private final Deque<Interval> mIntervals;

    public EventModel(Duration blockDuration) {
        mBlockDuration = blockDuration;
        mIntervals = new ArrayDeque<>();
    }

    /**
     * Adds an event to this model
     * @param event the event to add. The event must have a time later than the time of all events
     *              in this model.
     */
    public void add(Event event) {
        final Event last = getLast();
        if (last != null && event.getTime().compareTo(last.getTime()) < 0) {
            throw new IllegalArgumentException("The provided event is not newer than all other events in this model");
        }


        if (mIntervals.isEmpty()) {
            // Create an interval
            final Interval interval = new Interval(event.getTime());
            interval.mEvents.addLast(event);
            mIntervals.addLast(interval);
        } else {
            final Interval lastInterval = mIntervals.peekLast();
            final DateTime lastIntervalEnd = lastInterval.mStartTime.withDurationAdded(mBlockDuration, 1);
            if (lastIntervalEnd.isAfter(event.getTime())) {
                // This interval is still in progress
                lastInterval.mEvents.addLast(event);
            } else {
                // TODO: Add empty intervals for any periods in between
                // Make a new interval
                // Find the start time
                DateTime intervalStart = lastIntervalEnd;
                while (intervalStart.isBefore(event.getTime())) {
                    intervalStart = intervalStart.withDurationAdded(mBlockDuration, 1);
                }
                // intervalStart is now in the future. Move it back.
                intervalStart = intervalStart.withDurationAdded(mBlockDuration, -1);
                if (!intervalStart.isAfter(lastInterval.mStartTime)) {
                    throw new AssertionError("New interval is not after the last one");
                }

                final Interval newInterval = new Interval(intervalStart);
                newInterval.mEvents.addLast(event);
                mIntervals.addLast(newInterval);
            }
        }
    }

    /**
     * Removes the last event from this file
     * @return true if an event was removed, or false is this model is empty
     */
    public boolean removeLast() {
        final Interval lastInterval = mIntervals.peekLast();
        if (lastInterval != null) {
            final boolean removed = lastInterval.mEvents.removeLast() != null;
            if (removed) {
                // Check for an empty interval
                if (lastInterval.mEvents.isEmpty()) {
                    mIntervals.removeLast();
                }
            }
            return removed;
        } else {
            return false;
        }
    }

    public ReadableDuration getBlockInterval() {
        return mBlockDuration;
    }

    public int getInCount() {
        int count = 0;
        for (Iterator<Event> iter = eventIterator(); iter.hasNext(); ) {
            final Event event = iter.next();
            if (event.getType() == Type.AntIn) {
                count++;
            }
        }
        return count;
    }
    public int getOutCount() {
        int count = 0;
        for (Iterator<Event> iter = eventIterator(); iter.hasNext(); ) {
            final Event event = iter.next();
            if (event.getType() == Type.AntOut) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the last event in this model
     * @return the event, or null if this model is empty
     */
    private Event getLast() {
        if (mIntervals.isEmpty()) {
            return null;
        } else {
            final Interval interval = mIntervals.peekLast();
            return interval.mEvents.peekLast();
        }
    }

    public Event getFirst() {
        final Interval firstInterval = mIntervals.peekFirst();
        if (firstInterval != null) {
            return firstInterval.mEvents.peekFirst();
        } else {
            return null;
        }
    }

    public int getIntervalCount() {
        return mIntervals.size();
    }

    /**
     * Returns an iterator over the events in this model, from earliest to latest
     * @return an iterator
     */
    public Iterator<Event> eventIterator() {
        return new EventIterator(mIntervals.iterator());
    }

    public Iterator<IntervalRates> intervalRatesIterator() {
        return new IntervalIterator(mIntervals.iterator());
    }


    private double intervalInRate(Interval interval) {
        return interval.inCount() / ((double) mBlockDuration.getMillis() / 1000.0);
    }
    private double intervalOutRate(Interval interval) {
        return interval.outCount() / ((double) mBlockDuration.getMillis() / 1000.0);
    }

    /**
     * An interval of time, containing events that occured during that time
     */
    private static class Interval {
        /**
         * The start time of this interval
         */
        public final DateTime mStartTime;
        /**
         * The events in the time range [mStartTime, EventModel.this.mBlockDuration),
         * with the earliest at the head and the latest at the tail
         */
        public final Deque<Event> mEvents;

        public Interval(DateTime startTime) {
            mStartTime = startTime;
            mEvents = new ArrayDeque<>();
        }

        public int inCount() {
            int count = 0;
            for (Event event : mEvents) {
                if (event.getType() == Type.AntIn) {
                    count++;
                }
            }
            return count;
        }
        public int outCount() {
            int count = 0;
            for (Event event : mEvents) {
                if (event.getType() == Type.AntOut) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * An iterator over all events in an EventModel
     */
    private static class EventIterator implements Iterator<Event> {

        /**
         * The iterator over intervals
         */
        private final Iterator<Interval> mIntervalIterator;

        /**
         * The iterator over events in the current interval
         */
        private Iterator<Event> mEventIterator;

        public EventIterator(
                Iterator<Interval> intervalIterator) {
            mIntervalIterator = intervalIterator;
        }

        @Override
        public boolean hasNext() {
            if (mEventIterator == null || !mEventIterator.hasNext()) {
                if (mIntervalIterator.hasNext()) {
                    mEventIterator = mIntervalIterator.next().mEvents.iterator();
                } else {
                    // No current interval, and no more available
                    return false;
                }
            }
            // Now mEventIterator is not null
            return mEventIterator.hasNext();
        }

        @Override
        public Event next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more events");
            }
            return mEventIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    /**
     * Represents an interval and its rates
     */
    public static class IntervalRates {
        /**
         * The start time of the interval
         */
        private final DateTime mStartTime;
        /**
         * The rate of in events, in events per second
         */
        public final double mInRate;
        /**
         * The rate of out events, in events per second
         */
        public final double mOutRate;

        public IntervalRates(DateTime startTime, double inRate, double outRate) {
            mStartTime = startTime;
            mInRate = inRate;
            mOutRate = outRate;
        }
    }

    private class IntervalIterator implements Iterator<IntervalRates> {

        private final Iterator<Interval> mIntervalIterator;

        private IntervalIterator(Iterator<Interval> intervalIterator) {
            mIntervalIterator = intervalIterator;
        }

        @Override
        public boolean hasNext() {
            return mIntervalIterator.hasNext();
        }

        @Override
        public IntervalRates next() {
            final Interval interval = mIntervalIterator.next();
            return new IntervalRates(interval.mStartTime, intervalInRate(interval), intervalOutRate(interval));
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}

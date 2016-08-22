package org.samcrow.antrecorder;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.samcrow.antrecorder.Event.Type;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps track of numbers of events recorded
 */
public class CountModel {

    public static class Status {
        /**
         * The number of ants that have arrived
         */
        public final int inCount;
        /**
         * The number of ants that have left
         */
        public final int outCount;
        /**
         * The ratio of outgoing ants to incoming ants
         */
        public final double outRatio;
        /**
         * The result of subtracting the in count from the out count,
         * or the number of outgoing ants who have not returned
         */
        public final int outDifference;
        /**
         * The rate of incoming ants over the last minute
         */
        public final double inRate;
        /**
         * The rate of outgoing ants over the last minute
         */
        public final double outRate;

        public Status(int inCount, int outCount, double outRatio, int outDifference, double inRate, double outRate) {
            this.inCount = inCount;
            this.outCount = outCount;
            this.outRatio = outRatio;
            this.outDifference = outDifference;
            this.outRate = outRate;
            this.inRate = inRate;
        }
    }

    /**
     * Queue of out events, with the oldest event at the head and the
     * newest at the tail
     */
    private final Deque<Event> outQueue = new ArrayDeque<>();
    /**
     * Queue of in events, with the oldest event at the head and the
     * newest at the tail
     */
    private final Deque<Event> inQueue = new ArrayDeque<>();

    /**
     * Creates a new model with no events
     */
    public CountModel() {

    }

    /**
     * Creates a new model by reading events from a file
     * @param file the file to read from
     */
    public CountModel(EventFile file) throws IOException, ParseException {
        final List<Event> events = file.getEvents();
        for (Event event : events) {
            process(event);
        }
    }

    public void process(Event event) {
        if (event == null) {
            throw new NullPointerException("event must not be null");
        }

        if (event.getType() == Type.AntIn) {
            inQueue.add(event);
        } else {
            outQueue.add(event);
        }
    }

    /**
     * @return the number of outgoing ants
     */
    public int getInCount() {
        return inQueue.size();
    }

    /**
     * @return the number of incoming ants
     */
    public int getOutCount() {
        return outQueue.size();
    }

    /**
     * Calculates the average rate of events over the provided duration
     * @param queue a queue of events
     * @param duration the duration over which to calculate the rate
     * @return the average rate of events, in events per second
     */
    private double getRate(Deque<Event> queue, Duration duration) {
        final DateTime threshold = DateTime.now().minus(duration);
        // Count the events in the time range
        int count = 0;
        // Iterate from the tail to the head
        for (Iterator<Event> iter = queue.descendingIterator(); iter.hasNext(); ) {
            final Event event = iter.next();
            if (event.getTime().isBefore(threshold)) {
                break;
            }
            count++;
        }

        final long seconds = duration.getStandardSeconds();

        return (double) count / (double) seconds;
    }

    public Status getStatus() {

        final Duration rateDuration = Duration.standardMinutes(1);

        final int inCount = getInCount();
        final int outCount = getOutCount();
        return new Status(inCount,
                outCount,
                outCount / (double) inCount,
                outCount - inCount,
                getRate(inQueue, rateDuration), getRate(outQueue, rateDuration)
        );
    }

    /**
     * Deletes the last entry from the model
     */
    public void deleteLast() {
        final Event lastIn = inQueue.peekLast();
        final Event lastOut = outQueue.peekLast();
        if (lastIn != null && lastOut == null) {
            inQueue.removeLast();
        } else if (lastIn == null && lastOut != null) {
            outQueue.removeLast();
        } else //noinspection StatementWithEmptyBody
            if (lastIn != null) {
            // Neither is null
            // Delete the newer one
            if (lastIn.getTime().compareTo(lastOut.getTime()) > 0) {
                inQueue.removeLast();
            } else {
                outQueue.removeLast();
            }
        } else {
            // Both null
            // Do nothing
        }
    }

    /**
     * Calculates and returns the rate of in events
     * @param end the end of the time range to evaluate
     * @param duration the duration of the time range to evaluate
     * @return the rate of in events, in events per second, over the provided time range
     */
    public double getInRate(DateTime end, Duration duration) {
        return getRate(inQueue, end.minus(duration), end);
    }
    /**
     * Calculates and returns the rate of out events
     * @param end the end of the time range to evaluate
     * @param duration the duration of the time range to evaluate
     * @return the rate of out events, in events per second, over the provided time range
     */
    public double getOutRate(DateTime end, Duration duration) {
        return getRate(outQueue, end.minus(duration), end);
    }

    private double getRate(Deque<Event> events, DateTime start, DateTime end) {
        // Iterate from earliest to latest
        int count = 0;
        for (Event event : events) {
            if (event.getTime().isAfter(end)) {
                break;
            }
            if (event.getTime().isAfter(start)) {
                count++;
            }
        }
        final long durationSeconds = new Duration(start, end).getStandardSeconds();
        return count / (double) durationSeconds;
    }
}

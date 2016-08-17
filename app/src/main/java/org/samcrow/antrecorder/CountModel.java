package org.samcrow.antrecorder;

import android.content.Context;
import android.content.SharedPreferences;

import org.joda.time.DateTime;
import org.samcrow.antrecorder.Event.Type;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Keeps track of numbers of events recorded
 */
public class CountModel {
	private static final String PREFS_TAG = CountModel.class.getName();

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
	 * Incoming ant count
	 */
	private int inCount = 0;
	/**
	 * Outgoing ant count
	 */
	private int outCount = 0;

	/**
	 * Queue of out events in the last minute, with the oldest event at the head and the
	 * newest at the tail
	 */
	private final Queue<Event> outQueue = new ArrayDeque<>();
	/**
	 * Queue of in events in the last , with the oldest event at the head and the
	 * newest at the tail
	 */
	private final Queue<Event> inQueue = new ArrayDeque<>();

	/**
	 * Creates a new model
	 */
	public CountModel() {

	}

	/**
	 * Restores a CountModel from saved preferences
	 * @param source the context to get preferences from
	 * @return a restored CountModel, or null if none could be restored
	 */
	public static CountModel restore(Context source) {
		final SharedPreferences prefs = source.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE);
		if(prefs.contains("in_count") && prefs.contains("out_count")) {
			final CountModel model = new CountModel();
			model.inCount = prefs.getInt("in_count", 0);
			model.outCount = prefs.getInt("out_count", 0);
			return model;
		}
		else {
			return null;
		}
	}

	/**
	 * Saves this CountModel to the preferences
	 * @param context the context to get preferences from
	 */
	public void save(Context context) {
		final SharedPreferences prefs = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE);
		prefs.edit()
				.putInt("in_count", inCount)
				.putInt("out_count", outCount)
				.apply();
	}

	public void process(Event event) {
		if (event == null) {
			throw new NullPointerException("event must not be null");
		}

		if (event.getType() == Type.AntIn) {
			incrementIn();
			inQueue.add(event);
		} else {
			incrementOut();
			outQueue.add(event);
		}
		trimQueues();
	}

	/**
	 * Increases by one the number of incoming ants
	 */
	private void incrementIn() {
		inCount++;
	}

	/**
	 * Increases by one the number of outgoing ants
	 */
	private void incrementOut() {
		outCount++;
	}

	/**
	 * @return the number of outgoing ants
	 */
	public int getInCount() {
		return inCount;
	}

	/**
	 * @return the number of incoming ants
	 */
	public int getOutCount() {
		return outCount;
	}

	public Status getStatus() {
		trimQueues();
		return new Status(inCount,
				outCount,
				outCount / (double) inCount,
				outCount - inCount,
				inQueue.size(), outQueue.size()
		);
	}

	private void trimQueues() {
		trimQueue(inQueue);
		trimQueue(outQueue);
	}

	private static void trimQueue(Queue<Event> queue) {
		final DateTime oneMinuteAgo = DateTime.now().minusMinutes(1);

		for (final Iterator<Event> iter = queue.iterator(); iter.hasNext(); ) {
			final Event event = iter.next();
			if (event.getTime().isBefore(oneMinuteAgo)) {
				iter.remove();
			} else {
				// The other elements are more recent
				break;
			}
		}
	}
}

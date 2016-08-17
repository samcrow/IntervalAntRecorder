package org.samcrow.antrecorder;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * An event
 */
public class Event implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Type {
		AntIn,
		AntOut,
	}

	/**
	 * The event type
	 */
	private final Type type;
	/**
	 * The time the event was recorded
	 */
	private final DateTime time;

	public Event(Type type, DateTime time) {
		this.type = type;
		this.time = time;
	}


	public Type getType() {
		return type;
	}

	public DateTime getTime() {
		return time;
	}
}

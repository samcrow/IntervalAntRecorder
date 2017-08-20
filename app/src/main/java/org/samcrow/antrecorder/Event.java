package org.samcrow.antrecorder;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

/**
 * An event
 */
public final class Event implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Type {
		AntIn,
		AntOut,
	}

	/**
	 * The event type
	 */
	@NonNull
	private final Type type;
	/**
	 * The time the event was recorded
	 */
	@NonNull
	private final DateTime time;

	public Event(@NonNull Type type, @NonNull DateTime time) {
		this.type = type;
		this.time = time;
	}


	@NonNull
	public Type getType() {
		return type;
	}

	@NonNull
	public DateTime getTime() {
		return time;
	}
}

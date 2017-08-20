package org.samcrow.antrecorder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An action to be performed on a file
 */
public final class FileAction {

	/**
	 * The type of this action
	 */
	@NonNull
	private Type mType;

	/**
	 * The event, if the type of this action is {@link Type#Event}, otherwise null
	 */
	@Nullable
	private Event mEvent;

	public enum Type {
		Event,
		DeleteLast,
		Shutdown,
	}

	/**
	 * Creates an event file action with the provided event
	 * @param event the event to include
	 * @return a new FileAction containing the event
	 */
	public static FileAction event(Event event) {
		return new FileAction(Type.Event, event);
	}

	/**
	 * Creates a DeleteLast event
	 * @return a new event
	 */
	public static FileAction deleteLast() {
		return new FileAction(Type.DeleteLast, null);
	}

	/**
	 * Creates a Shutdown event
	 * @return a new event
	 */
	public static FileAction shutdown() {
		return new FileAction(Type.Shutdown, null);
	}

	private FileAction(@NonNull Type type, @Nullable Event event) {
		mType = type;
		mEvent = event;
	}

	@NonNull
	public Type getType() {
		return mType;
	}

	public @Nullable Event getEvent() {
		return mEvent;
	}
}

package org.samcrow.antrecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronously writes events to a file
 *
 * When run, an object of this class reads actions from a queue.
 *
 * An action with type {@link org.samcrow.antrecorder.FileAction.Type#Event} and an included
 * {@link Event} causes the event to be written to the file provided in the constructor.
 *
 * An action with type {@link org.samcrow.antrecorder.FileAction.Type#DeleteLast} causes the event
 * most recently written to the file to be deleted.
 *
 * An action with type {@link org.samcrow.antrecorder.FileAction.Type#Shutdown} causes the writing
 * process to stop. After such an event is processed, no more events will be read.
 */
public final class EventWriter implements Runnable {

	public interface WriteExceptionHandler {
		void handleException(IOException e);
	}

	/**
	 * The UTF-8 character set
	 */
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final String TAG = EventWriter.class.getSimpleName();

	/**
	 * The file to write to
	 *
	 * Invariant: When no function of this class is executing, the file pointer (offset) is at the
	 * end of the file.
	 */
	@NonNull
	private final RandomAccessFile mFile;

	/**
	 * A queue of actions to perform
	 */
	@NonNull
	private final BlockingQueue<FileAction> mActionQueue;

	/**
	 * The handler used to move exceptions to the UI thread
	 */
	@NonNull
	private final Handler mHandler;

	@NonNull
	private final WriteExceptionHandler mExceptionHandler;


	/**
	 * Creates an EventWriter to write to a file
	 * @param file the file to write to
	 * @param exceptionHandler a handler that will be given any exception that occurs when writing.
	 *                         The handler will be called on the main (UI) thread.
	 * @throws IOException if the file could not be created or written to
	 */
	public EventWriter(@NonNull File file, @NonNull WriteExceptionHandler exceptionHandler) throws IOException {
		mFile = new RandomAccessFile(file, "rw");
		mActionQueue = new LinkedBlockingQueue<>();
		mExceptionHandler = exceptionHandler;
		mHandler = new Handler();

		// Read the file to count the existing events
	}

	/**
	 * Returns a thread-safe queue that can be used to send events to this writer
	 * @return a queue
	 */
	public Queue<FileAction> getQueue() {
		return mActionQueue;
	}

	@Override
	public void run() {
		try {
			while (true) {
				try {
					final FileAction action = mActionQueue.take();
					final FileAction.Type actionType = action.getType();
					if (actionType.equals(FileAction.Type.Event)) {
						final Event event = action.getEvent();
						writeEvent(event);
					} else if (actionType.equals(FileAction.Type.DeleteLast)) {
						deleteLast();
					} else if (actionType.equals(FileAction.Type.Shutdown)) {
						break;
					}
				} catch (InterruptedException ignored) {

				}
			}
		} catch (final IOException e) {
			Log.e(TAG, "Failed to write", e);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mExceptionHandler.handleException(e);
				}
			});
		} finally {
			try {
				mFile.close();
			} catch (final IOException e) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mExceptionHandler.handleException(e);
					}
				});
			}
		}
	}

	private void writeEvent(Event event) throws IOException {
		final String timeString = event.getTime().toString(ISODateTimeFormat.dateTime());
		final String eventString = event.getType().equals(Event.Type.AntIn) ? "In" : "Out";
		final String line = timeString + "," + eventString + "\n";

		final byte[] encoded = line.getBytes(UTF8);
		mFile.write(encoded);
	}

	private void deleteLast() throws IOException {
		// Seek backwards until the beginning of the file or a newline is reached
		long offset = mFile.getFilePointer();
		while (true) {
			if (offset == 0) {
				// Already at beginning: Delete everything
				mFile.setLength(0);
				return;
			}
			offset -= 1;
			mFile.seek(offset);
			final byte current = readByte();
			if (current == '\n') {
				// offset is the position of the first byte of the record to be deleted
				// (one after the \n)
				mFile.setLength(offset);
			}
		}
	}

	private void callExceptionHandler(final IOException e) {

	}

	/**
	 * Reads one byte from mFile and returns it
	 * @return the byte read
	 * @throws IOException if an error occurs or if mFile is at the end of file
	 */
	private byte readByte() throws IOException {
		final int readInt = mFile.readByte();
		if (readInt == -1) {
			throw new IOException("Reached end of file when trying to read");
		} else {
			return (byte) readInt;
		}
	}
}

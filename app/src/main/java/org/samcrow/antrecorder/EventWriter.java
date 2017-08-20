package org.samcrow.antrecorder;

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

	public interface WriteHandler {
		void handleException(IOException e);
		void countsUpdated(int inCount, int outCount);
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
	private final WriteHandler mExceptionHandler;

	private int mInCount;
	private int mOutCount;


	/**
	 * Creates an EventWriter to write to a file
	 * @param file the file to write to
	 * @param exceptionHandler a handler that will be given any exception that occurs when writing.
	 *                         The handler will be called on the main (UI) thread.
	 * @throws IOException if the file could not be created or written to
	 */
	public EventWriter(@NonNull File file, @NonNull WriteHandler exceptionHandler) throws IOException {
		mFile = new RandomAccessFile(file, "rw");
		mActionQueue = new LinkedBlockingQueue<>();
		mExceptionHandler = exceptionHandler;
		mHandler = new Handler();
		mInCount = 0;
		mOutCount = 0;
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
			// Count existing events
			countEventsInFile();
			callCountHandler();
			mFile.seek(mFile.length());

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
			callExceptionHandler(e);
		} finally {
			try {
				mFile.close();
			} catch (final IOException e) {
				callExceptionHandler(e);
			}
		}
	}

	private void writeEvent(Event event) throws IOException {
		final String timeString = event.getTime().toString(ISODateTimeFormat.dateTime());
		final String eventString = event.getType().equals(Event.Type.AntIn) ? "In" : "Out";
		final String line = timeString + "," + eventString + "\n";

		final byte[] encoded = line.getBytes(UTF8);
		mFile.write(encoded);
		if (event.getType().equals(Event.Type.AntIn)) {
			mInCount++;
		} else if (event.getType().equals(Event.Type.AntOut)) {
			mOutCount++;
		}
		callCountHandler();
	}

	private void deleteLast() throws IOException {
		// Can be slow
		// Reread everything
		mFile.seek(0);
		String line = mFile.readLine();
		while (true) {
			final String newLine = mFile.readLine();
			if (newLine == null) {
				// line contains the last line of the file, to be deleted
				final int lineLength = line.length() + 1;
				mFile.setLength(mFile.length() - lineLength);
			} else {
				line = newLine;
			}
		}
	}

	private void countEventsInFile() throws IOException {
		mInCount = 0;
		mOutCount = 0;
		mFile.seek(0);
		String line;
		while ((line = mFile.readLine()) != null) {
			final String[] parts = line.split(",");
			if (parts.length < 2) {
				continue;
			}
			final String inOut = parts[1];
			if (inOut.equals("In")) {
				mInCount++;
			} else if (inOut.equals("Out")) {
				mOutCount++;
			}
		}
	}

	private void callExceptionHandler(final IOException e) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mExceptionHandler.handleException(e);
			}
		});
	}

	private void callCountHandler() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mExceptionHandler.countsUpdated(mInCount, mOutCount);
			}
		});
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

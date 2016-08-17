package org.samcrow.antrecorder;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Writes events to a file
 * <p/>
 * Intents sent to this service must have the URL of the file to append to as the URL of the
 * intent.
 * <p/>
 * Intents sent to ths service must have an extras bundle with an "action" value containing
 * a serialized action to be recorded.
 * <p/>
 * This service broadcasts a {@link #BROADCAST_SUCCESS} intent when it finishes writing an event,
 * with the {@link Event} as the "event" extra.
 * <p/>
 * If an error occurs, this service broadcasts a {@link #BROADCAST_FAILURE} intent. If the error
 * was due to an exception, the intent contains that exception as the "exception" extra.
 * Otherwise, it contains a message string as a "message" extra.
 */
public class FileWriteService extends IntentService {
    private static final String TAG = FileWriteService.class.getSimpleName();

    public static final String BROADCAST_SUCCESS = FileWriteService.class.getName() + ".BROADCAST_SUCCESS";
    public static final String BROADCAST_FAILURE = FileWriteService.class.getName() + ".BROADCAST_FAILURE";

    public FileWriteService() {
        super(FileWriteService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final File file = new File(intent.getData().getPath());
        final Event event = (Event) intent.getSerializableExtra("event");
        if (event == null) {
            String message = "No event was provided in the intent";
            Log.e(TAG, message);
            reportFailure(message);
            return;
        }
        try {
            PrintStream stream = null;
            try {
                stream = new PrintStream(new FileOutputStream(file, true));

                final String timeString = ISODateTimeFormat.dateTime().print(event.getTime());

                stream.print(timeString);
                stream.print(',');
                if (event.getType() == Type.AntIn) {
                    stream.print("In");
                } else {
                    stream.print("Out");
                }
                stream.println();
                stream.flush();

                reportSuccess(event);
            } catch (IOException e) {
                Log.e(TAG, "Could not write to file " + file.getAbsolutePath(), e);
                reportFailure(e);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }

        } catch (ClassCastException e) {
            Log.e(TAG, "The event provided is not an Event", e);
            reportFailure(e);
        } catch (NullPointerException e) {
            Log.e(TAG, "Invalid intent", e);
            reportFailure(e);
        }
    }

    private void reportSuccess(Event event) {
        final Intent intent = new Intent(BROADCAST_SUCCESS);
        intent.putExtra("event", event);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportFailure(String message) {
        final Intent intent = new Intent(BROADCAST_FAILURE);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void reportFailure(Exception e) {
        final Intent intent = new Intent(BROADCAST_FAILURE);
        intent.putExtra("exception", e);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}

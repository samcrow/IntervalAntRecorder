package org.samcrow.antrecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * Updates an event file with events from a model
 */
public class FileUpdater extends TimerTask {

    /**
     * The event model that provides events
     */
    private final EventModel mModel;

    /**
     * The file to write to
     */
    private final File mFile;

    /**
     * The handler used to run events on the main thread
     */
    private final Handler mHandler;

    /**
     * The context
     */
    private final Context mContext;

    public FileUpdater(Context context, EventModel model, File file) {
        mContext = context;
        mHandler = new Handler();
        mModel = model;
        mFile = file;
    }

    /**
     * Runs the file updater. Returns when the thread is interrupted.
     */
    @Override
    public void run() {
        try {
            {
                // Truncate the file
                final RandomAccessFile random = new RandomAccessFile(mFile, "rw");
                try {
                    random.setLength(0);
                } finally {
                    random.close();
                }
            }
            final PrintStream stream = new PrintStream(new FileOutputStream(mFile));
            try {
                synchronized (mModel) {
                    for (Iterator<Event> iter = mModel.eventIterator(); iter.hasNext(); ) {
                        final Event event = iter.next();
                        final String timeString = ISODateTimeFormat.dateTime()
                                .print(event.getTime());

                        stream.print(timeString);
                        stream.print(',');
                        if (event.getType() == Type.AntIn) {
                            stream.print("In");
                        } else {
                            stream.print("Out");
                        }
                        stream.println();
                    }
                }
            } finally {
                stream.close();
            }
        } catch (final Exception e) {
            Log.e(FileUpdater.class.getSimpleName(), "Failed to write", e);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(mContext)
                            .setTitle("Failed to save events")
                            .setMessage(e.getLocalizedMessage())
                            .show();
                }
            });
        }
    }
}

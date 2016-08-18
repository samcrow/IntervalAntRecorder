package org.samcrow.antrecorder;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.antrecorder.Event.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to an event file
 */
public class EventFile {

    /**
     * The file
     */
    private final RandomAccessFile mFile;

    /**
     * Creates a new event file, or opens an existing file
     * @param path the path to the file to open
     * @throws FileNotFoundException if the file could not be created
     */
    public EventFile(String path) throws FileNotFoundException {
        mFile = new RandomAccessFile(new File(path), "rw");
    }

    /**
     * Reads the first event from the file
     * @return the event that was read, or null if no event exists
     * @throws IOException if reading failed
     * @throws ParseException if an event could not be parsed
     */
    public Event getFirstEvent() throws IOException, ParseException {
        mFile.seek(0);
        final String line = mFile.readLine();
        if (line != null) {
            return parseEventFromLine(line, 1);
        } else {
            return null;
        }
    }

    private Event parseEventFromLine(String line, int location) throws ParseException {
        final String[] parts = line.split(",");
        if (parts.length != 2) {
            throw new ParseException("Expected two comma-separated parts", location);
        }
        try {
            final DateTimeFormatter timeParser = ISODateTimeFormat.dateTimeParser();
            final DateTime time = timeParser.parseDateTime(parts[0]);
            Event.Type eventType;
            switch (parts[1]) {
                case "In":
                    eventType = Type.AntIn;
                    break;
                case "Out":
                    eventType = Type.AntOut;
                    break;
                default:
                    throw new ParseException("Invalid event type", location);
            }
            return new Event(eventType, time);
        } catch (IllegalArgumentException e) {
            final ParseException parseException = new ParseException("Invalid date/time format", location);
            parseException.initCause(e);
            throw parseException;
        }
    }

    /**
     * Reads all events from the file
     * @return the events that were read
     * @throws IOException if reading failed
     * @throws ParseException if an event could not be parsed
     */
    public List<Event> getEvents() throws IOException, ParseException {
        mFile.seek(0);
        final List<Event> events = new ArrayList<>();


        String line;
        int lineNumber = 1;
        while ((line = mFile.readLine()) != null) {
            events.add(parseEventFromLine(line, lineNumber));
            lineNumber++;
        }
        return events;
    }

    /**
     * Appends an event to the end of the file
     * @param event the event to append
     * @throws IOException if a failure occurs
     */
    public void appendEvent(Event event) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(64);
        final PrintStream stream = new PrintStream(byteStream);

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

        // Seek to end and write
        mFile.seek(mFile.length());
        mFile.write(byteStream.toByteArray());
    }

    /**
     * Removes the last event from the file
     * @throws IOException
     */
    public void removeLastEvent() throws IOException {
        if (mFile.length() < 2) {
            // Nothing to delete
            return;
        }

        // Seek to the end, then go back to the newline before the last line
        // Start 2 before the file length, so that the first character read will be just before
        // the newline at the end of the file
        mFile.seek(mFile.length() - 2);
        // Move back until a newline is found
        while (mFile.getFilePointer() != 0) {
            mFile.seek(mFile.getFilePointer() - 1);
            final byte thisCharacter = mFile.readByte();
            // Move back again to undo the forward movement caused by the write
            mFile.seek(mFile.getFilePointer() - 1);
            final byte newline = (byte) 0x0A;
            if (thisCharacter == newline) {
                // At the end of the line
                // Truncate the file to this length
                mFile.setLength(mFile.getFilePointer() + 1);
                break;
            }
        }
        // Beginning of file reached
        if (mFile.getFilePointer() == 0) {
            mFile.setLength(0);
        }
    }

}

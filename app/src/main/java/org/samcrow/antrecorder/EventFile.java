package org.samcrow.antrecorder;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.antrecorder.Event.Type;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

    public static EventModel readModel(File source, Duration interval) throws IOException, ParseException {
        final BufferedReader reader = new BufferedReader(new FileReader(source));
        final EventModel model = new EventModel(interval);
        try {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                final Event event = parseEventFromLine(line, lineNumber);
                model.add(event);
                lineNumber++;
            }
        } finally {
            reader.close();
        }
        return model;
    }

    private static Event parseEventFromLine(String line, int location) throws ParseException {
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
}

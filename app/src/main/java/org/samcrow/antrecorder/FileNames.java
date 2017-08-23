package org.samcrow.antrecorder;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Utilities for data set file names
 */

public final class FileNames {
	/**
	 * The prefix for file names
	 */
	private static final String FILE_PREFIX = "color_events_";
	/**
	 * The suffix for file names
	 */
	private static final String FILE_SUFFIX = ".csv";

	/**
	 * Creates a file name from a data set name
	 * @param dataSet a data set name
	 * @return a corresponding file name
	 * @throws IllegalArgumentException if the data set name contains characters not allowed
	 * in a file name
	 */
	public static @NonNull String createFileName(@NonNull String dataSet) {
		if (dataSet.contains("/") || dataSet.contains(":")) {
			throw new IllegalArgumentException("A data set name may not contain a / or : character");
		}
		return FILE_PREFIX + dataSet + FILE_SUFFIX;
	}

	/**
	 * Extracts a data set name from a file name
	 * @param fileName a file name
	 * @return the data set name from the file
	 * @throws IllegalArgumentException if the file name does not have the correct format
	 */
	public static @NonNull String extractDataSet(@NonNull String fileName) {
		if (!(fileName.startsWith(FILE_PREFIX) && fileName.endsWith(FILE_SUFFIX))) {
			throw new IllegalArgumentException("Invalid file name format");
		}
		final int suffixIndex = fileName.lastIndexOf(FILE_SUFFIX);
		return fileName.substring(FILE_PREFIX.length(), suffixIndex);
	}

	/**
	 * A filter that accepts color event CSV files
	 */
	public static class ColorEventsFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
		}
	}
}

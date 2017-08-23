package org.samcrow.antrecorder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Shows recent data sets in a directory
 */

public final class RecentDataSetModel extends BaseAdapter implements AdapterView.OnItemClickListener {

	public interface DataSetSelectedListener {
		void dataSetSelected(String dataSetName, File file);
	}

	private static class Entry {
		/**
		 * The data set name
		 */
		@NonNull
		String mName;
		/**
		 * The file containing the data set
		 */
		@NonNull
		File mFile;
		Entry(@NonNull String name, @NonNull File file) {
			mName = name;
			mFile = file;
		}
	}

	private final Context mContext;

	/**
	 * The data set entries
	 */
	private final List<Entry> mEntries;

	/**
	 * The data set selected listener
	 */
	private DataSetSelectedListener mListener;

	/**
	 * Creates a model
	 * @param memoryCard a File containing the absolute path to the memory card directory
	 */
	public RecentDataSetModel(@NonNull Context context, @NonNull File memoryCard) {
		if (!memoryCard.isDirectory()) {
			throw new IllegalArgumentException("Memory card must be a directory");
		}

		mContext = context;

		final File[] files = memoryCard.listFiles(new FileNames.ColorEventsFilter());
		// Sort by time modified descending
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				return (int) (f2.lastModified() - f1.lastModified());
			}
		});

		// Build the entry list
		mEntries = new ArrayList<>(files.length);
		for (File file : files) {
			try {
				final String dataSet = FileNames.extractDataSet(file.getName());
				mEntries.add(new Entry(dataSet, file));
			} catch (IllegalArgumentException ignored) {
				// Ignore this file
			}
		}

		mListener = null;
	}

	public void setListener(DataSetSelectedListener listener) {
		mListener = listener;
	}

	@Override
	public int getCount() {
		return mEntries.size();
	}

	@Override
	public Object getItem(int position) {
		return mEntries.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Entry entry = mEntries.get(position);
		if (mListener != null) {
			mListener.dataSetSelected(entry.mName, entry.mFile);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view;
		if (convertView instanceof TextView) {
			view = (TextView) convertView;
		} else {
			view = createTextView();
		}
		final String name = mEntries.get(position).mName;
		view.setText(name);
		return view;
	}

	private TextView createTextView() {
		final TextView view = new TextView(mContext);
		view.setTextSize(mContext.getResources().getDimensionPixelSize(R.dimen.setup_text_size));
		final int padding = mContext.getResources().getDimensionPixelSize(R.dimen.setup_padding);
		view.setPadding(padding, padding, padding, padding);
		return view;
	}

}

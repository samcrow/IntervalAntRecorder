package org.samcrow.antrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.joda.time.DateTime;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final float VOLUME = 0.5f;

	private EditText dataSetField;
	private Button inButton;
	private Button outButton;
	private View enterNameLabel;

	private TextView inCountField;
	private TextView outCountField;

	/**
	 * The menu item used to delete entries
	 */
	private MenuItem mDeleteItem;

	private SoundPool sound;
	private int inSoundId;
	private int outSoundId;

	/**
	 * The service that runs file operations
	 */
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	/**
	 * A thread-safe queue storing the events to be written
	 */
	private Queue<FileAction> mFileQueue;

	/**
	 * Tries to find the location of the SD card on the file system. If no known directories
	 * are available, returns some other external storage directory.
	 *
	 * @return a directory for storage, which may be a memory card
	 */
	public static File getMemoryCard() {
		File dir = new File("/mnt/extSdCard");
		if (dir.exists() && dir.isDirectory()) {
			return dir;
		}
		dir = new File("/Removable/MicroSD");
		if (dir.exists() && dir.isDirectory()) {
			return dir;
		}
		dir = new File("/storage/extSdCard");
		if (dir.exists() && dir.isDirectory()) {
			return dir;
		}
		return Environment.getExternalStorageDirectory();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		inButton = (Button) findViewById(R.id.in_button);
		outButton = (Button) findViewById(R.id.out_button);
		dataSetField = (EditText) findViewById(R.id.data_set_field);
		enterNameLabel = findViewById(R.id.enter_data_set_label);
		inCountField = (TextView) findViewById(R.id.in_count);
		outCountField = (TextView) findViewById(R.id.out_count);

		// Disable buttons (they will be enabled when a data set is entered)
		setButtonsEnabled(false);

		dataSetField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					// Done key pressed, or otherwise lost focus
					updateDataSet();
				}
			}
		});

		inButton.getBackground().setColorFilter(Color.parseColor("#8BC34A"), Mode.OVERLAY);
		outButton.getBackground().setColorFilter(Color.parseColor("#FFCA28"), Mode.OVERLAY);

		inButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveEvent(new Event(Type.AntIn, DateTime.now()));
				sound.play(inSoundId, VOLUME, VOLUME, 1, 0, 1);
			}
		});
		outButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveEvent(new Event(Type.AntOut, DateTime.now()));
				sound.play(outSoundId, VOLUME, VOLUME, 1, 0, 1);
			}
		});

		// Set up sound
		sound = new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
		inSoundId = sound.load(this, R.raw.ping_high, 1);
		outSoundId = sound.load(this, R.raw.ping_low, 1);
	}

	private void updateDataSet() {
		if (dataSetField.getText().length() != 0) {
			// Some data set was selected

			inCountField.setText("-");
			outCountField.setText("-");

			setButtonsEnabled(true);
			updateCountLabels();

			try {
				final EventWriter writer = new EventWriter(this, getDataPath());
				mFileQueue = writer.getQueue();
				mExecutor.submit(writer);
			} catch (IOException e) {
				Log.e(TAG, "Failed to open file", e);
				new AlertDialog.Builder(this)
						.setTitle("Failed to open file")
						.setMessage(e.getLocalizedMessage())
						.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						})
						.show();
			}

		} else {
			// No data set
			if (mFileQueue != null) {
				// Tell the file process to shut down
				mFileQueue.add(FileAction.shutdown());
				mFileQueue = null;
			}
			setButtonsEnabled(false);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveDatasetName();
	}

	@Override
	protected void onResume() {
		super.onResume();
		restoreDatasetName();
		updateDataSet();
		updateCountLabels();
	}

	private void saveDatasetName() {
		final SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(),
				MODE_PRIVATE);
		prefs.edit().putString("dataset_name", dataSetField.getText().toString()).apply();
	}

	private void restoreDatasetName() {
		final SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(),
				MODE_PRIVATE);
		final String name = prefs.getString("dataset_name", "");
		dataSetField.setText(name);
	}

	private void updateCountLabels() {
		// TODO
	}

	private void setButtonsEnabled(boolean enabled) {
		inButton.setEnabled(enabled);
		outButton.setEnabled(enabled);
		enterNameLabel.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
		if (mDeleteItem != null) {
			mDeleteItem.setEnabled(enabled);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		mDeleteItem = menu.findItem(R.id.action_delete);
		mDeleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mFileQueue.add(FileAction.deleteLast());
				return true;
			}
		});

		return true;
	}

	private File getDataPath() {
		return new File(getMemoryCard(),
				"Ant events " + dataSetField.getText().toString() + ".csv");
	}

	private void saveEvent(@NonNull Event event) {
		if (mFileQueue != null) {
			mFileQueue.add(FileAction.event(event));
		} else {
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("No data set selected")
					.setMessage("Please enter a data set name")
					.show();
		}
	}
}

package org.samcrow.antrecorder;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An activity for recording events
 *
 * This activity must be started with an extra with key {@link #EXTRA_FILE_PATH} corresponding to
 * the path to the file to write to.
 *
 * It may be given an extra with key {@link #EXTRA_DATA_SET} containing the data set name.
 */
public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	/**
	 * The volume level for sounds
	 */
	private static final float VOLUME = 0.5f;

	public static final String EXTRA_FILE_PATH = MainActivity.class.getName() + ".EXTRA_FILE_PATH";
	public static final String EXTRA_DATA_SET = MainActivity.class.getName() + ".EXTRA_DATA_SET";

	private TextView mInCountField;
	private TextView mOutCountField;

	private SoundPool mSoundPool;
	private int mInSoundId;
	private int mOutSoundId;

	/**
	 * The service that runs file operations
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private ExecutorService mExecutor;

	/**
	 * A thread-safe queue storing the events to be written
	 */
	private Queue<FileAction> mFileQueue;

	/**
	 * A click listener that calls {@link #finish()} on this activity
	 */
	private final DialogInterface.OnClickListener FINISH_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			finish();
		}
	};
	private EventWriter mWriter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ActionBar bar = getSupportActionBar();
		if (bar != null) {
			bar.setDisplayHomeAsUpEnabled(true);
		}

		final String dataSetName = getIntent().getStringExtra(EXTRA_DATA_SET);
		if (dataSetName != null) {
			setTitle(dataSetName);
		}

		final String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
		if (filePath == null) {
			throw new IllegalStateException("This activity must be started with a file path extra");
		}
		// Set up the file writer
		try {
			mExecutor = Executors.newSingleThreadExecutor();
			mWriter = new EventWriter(new File(filePath), new EventWriter.WriteHandler() {
				@Override
				public void handleException(IOException e) {
					new AlertDialog.Builder(MainActivity.this)
							.setTitle("Failed to write event")
							.setMessage(e.getLocalizedMessage())
							.setNeutralButton(android.R.string.ok, FINISH_LISTENER)
							.show();
				}

				@Override
				public void countsUpdated(int inCount, int outCount) {
					mInCountField.setText(String.format(Locale.getDefault(), "%d", inCount));
					mOutCountField.setText(String.format(Locale.getDefault(), "%d", outCount));
				}
			});
			mFileQueue = mWriter.getQueue();
			mExecutor.submit(mWriter);
		} catch (IOException e) {
			new AlertDialog.Builder(this)
					.setTitle("Failed to open file")
					.setMessage(e.getLocalizedMessage())
					.setNeutralButton(android.R.string.ok, FINISH_LISTENER);
		}


		Button inButton = (Button) findViewById(R.id.in_button);
		Button outButton = (Button) findViewById(R.id.out_button);
		mInCountField = (TextView) findViewById(R.id.in_count);
		mOutCountField = (TextView) findViewById(R.id.out_count);

		ViewCompat.setBackgroundTintList(inButton, getResources().getColorStateList(R.color.in_background));
		ViewCompat.setBackgroundTintList(outButton, getResources().getColorStateList(R.color.out_background));


		inButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveEvent(new Event(Type.AntIn, DateTime.now()));
				mSoundPool.play(mInSoundId, VOLUME, VOLUME, 1, 0, 1);
			}
		});
		outButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveEvent(new Event(Type.AntOut, DateTime.now()));
				mSoundPool.play(mOutSoundId, VOLUME, VOLUME, 1, 0, 1);
			}
		});

		// Set up mSoundPool
		mSoundPool = new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
		mInSoundId = mSoundPool.load(this, R.raw.ping_high, 1);
		mOutSoundId = mSoundPool.load(this, R.raw.ping_low, 1);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		/*
	  The menu item used to delete entries
	 */
		MenuItem deleteItem = menu.findItem(R.id.action_delete);
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				mFileQueue.add(FileAction.deleteLast());
				return true;
			}
		});

		return true;
	}

	private void saveEvent(@NonNull Event event) {
		if (!mWriter.isRunning()) {
			new AlertDialog.Builder(this)
					.setTitle("Writer not running")
					.setMessage("Due to an internal error, the writer task is not running.")
					.setNeutralButton(android.R.string.ok, FINISH_LISTENER)
					.show();
		}
		if (mFileQueue.size() > 8) {
			new AlertDialog.Builder(this)
					.setTitle("Writer not keeping up")
					.setMessage("The writer task has not kept up with events.")
					.setNeutralButton(android.R.string.ok, FINISH_LISTENER)
					.show();
		}
		mFileQueue.add(FileAction.event(event));
	}
}

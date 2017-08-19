package org.samcrow.antrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final float VOLUME = 0.5f;

    /**
     * The duration over which to calculate ant rates
     */
    private static final Duration RATE_INTERVAL = Duration.standardMinutes(5);
    /**
     * The interval, in milliseconds, between file writes
     */
    private static final long WRITE_INTERVAL = 10000;

    /**
     * The interval, in milliseconds, between graph updates
     */
    private static final long GRAPH_INTERVAL = 1000;

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

    /**
     * The rate chart
     */
    private XYPlot mChart;

    /**
     * The current event model, or null if no dataset is active
     */
    private EventModel mModel;

    /**
     * The timer used to schedule file write tasks
     */
    private Timer mWriteTimer;

    /**
     * The timer used to schedule graph updates
     */
    private Timer mGraphTimer;

    private SoundPool sound;
    private int inSoundId;
    private int outSoundId;
    private FileUpdater mUpdateTask;

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

        mChart = (XYPlot) findViewById(R.id.chart);
        mChart.setDomainBoundaries(0, BoundaryMode.FIXED, 1, BoundaryMode.FIXED);
        mChart.setRangeBoundaries(0, BoundaryMode.FIXED, 1, BoundaryMode.FIXED);
        mChart.getLegendWidget().setVisible(false);


        // Disable buttons (they will be enabled when a data set is entered)
        setButtonsEnabled(false);

        dataSetField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Done key pressed
                updateDataSet();
                return false;
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

            final File dataFile = getDataPath();
            if (dataFile.isFile()) {
                try {
                    mModel = EventFile.readModel(dataFile, RATE_INTERVAL);
                } catch (IOException | ParseException e) {
                    new AlertDialog.Builder(this)
                            .setTitle("Failed to read existing events")
                            .setMessage(e.getLocalizedMessage())
                            .show();
                    mModel = new EventModel(RATE_INTERVAL);
                }
            } else {
                mModel = new EventModel(RATE_INTERVAL);
            }

            mUpdateTask = new FileUpdater(this, mModel, dataFile);
            if (mWriteTimer != null) {
                mWriteTimer.cancel();
            }
            mWriteTimer = new Timer();
            mWriteTimer.schedule(mUpdateTask, WRITE_INTERVAL, WRITE_INTERVAL);

            if (mGraphTimer != null) {
                mGraphTimer.cancel();
            }
            mGraphTimer = new Timer();
            mGraphTimer.schedule(new GraphUpdater(), GRAPH_INTERVAL, GRAPH_INTERVAL);

            mChart.clear();
            final Paint noFill = new Paint();
            noFill.setAlpha(0);
            {
                // 1:1 series
                final LineAndPointFormatter formatter11 = new LineAndPointFormatter();
                formatter11.configure(this, R.xml.one_to_one_line_formatter);
                formatter11.setFillPaint(noFill);

                mChart.addSeries(new OneToOneSeries(), formatter11);
            }
            final LineAndPointFormatter formatter = new LineAndPointFormatter();
            formatter.configure(this, R.xml.line_formatter);
            formatter.setFillPaint(noFill);
            mChart.addSeries(new AntRateSeries(mModel), formatter);
            mChart.redraw();

            setButtonsEnabled(true);
            updateChartData();
            updateCountLabels();

        } else {
            // No data set
            setButtonsEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDatasetName();
        if (mWriteTimer != null) {
            mWriteTimer.cancel();
            mWriteTimer = null;
            // Ensure that events are written
            if (mUpdateTask != null) {
                mUpdateTask.run();
            }
        }

        if (mGraphTimer != null) {
            mGraphTimer.cancel();
            mGraphTimer = null;
        }
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
        if (mModel != null) {
            synchronized (mModel) {
                inCountField.setText(String.format(Locale.getDefault(), "%d", mModel.getInCount()));
                outCountField.setText(
                        String.format(Locale.getDefault(), "%d", mModel.getOutCount()));
            }
        } else {
            inCountField.setText("-");
            outCountField.setText("-");
        }
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
                if (mModel != null) {
                    synchronized (mModel) {
                        mModel.removeLast();
                        updateCountLabels();
                        updateChartData();
                        Toast.makeText(MainActivity.this, R.string.deleted_entry,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }


                return true;
            }
        });

        return true;
    }

    private File getDataPath() {
        return new File(getMemoryCard(),
                "Ant events " + dataSetField.getText().toString() + ".csv");
    }

    private void saveEvent(Event event) {
        if (mModel != null) {
            synchronized (mModel) {
                mModel.add(event);
            }
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("No data set selected")
                    .setMessage("Please enter a data set name")
                    .show();
        }
    }

    private void updateChartData() {
        mChart.redraw();
    }


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

    /**
     * Periodically updates the graph and count labels
     */
    private class GraphUpdater extends TimerTask {

        private final Handler mHandler = new Handler();

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mModel) {
                        updateCountLabels();
                        updateChartData();
                    }
                }
            });
        }
    }
}

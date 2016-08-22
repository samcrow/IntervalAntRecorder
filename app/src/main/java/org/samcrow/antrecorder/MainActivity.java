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

import com.androidplot.Plot;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeriesFormatter;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.samcrow.antrecorder.Event.Type;
import org.samcrow.simplechart.Chart;
import org.samcrow.simplechart.Entry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

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

    /**
     * The rate chart
     */
    private XYPlot mChart;

    /**
     * The current event file, or null if no dataset name has been entered
     */
    private EventFile mFile;

    private CountModel model;

    private SoundPool sound;
    private int inSoundId;
    private int outSoundId;

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
        mChart.setDomainBoundaries(0, BoundaryMode.FIXED, null, BoundaryMode.AUTO);
        mChart.setRangeBoundaries(0, BoundaryMode.FIXED, null, BoundaryMode.AUTO);
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

            inCountField.setText("");
            outCountField.setText("");

            try {
                mFile = new EventFile(getDataPath());
                model = new CountModel(mFile);

                final LineAndPointFormatter formatter = new LineAndPointFormatter();
                formatter.configure(this, R.xml.line_formatter);
                final Paint noFill = new Paint();
                noFill.setAlpha(0);
                formatter.setFillPaint(noFill);

                mChart.clear();
                mChart.addSeries(new AntRateSeries(mFile, model), formatter);
                mChart.redraw();

                setButtonsEnabled(true);
                updateChartData();

            } catch (FileNotFoundException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Failed to open or create data file")
                        .setMessage(e.getLocalizedMessage())
                        .show();
            } catch (ParseException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Failed to parse data file")
                        .setMessage(e.getLocalizedMessage())
                        .show();
            } catch (IOException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Failed to read from data file")
                        .setMessage(e.getLocalizedMessage())
                        .show();
            }
        } else {
            // No data set
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
        if (model != null) {
            inCountField.setText(String.format(Locale.getDefault(), "%d", model.getInCount()));
            outCountField.setText(String.format(Locale.getDefault(), "%d", model.getOutCount()));
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
                try {
                    if (mFile != null) {
                        mFile.removeLastEvent();
                        model.deleteLast();
                        updateChartData();
                        Toast.makeText(MainActivity.this, R.string.deleted_entry,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (IOException e) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Failed to delete entry")
                            .setMessage(e.getLocalizedMessage())
                            .show();
                }


                return true;
            }
        });

        return true;
    }

    private String getDataPath() {
        return new File(getMemoryCard(),
                "Ant events " + dataSetField.getText().toString() + ".csv").getAbsolutePath();
    }

    private void saveEvent(Event event) {
        if (mFile != null) {
            try {
                mFile.appendEvent(event);
                model.process(event);
                updateCountLabels();
                updateChartData();
            } catch (IOException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Failed to save entry")
                        .setMessage(e.getLocalizedMessage())
                        .show();
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
}

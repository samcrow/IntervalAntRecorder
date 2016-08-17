package org.samcrow.antrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.samcrow.antrecorder.Event.Type;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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

        // Disable buttons (they will be enabled when a data set is entered)
        inButton.setEnabled(false);
        outButton.setEnabled(false);

        dataSetField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // Enable buttons if text is empty
                // Disable if not empty
                if (dataSetField.getText().toString().isEmpty()) {
                    setButtonsEnabled(false);
                } else {
                    setButtonsEnabled(true);
                }
                // Reset counts
                model = new CountModel();
                updateCountLabels();
            }
        });

        inButton.getBackground().setColorFilter(Color.parseColor("#8BC34A"), Mode.OVERLAY);
        outButton.getBackground().setColorFilter(Color.parseColor("#FFCA28"), Mode.OVERLAY);

        inButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSaveEvent(getDataFile(), new Event(Type.AntIn, DateTime.now()));
                sound.play(inSoundId, VOLUME, VOLUME, 1, 0, 1);
            }
        });
        outButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSaveEvent(getDataFile(), new Event(Type.AntOut, DateTime.now()));
                sound.play(outSoundId, VOLUME, VOLUME, 1, 0, 1);
            }
        });

        // Set up handlers for intents from the write task
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Success
                final Event event = (Event) intent.getSerializableExtra("event");
                model.process(event);
                updateCountLabels();
            }
        }, new IntentFilter(FileWriteService.BROADCAST_SUCCESS));

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Failure
                if (intent.hasExtra("exception")) {
                    final Exception ex = (Exception) intent.getSerializableExtra("exception");
                    showErrorDialog(ex.getClass().getSimpleName(), ex.getMessage());
                } else {
                    showErrorDialog("Failed to write", intent.getStringExtra("message"));
                }
            }
        }, new IntentFilter(FileWriteService.BROADCAST_FAILURE));

        // Set up sound
        sound = new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
        inSoundId = sound.load(this, R.raw.ping_high, 1);
        outSoundId = sound.load(this, R.raw.ping_low, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        model.save(this);
        saveDatasetName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreDatasetName();
        model = CountModel.restore(this);
        if (model == null) {
            model = new CountModel();
        }
        updateCountLabels();
    }

    private void saveDatasetName() {
        final SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE);
        prefs.edit().putString("dataset_name", dataSetField.getText().toString()).apply();
    }

    private void restoreDatasetName() {
        final SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE);
        final String name = prefs.getString("dataset_name", "");
        dataSetField.setText(name);
    }

    private void updateCountLabels() {
        inCountField.setText(String.format(Locale.getDefault(), "%d", model.getInCount()));
        outCountField.setText(String.format(Locale.getDefault(), "%d", model.getOutCount()));
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing
                    }
                })
                .show();
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
                    deleteLastEntry(getDataFile());
                    Toast.makeText(MainActivity.this, R.string.deleted_entry, Toast.LENGTH_SHORT)
                            .show();
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

    /**
     * Deletes the last entry from the provided file
     *
     * @param file the file to delete from
     * @throws IOException
     */
    private void deleteLastEntry(File file) throws IOException {
        final RandomAccessFile random = new RandomAccessFile(file, "rw");
        if (file.length() < 2) {
            // Nothing to delete
            return;
        }

        // Seek to the end, then go back to the newline before the last line
        // Start 2 before the file length, so that the first character read will be just before
        // the newline at the end of the file
        random.seek(random.length() - 2);
        // Move back until a newline is found
        while (random.getFilePointer() != 0) {
            random.seek(random.getFilePointer() - 1);
            final byte thisCharacter = random.readByte();
            // Move back again to undo the forward movement caused by the write
            random.seek(random.getFilePointer() - 1);
            final byte newline = (byte) 0x0A;
            if (thisCharacter == newline) {
                // At the end of the line
                // Truncate the file to this length
                random.setLength(random.getFilePointer() + 1);
                break;
            }
        }
        if (random.getFilePointer() == 0) {
            random.setLength(0);
        }
    }

    private File getDataFile() {
        return new File(getMemoryCard(),
                "Ant events " + dataSetField.getText().toString() + ".csv");
    }

    private void startSaveEvent(File file, Event event) {

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG, "External storage state is " + Environment.getExternalStorageState());
        }

        final Intent intent = new Intent(this, FileWriteService.class);
        intent.setData(Uri.parse(file.toURI().toString()));
        intent.putExtra("event", event);
        startService(intent);
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
        dir = new File("/storage/6133-3731");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        return Environment.getExternalStorageDirectory();
    }
}

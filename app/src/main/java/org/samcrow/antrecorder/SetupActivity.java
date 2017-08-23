package org.samcrow.antrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;

public class SetupActivity extends AppCompatActivity {

	private static final int REQUEST_PERMISSIONS = 9;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);

		final String[] permissions = {
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
		};
		boolean allGranted = true;
		for (String permission : permissions) {
			if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				allGranted = false;
			}
		}
		if (allGranted) {
			postPermissionSetup();
		} else {
			ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_PERMISSIONS) {
			if (permissions.length > 0) {
				boolean allGranted = true;
				for (int result : grantResults) {
					if (result != PackageManager.PERMISSION_GRANTED) {
						allGranted = false;
					}
				}
				if (allGranted) {
					postPermissionSetup();
				} else {
					finish();
				}
			} else {
				finish();
			}
		}
	}

	private void postPermissionSetup() {
		final ListView recentDataSetsList = (ListView) findViewById(R.id.recent_data_set_list);
		final RecentDataSetModel adapter = new RecentDataSetModel(this, Storage.getMemoryCard());
		adapter.setListener(new RecentDataSetModel.DataSetSelectedListener() {
			@Override
			public void dataSetSelected(String dataSetName, File file) {
				startEntryActivity(dataSetName, file);
			}
		});
		recentDataSetsList.setAdapter(adapter);
		recentDataSetsList.setOnItemClickListener(adapter);

		final EditText nameField = (EditText) findViewById(R.id.data_set_field);

		final Button startButton = (Button) findViewById(R.id.setup_start_button);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final CharSequence dataSet = nameField.getText();
				if (dataSet.length() > 0) {
					try {
						final String fileName = FileNames.createFileName(dataSet.toString());
						final File dataFile = new File(Storage.getMemoryCard(), fileName);
						startEntryActivity(dataSet.toString(), dataFile);
					} catch (IllegalArgumentException e) {
						new AlertDialog.Builder(SetupActivity.this)
								.setTitle("Invalid data set name")
								.setMessage(e.getLocalizedMessage())
								.show();
					}
				} else {
					new AlertDialog.Builder(SetupActivity.this)
							.setTitle("Empty data set name")
							.setMessage("Please enter a data set name")
							.show();
				}
			}
		});
	}

	private void startEntryActivity(String dataSet, File dataSetFile) {
		final Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra(MainActivity.EXTRA_DATA_SET, dataSet);
		intent.putExtra(MainActivity.EXTRA_FILE_PATH, dataSetFile.getAbsolutePath());
		startActivity(intent);
	}
}

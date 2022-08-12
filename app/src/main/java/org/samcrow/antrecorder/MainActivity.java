package org.samcrow.antrecorder;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An activity for counting events
 */
public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();

	/**
	 * The volume level for sounds
	 */
	private static final float VOLUME = 1.0f;

	private TextView mInCountField;
	private TextView mOutCountField;

	/** Field to display countdown */
	private TextView mTimeField;


    private Button mInButton;
    private Button mOutButton;
    private Button mStartButton;

	private SoundPool mSoundPool;
	private int mInSoundId;
	private int mOutSoundId;
	private int mCountdownExpiredSoundId;

	private int mInCount = 0;
	private int mOutCount = 0;
    /**
     * Seconds remaining in the interval, rounded down
     */
	private int mSeconds = 0;

    /**
     * The timer used to count down
     */
    private Timer mTimer;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mInButton = (Button) findViewById(R.id.in_button);
        mOutButton = (Button) findViewById(R.id.out_button);
        mStartButton = (Button) findViewById(R.id.start_button);
		mInCountField = (TextView) findViewById(R.id.in_count);
		mOutCountField = (TextView) findViewById(R.id.out_count);
		mTimeField = (TextView) findViewById(R.id.time_label);

		ViewCompat.setBackgroundTintList(mInButton, AppCompatResources.getColorStateList(this, R.color.in_background));
		ViewCompat.setBackgroundTintList(mOutButton, AppCompatResources.getColorStateList(this, R.color.out_background));

		setInCount(0);
		setOutCount(0);

		mInButton.setEnabled(false);
		mOutButton.setEnabled(false);

		// Set up sound pool
		mSoundPool = new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
		mInSoundId = mSoundPool.load(this, R.raw.ping_high, 1);
		mOutSoundId = mSoundPool.load(this, R.raw.ping_low, 1);
		mCountdownExpiredSoundId = mSoundPool.load(this, R.raw.sound_notification_double, 1);
	}

	public void onInButtonClicked(View v) {
        setInCount(mInCount + 1);
        mSoundPool.play(mInSoundId, VOLUME, VOLUME, 1, 0, 1);
    }

    public void onOutButtonClicked(View v) {
        setOutCount(mOutCount + 1);
        mSoundPool.play(mOutSoundId, VOLUME, VOLUME, 1, 0, 1);
    }

    public void onStartButtonClicked(View v) {
        setInCount(0);
        setOutCount(0);
        setSeconds(29);
        mStartButton.setEnabled(false);
        mInButton.setEnabled(true);
        mOutButton.setEnabled(true);
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new CountdownTask(), 1000, 1000);
    }

    private void setSeconds(int seconds) {
        mSeconds = seconds;
        mTimeField.post(new Runnable() {
            @Override
            public void run() {
                mTimeField.setText(String.format(Locale.getDefault(), "%d", mSeconds));
            }
        });
    }

    private void setInCount(int count) {
        mInCount = count;
        mInCountField.setText(String.format(Locale.getDefault(), "%d", mInCount));
    }
    private void setOutCount(int count) {
        mOutCount = count;
        mOutCountField.setText(String.format(Locale.getDefault(), "%d", mOutCount));
    }

    private class CountdownTask extends TimerTask {

        @Override
        public void run() {
            if (mSeconds == 0) {
                // Stop counting
                mTimer.cancel();
                mTimer = null;
                mStartButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mStartButton.setEnabled(true);
                        mSoundPool.play(mCountdownExpiredSoundId, VOLUME, VOLUME, 1, 0, 1);
                    }
                });
                mInButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mInButton.setEnabled(false);
                    }
                });
                mOutButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mOutButton.setEnabled(false);
                    }
                });
            } else {
                setSeconds(mSeconds - 1);
            }
        }
    }
}

package com.kircherelectronics.lowpasslinearacceleration;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.kircherelectronics.lowpasslinearacceleration.filter.LPFAndroidDeveloper;
import com.kircherelectronics.lowpasslinearacceleration.filter.LPFWikipedia;
import com.kircherelectronics.lowpasslinearacceleration.filter.LowPassFilter;

import java.util.HashSet;
import java.util.Set;

import nebmo.pedometer.AccelerationInfo;
import nebmo.pedometer.Pedometer;


/**
 * Created by niklas.weidemann on 2014-06-17.
 */
public class StepCounterService extends Service implements StepCounterInteractor {
	private long _sensorEventTime;
	public AccelerationInfo _sensorInfo;
	private int _eventFrequency = 25;
	private final Set<OnStepsCountedListener> mListeners = new HashSet<OnStepsCountedListener>();
	private final IBinder mBinder = new StepServiceBinder();
	private SensorManager mSensorManager;
	private long mStepsCounted;
	private boolean mIsListening;
	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];
	private float[] lpfWikiOutput = new float[4];
	private float[] lpfAndDevOutput = new float[3];
	// Low-Pass Filters
	private LowPassFilter lpfWiki;
	private LowPassFilter lpfAndDev;

	// The static alpha for the LPF Wikipedia
	private static float WIKI_STATIC_ALPHA = 0.1f;
	// The static alpha for the LPF Android Developer
	private static float AND_DEV_STATIC_ALPHA = 0.9f;

	// Indicate if a static alpha should be used for the LPF Wikipedia
	private boolean staticWikiAlpha = false;

	// Indicate if a static alpha should be used for the LPF Android Developer
	private boolean staticAndDevAlpha = false;
	private Pedometer _pedometer;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		lpfWiki = new LPFWikipedia();
		lpfAndDev = new LPFAndroidDeveloper();
		mIsListening = false;
		mStepsCounted = 0;

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		_pedometer = new Pedometer();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void startListening() {
		if (mIsListening)
			return;

		final Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(mSensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		mIsListening = true;
	}



	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			long lastSensorTime = _sensorEventTime;
			_sensorEventTime = System.currentTimeMillis();
			System.arraycopy(event.values, 0, acceleration, 0, event.values.length);

			acceleration[0] = acceleration[0] / SensorManager.GRAVITY_EARTH;
			acceleration[1] = acceleration[1] / SensorManager.GRAVITY_EARTH;
			acceleration[2] = acceleration[2] / SensorManager.GRAVITY_EARTH;

			lpfWikiOutput = lpfWiki.addSamples(acceleration);
			lpfAndDevOutput = lpfAndDev.addSamples(acceleration);

			_sensorInfo = new AccelerationInfo();
			_sensorInfo.x = acceleration[0];
			_sensorInfo.y = acceleration[1];
			_sensorInfo.z = acceleration[2];
			_sensorInfo.wx = lpfWikiOutput[0];
			_sensorInfo.wy = lpfWikiOutput[1];
			_sensorInfo.wz = lpfWikiOutput[2];
			_sensorInfo.time = _sensorEventTime;

			if(_sensorEventTime - lastSensorTime > (1000/_eventFrequency )){
				_pedometer.onInput(_sensorInfo);
				notifySensorChanged();
				int steps = _pedometer.getSteps();
				if(steps > mStepsCounted){
					mStepsCounted = steps;
					notifyStepsChanged();
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

	/**
	 * Initialize the filters.
	 */
	private void initFilters() {
		// Create the low-pass filters
		lpfWiki = new LPFWikipedia();
		lpfAndDev = new LPFAndroidDeveloper();

		// Initialize the low-pass filters with the saved prefs
		lpfWiki.setAlphaStatic(staticWikiAlpha);
		lpfWiki.setAlpha(WIKI_STATIC_ALPHA);

		lpfAndDev.setAlphaStatic(staticAndDevAlpha);
		lpfAndDev.setAlpha(AND_DEV_STATIC_ALPHA);
	}

	private void notifyStepsChanged() {
		synchronized (mListeners) {
			for (OnStepsCountedListener listener : mListeners) {
				listener.onStepsCounted(this);
			}
		}
	}

	private void notifySensorChanged() {
		synchronized (mListeners) {
			for (OnStepsCountedListener listener : mListeners) {
				listener.onSensorChanged(this);
			}
		}
	}

	@Override
	public void stopListener() {
		mIsListening = false;
		mSensorManager.unregisterListener(mSensorEventListener);
	}

	@Override
	public void registerOnStepsCountedListener(OnStepsCountedListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener is null");

		synchronized (mListeners) {
			if(!mListeners.contains(listener))
				mListeners.add(listener);
		}
	}

	@Override
	public void unregisterOnStepsCountedListener(OnStepsCountedListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener is null");

		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	@Override
	public long getCountedSteps() {
		return mStepsCounted;
	}

	@Override
	public AccelerationInfo getAccelerationInfo() {
		return _sensorInfo;
	}

	@Override
	public boolean isListening() {
		return mIsListening;
	}

	public class StepServiceBinder extends Binder {
		public StepCounterInteractor getStepInteractor() {
			return StepCounterService.this;
		}
	}
}


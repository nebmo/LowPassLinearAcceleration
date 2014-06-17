package com.kircherelectronics.lowpasslinearacceleration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Calendar;
import com.androidplot.xy.XYPlot;
import com.kircherelectronics.lowpasslinearacceleration.dialog.SettingsDialog;
import com.kircherelectronics.lowpasslinearacceleration.filter.LPFAndroidDeveloper;
import com.kircherelectronics.lowpasslinearacceleration.filter.LPFWikipedia;
import com.kircherelectronics.lowpasslinearacceleration.filter.LowPassFilter;
import com.kircherelectronics.lowpasslinearacceleration.gauge.GaugeAccelerationHolo;
import com.kircherelectronics.lowpasslinearacceleration.gauge.GaugeRotationHolo;
import com.kircherelectronics.lowpasslinearacceleration.plot.DynamicLinePlot;
import com.kircherelectronics.lowpasslinearacceleration.plot.PlotColor;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import nebmo.pedometer.AccelerationInfo;
import nebmo.pedometer.Pedometer;

/*
 * Low-Pass Linear Acceleration
 * Copyright (C) 2013, Kaleb Kircher - Boki Software, Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Implements an Activity that is intended to run low-pass filters on
 * accelerometer inputs in an attempt to find the gravity and linear
 * acceleration components of the accelerometer signal. This is accomplished by
 * using a low-pass filter to filter out signals that are shorter than a
 * pre-determined period. The result is only the long term signal, or gravity,
 * which can then be subtracted from the acceleration to find the linear
 * acceleration.
 * 
 * Currently supports two versions of IIR digital low-pass filter. The low-pass
 * filters are classified as recursive, or infinite response filters (IIR). The
 * current, nth sample output depends on both current and previous inputs as
 * well as previous outputs. It is essentially a weighted moving average, which
 * comes in many different flavors depending on the values for the coefficients,
 * a and b.
 * 
 * The first low-pass filter, the Wikipedia LPF, is an IIR single-pole
 * implementation. The coefficient, a (alpha), can be adjusted based on the
 * sample period of the sensor to produce the desired time constant that the
 * filter will act on. It takes a simple form of y[i] = y[i] + alpha * (x[i] -
 * y[i]). Alpha is defined as alpha = dt / (timeConstant + dt);) where the time
 * constant is the length of signals the filter should act on and dt is the
 * sample period (1/frequency) of the sensor.
 * 
 * The second low-pass filter, the Android Developer LPF, is an IIR single-pole
 * implementation. The coefficient, a (alpha), can be adjusted based on the
 * sample period of the sensor to produce the desired time constant that the
 * filter will act on. It is essentially the same as the Wikipedia LPF. It takes
 * a simple form of y[0] = alpha * y[0] + (1 - alpha) * x[0]. Alpha is defined
 * as alpha = timeConstant / (timeConstant + dt) where the time constant is the
 * length of signals the filter should act on and dt is the sample period
 * (1/frequency) of the sensor.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class LinearAccelerationLPFActivity extends Activity implements Runnable, OnTouchListener, OnStepsCountedListener {

	// The static alpha for the LPF Wikipedia
	private static float WIKI_STATIC_ALPHA = 0.1f;
	// The static alpha for the LPF Android Developer
	private static float AND_DEV_STATIC_ALPHA = 0.9f;

	// The size of the sample window that determines RMS Amplitude Noise
	// (standard deviation)
	private final static int SAMPLE_WINDOW = 50;

	// Plot keys for the acceleration plot
	private final static int PLOT_ACCEL_X_AXIS_KEY = 0;
	private final static int PLOT_ACCEL_Y_AXIS_KEY = 1;
	private final static int PLOT_ACCEL_Z_AXIS_KEY = 2;

	// Plot keys for the LPF Wikipedia plot
	private final static int PLOT_LPF_WIKI_X_AXIS_KEY = 3;
	private final static int PLOT_LPF_WIKI_Y_AXIS_KEY = 4;
	private final static int PLOT_LPF_WIKI_Z_AXIS_KEY = 5;
	private final static int PLOT_LPF_WIKI_T_AXIS_KEY = 9;


	// Plot keys for the LPF Android Developer plot
	private final static int PLOT_LPF_AND_DEV_X_AXIS_KEY = 6;
	private final static int PLOT_LPF_AND_DEV_Y_AXIS_KEY = 7;
	private final static int PLOT_LPF_AND_DEV_Z_AXIS_KEY = 8;
	private final static int PLOT_LPF_AND_DEV_T_AXIS_KEY = 10;

	// Indicate if the AndDev LPF should be plotted
	private boolean plotLPFAndDev = false;

	// Indicate if the Wiki LPF should be plotted
	private boolean plotLPFWiki = false;

	// Indicate the plots are ready to accept inputs
	private boolean plotLPFWikiReady = false;
	private boolean plotLPFAndDevReady = false;

	// Indicate if the output should be logged to a .csv file
	private boolean logData = false;


	// Decimal formats for the UI outputs
	private DecimalFormat df;
	private DecimalFormat dfLong;

	// Graph plot for the UI outputs
	private DynamicLinePlot dynamicPlot;

	// Touch to zoom constants for the dynamicPlot
	private float distance = 0;
	private float zoom = 1.2f;

	AccelerationInfo _accelerationInfo = new AccelerationInfo();

	// The Acceleration Gauge
	private GaugeRotationHolo gaugeAccelerationTilt;

	// The LPF Gauge
	private GaugeRotationHolo gaugeLPFWikiTilt;

	// The LPF Gauge
	private GaugeRotationHolo gaugeLPFAndDevTilt;

	private GaugeAccelerationHolo gaugeAcceleration;

	private GaugeAccelerationHolo gaugeLPFWikiAcceleration;

	private GaugeAccelerationHolo gaugeLPFAndDevAcceleration;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;

	// Icon to indicate logging is active
	private ImageView iconLogger;

	// The generation of the log output
	private int generation = 0;

	// Color keys for the acceleration plot
	private int plotAccelXAxisColor;
	private int plotAccelYAxisColor;
	private int plotAccelZAxisColor;

	// Color keys for the LPF Wikipedia plot
	private int plotLPFWikiXAxisColor;
	private int plotLPFWikiYAxisColor;
	private int plotLPFWikiZAxisColor;

	// Color keys for the LPF Android Developer plot
	private int plotLPFAndDevXAxisColor;
	private int plotLPFAndDevYAxisColor;
	private int plotLPFAndDevZAxisColor;

	// Log output time stamp
	private long logTime = 0;

	// Low-Pass Filters
	private LowPassFilter lpfWiki;
	private LowPassFilter lpfAndDev;

	// Plot colors
	private PlotColor color;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	private SettingsDialog settingsDialog;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// LPF Wikipedia plot titles
	private String plotLPFWikiXAxisTitle = "WX";
	private String plotLPFWikiYAxisTitle = "WY";
	private String plotLPFWikiZAxisTitle = "WZ";
	private String plotLPFWikiTAxisTitle = "WT";

	// LPF Android Developer plot tiltes
	private String plotLPFAndDevXAxisTitle = "ADX";
	private String plotLPFAndDevYAxisTitle = "ADY";
	private String plotLPFAndDevZAxisTitle = "ADZ";
	private String plotLPFAndDevTAxisTitle = "ADZ";
	// Output log
	private String log;

	// Acceleration UI outputs
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;
	private TextView vSteps;
	private Pedometer _pedometer;
	private long _steps;
	private int _postCount;
	private StepCounterInteractor mInteractor;
	/**
	 * Get the sample window size for the standard deviation.
	 * 
	 * @return Sample window size for the standard deviation.
	 */
	public static int getSampleWindow()
	{
		return SAMPLE_WINDOW;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.plot_sensor_activity);

		// Read in the saved prefs
		readPrefs();


		initTextViewOutputs();

		initIcons();

		// Initialize the plots
		initColor();
		initPlots();
		initGauges();
		initService();
		handler = new Handler();
	}

	private void initService() {
		final Intent intent = new Intent(this, StepCounterService.class);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			final StepCounterService.StepServiceBinder binder = (StepCounterService.StepServiceBinder) service;
			mInteractor = binder.getStepInteractor();
			mInteractor.registerOnStepsCountedListener(LinearAccelerationLPFActivity.this);
			mInteractor.startListening();
			invalidateOptionsMenu();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mInteractor = null;
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(mServiceConnection);
	}


	@Override
	public void onPause()
	{
		super.onPause();

		if (logData)
		{
			writeLogToFile();
		}
		if(mInteractor != null) {
			mInteractor.unregisterOnStepsCountedListener(this);
		}
		handler.removeCallbacks(this);
	}


	@Override
	public void onResume()
	{
		super.onResume();

		readPrefs();

		handler.post(this);

		if(mInteractor != null) {
			mInteractor.registerOnStepsCountedListener(this);
			if(!mInteractor.isListening())
				mInteractor.startListening();
		}
	}

	/**
	 * Output and logs are run on their own thread to keep the UI from hanging
	 * and the output smooth.
	 */
	@Override
	public void run()
	{

		plotData();
		updateTextViewOutputs();

		handler.postDelayed(this, 100);
	}

	/**
	 * Log output data to an external .csv file.
	 */
	private void logData()
	{
		if (logData)
		{
			if (generation == 0)
			{
				logTime = _accelerationInfo.time;
			}

			log += System.getProperty("line.separator");
			log += generation++ + ",";
			log += _accelerationInfo.time - logTime + ",";

			log += _accelerationInfo.x + ",";
			log += _accelerationInfo.y + ",";
			log += _accelerationInfo.z + ",";

			log += _accelerationInfo.wx + ",";
			log += _accelerationInfo.wy + ",";
			log += _accelerationInfo.wz + ",";

			log += _accelerationInfo.adx + ",";
			log += _accelerationInfo.ady + ",";
			log += _accelerationInfo.adz + ",";
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_logger_menu, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{

		// Log the data
		case R.id.menu_settings_logger_plotdata:
			startDataLog();
			return true;

			// Log the data
		case R.id.menu_settings_filter:
			showSettingsDialog();
			return true;

			// Log the data
		case R.id.menu_settings_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Pinch to zoom.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent e)
	{
		// MotionEvent reports input details from the touch screen
		// and other input controls.
		float newDist = 0;

		switch (e.getAction())
		{

		case MotionEvent.ACTION_MOVE:

			// pinch to zoom
			if (e.getPointerCount() == 2)
			{
				if (distance == 0)
				{
					distance = fingerDist(e);
				}

				newDist = fingerDist(e);

				zoom *= distance / newDist;

				dynamicPlot.setMaxRange(zoom * Math.log(zoom));
				dynamicPlot.setMinRange(-zoom * Math.log(zoom));

				distance = newDist;
			}
		}

		return false;
	}


	/**
	 * Indicate if the Wikipedia LPF should be plotted.
	 * 
	 * @param plotLPFWiki
	 *            Plot the filter if true.
	 */
	public void setPlotLPFWiki(boolean plotLPFWiki)
	{
		this.plotLPFWiki = plotLPFWiki;

		if (this.plotLPFWiki)
		{
			addLPFWikiPlot();
		}
		else
		{
			removeLPFWikiPlot();
		}
	}

	/**
	 * Indicate if the Android Developer LPF should be plotted.
	 * 
	 * @param plotLPFAndDev
	 *            Plot the filter if true.
	 */
	public void setPlotLPFAndDev(boolean plotLPFAndDev)
	{
		this.plotLPFAndDev = plotLPFAndDev;

		if (this.plotLPFAndDev)
		{
			addLPFAndDevPlot();
		}
		else
		{
			removeLPFAndDevPlot();
		}
	}

	/**
	 * Create the output graph line chart.
	 */
	private void addAccelerationPlot()
	{
		addPlot(plotAccelXAxisTitle, PLOT_ACCEL_X_AXIS_KEY, plotAccelXAxisColor);
		addPlot(plotAccelYAxisTitle, PLOT_ACCEL_Y_AXIS_KEY, plotAccelYAxisColor);
		addPlot(plotAccelZAxisTitle, PLOT_ACCEL_Z_AXIS_KEY, plotAccelZAxisColor);
		addPlot(plotLPFWikiTAxisTitle, PLOT_LPF_WIKI_T_AXIS_KEY, plotAccelZAxisColor);
		//addPlot(plotLPFAndDevTAxisTitle, PLOT_LPF_AND_DEV_T_AXIS_KEY, plotAccelXAxisColor);
	}

	/**
	 * Add the Android Developer LPF plot.
	 */
	private void addLPFAndDevPlot()
	{
		if (plotLPFAndDev)
		{
			addPlot(plotLPFAndDevXAxisTitle, PLOT_LPF_AND_DEV_X_AXIS_KEY,
					plotLPFAndDevXAxisColor);
			addPlot(plotLPFAndDevYAxisTitle, PLOT_LPF_AND_DEV_Y_AXIS_KEY,
					plotLPFAndDevYAxisColor);
			addPlot(plotLPFAndDevZAxisTitle, PLOT_LPF_AND_DEV_Z_AXIS_KEY,
					plotLPFAndDevZAxisColor);

			plotLPFAndDevReady = true;
		}
	}

	/**
	 * Add the Wikipedia LPF plot.
	 */
	private void addLPFWikiPlot()
	{
		if (plotLPFWiki)
		{
			addPlot(plotLPFWikiXAxisTitle, PLOT_LPF_WIKI_X_AXIS_KEY,
					plotLPFWikiXAxisColor);
			addPlot(plotLPFWikiYAxisTitle, PLOT_LPF_WIKI_Y_AXIS_KEY,
					plotLPFWikiYAxisColor);
			addPlot(plotLPFWikiZAxisTitle, PLOT_LPF_WIKI_Z_AXIS_KEY,
					plotLPFWikiZAxisColor);

			plotLPFWikiReady = true;
		}
	}

	/**
	 * Add a plot to the graph.
	 * 
	 * @param title
	 *            The name of the plot.
	 * @param key
	 *            The unique plot key
	 * @param color
	 *            The color of the plot
	 */
	private void addPlot(String title, int key, int color)
	{
		dynamicPlot.addSeriesPlot(title, key, color);
	}

	/**
	 * Show the help dialog.
	 */
	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);
		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);

		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		helpDialog.setContentView(getLayoutInflater().inflate(R.layout.help,
				null));

		helpDialog.show();
	}

	/**
	 * Show a settings dialog.
	 */
	private void showSettingsDialog()
	{
		if (settingsDialog == null)
		{
			settingsDialog = new SettingsDialog(this, lpfWiki, lpfAndDev);
			settingsDialog.setCancelable(true);
			settingsDialog.setCanceledOnTouchOutside(true);
		}

		settingsDialog.show();
	}

	/**
	 * Create the plot colors.
	 */
	private void initColor()
	{
		color = new PlotColor(this);

		plotAccelXAxisColor = color.getDarkBlue();
		plotAccelYAxisColor = color.getDarkGreen();
		plotAccelZAxisColor = color.getDarkRed();

		plotLPFWikiXAxisColor = color.getMidBlue();
		plotLPFWikiYAxisColor = color.getMidGreen();
		plotLPFWikiZAxisColor = color.getMidRed();

		plotLPFAndDevXAxisColor = color.getLightBlue();
		plotLPFAndDevYAxisColor = color.getLightGreen();
		plotLPFAndDevZAxisColor = color.getLightRed();
	}



	/**
	 * Create the RMS Noise bar chart.
	 */
	private void initGauges()
	{
		gaugeAccelerationTilt = (GaugeRotationHolo) findViewById(R.id.gauge_acceleration_tilt);
		gaugeLPFWikiTilt = (GaugeRotationHolo) findViewById(R.id.gauge_lpf_wiki_tilt);
		gaugeLPFAndDevTilt = (GaugeRotationHolo) findViewById(R.id.gauge_lpf_and_dev_tilt);

		gaugeAcceleration = (GaugeAccelerationHolo) findViewById(R.id.gauge_acceleration);
		gaugeLPFWikiAcceleration = (GaugeAccelerationHolo) findViewById(R.id.gauge_lpf_wiki);
		gaugeLPFAndDevAcceleration = (GaugeAccelerationHolo) findViewById(R.id.gauge_lpf_and_dev);
	}

	/**
	 * Initialize the icons.
	 */
	private void initIcons()
	{
		// Create the logger icon
		iconLogger = (ImageView) findViewById(R.id.icon_logger);
		iconLogger.setVisibility(View.VISIBLE);

		iconLogger.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startDataLog();
			}
		});
	}

	/**
	 * Initialize the plots.
	 */
	private void initPlots()
	{
		View view = findViewById(R.id.ScrollView01);
		view.setOnTouchListener(this);

		// Create the graph plot
		XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);
		plot.setTitle("Acceleration");
		dynamicPlot = new DynamicLinePlot(plot);
		dynamicPlot.setMaxRange(0.9);
		dynamicPlot.setMinRange(-0.1);

		addAccelerationPlot();
		addLPFWikiPlot();
		addLPFAndDevPlot();
	}

	/**
	 * Initialize the Text View Sensor Outputs.
	 */
	private void initTextViewOutputs()
	{
		// Create the acceleration UI outputs
		xAxis = (TextView) findViewById(R.id.value_x_axis);
		yAxis = (TextView) findViewById(R.id.value_y_axis);
		zAxis = (TextView) findViewById(R.id.value_z_axis);
		vSteps = (TextView) findViewById(R.id.value_steps);
		// Format the UI outputs so they look nice
		df = new DecimalFormat("#.##");
		dfLong = new DecimalFormat("#.####");
	}

	/**
	 * Remove the Android Developer LPF plot.
	 */
	private void removeLPFAndDevPlot()
	{
		if (!plotLPFAndDev)
		{
			plotLPFAndDevReady = false;

			removePlot(PLOT_LPF_AND_DEV_X_AXIS_KEY);
			removePlot(PLOT_LPF_AND_DEV_Y_AXIS_KEY);
			removePlot(PLOT_LPF_AND_DEV_Z_AXIS_KEY);
		}
	}

	/**
	 * Remove the Wikipedia LPF plot.
	 */
	private void removeLPFWikiPlot()
	{
		if (!plotLPFWiki)
		{
			plotLPFWikiReady = false;

			removePlot(PLOT_LPF_WIKI_X_AXIS_KEY);
			removePlot(PLOT_LPF_WIKI_Y_AXIS_KEY);
			removePlot(PLOT_LPF_WIKI_Z_AXIS_KEY);
		}
	}

	/**
	 * Remove a plot from the graph.
	 * 
	 * @param key
	 */
	private void removePlot(int key)
	{
		dynamicPlot.removeSeriesPlot(key);
	}

	/**
	 * Begin logging data to an external .csv file.
	 */
	private void startDataLog()
	{
		if (logData == false)
		{
			CharSequence text = "Logging Data";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();

			String headers = "Generation" + ",";

			headers += "Timestamp" + ",";

			headers += this.plotAccelXAxisTitle + ",";

			headers += this.plotAccelYAxisTitle + ",";

			headers += this.plotAccelZAxisTitle + ",";

			headers += this.plotLPFWikiXAxisTitle + ",";

			headers += this.plotLPFWikiYAxisTitle + ",";

			headers += this.plotLPFWikiZAxisTitle + ",";

			headers += this.plotLPFAndDevXAxisTitle + ",";

			headers += this.plotLPFAndDevYAxisTitle + ",";

			headers += this.plotLPFAndDevZAxisTitle + ",";

			log = headers + "\n";

			iconLogger.setImageResource(R.drawable.launcher_icon);

			logData = true;
		}
		else
		{
			iconLogger.setImageResource(R.drawable.icon_logger);

			logData = false;
			writeLogToFile();
		}
	}

	/**
	 * Plot the output data in the UI.
	 */
	private void plotData()
	{
		//dynamicPlot.setData(lpfWikiOutput[3], PLOT_LPF_WIKI_T_AXIS_KEY);
		//dynamicPlot.setData(lpfAndDevOutput[3], PLOT_LPF_AND_DEV_T_AXIS_KEY);
		dynamicPlot.setData(_accelerationInfo.x, PLOT_ACCEL_X_AXIS_KEY);
//		dynamicPlot.setData(acceleration[1], PLOT_ACCEL_Y_AXIS_KEY);
//		dynamicPlot.setData(acceleration[2], PLOT_ACCEL_Z_AXIS_KEY);
//
		gaugeAccelerationTilt.updateRotation(new float[]{(float) _accelerationInfo.x, (float) _accelerationInfo.y, (float) _accelerationInfo.z});
//
//		gaugeAcceleration.updatePoint(acceleration[0]
//				* SensorManager.GRAVITY_EARTH, acceleration[1]
//				* SensorManager.GRAVITY_EARTH, Color.parseColor("#33b5e5"));

		if (plotLPFWikiReady)
		{
			dynamicPlot.setData(_accelerationInfo.wx, PLOT_LPF_WIKI_X_AXIS_KEY);
			dynamicPlot.setData(_accelerationInfo.wy, PLOT_LPF_WIKI_Y_AXIS_KEY);
			dynamicPlot.setData(_accelerationInfo.wz, PLOT_LPF_WIKI_Z_AXIS_KEY);

			gaugeLPFWikiTilt.updateRotation(new float[]{(float) _accelerationInfo.wx, (float) _accelerationInfo.wy, (float) _accelerationInfo.wz});

			gaugeLPFWikiAcceleration.updatePoint((float)_accelerationInfo.wx
					* SensorManager.GRAVITY_EARTH, (float)_accelerationInfo.wy
					* SensorManager.GRAVITY_EARTH, Color.parseColor("#33b5e5"));
		}

		if (plotLPFAndDevReady)
		{
			dynamicPlot
					.setData(_accelerationInfo.adx, PLOT_LPF_AND_DEV_X_AXIS_KEY);
			dynamicPlot
					.setData(_accelerationInfo.ady, PLOT_LPF_AND_DEV_Y_AXIS_KEY);
			dynamicPlot
					.setData(_accelerationInfo.adz, PLOT_LPF_AND_DEV_Z_AXIS_KEY);

			gaugeLPFAndDevTilt.updateRotation(new float[]{(float) _accelerationInfo.adx, (float) _accelerationInfo.ady, (float) _accelerationInfo.adz});

			gaugeLPFAndDevAcceleration.updatePoint((float)_accelerationInfo.adx
					* SensorManager.GRAVITY_EARTH, (float)_accelerationInfo.ady
					* SensorManager.GRAVITY_EARTH, Color.parseColor("#33b5e5"));
		}

		dynamicPlot.draw();

	}

	private void updateTextViewOutputs()
	{
		// Update the view with the new acceleration data
		xAxis.setText(df.format(_accelerationInfo.x));
		yAxis.setText(df.format(_accelerationInfo.y));
		zAxis.setText(df.format(_accelerationInfo.wz));
		vSteps.setText(_steps+"");
	}


	/**
	 * Write the logged data out to a persisted file.
	 */
	private void writeLogToFile()
	{
		Calendar c = Calendar.getInstance();
		String filename = "AccelerationFilter-" + c.get(Calendar.YEAR) + "-"
				+ c.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "-"
				+ c.get(Calendar.HOUR) + "-" + c.get(Calendar.HOUR) + "-"
				+ c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND)
				+ ".csv";

		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "AccelerationFilter" + File.separator
				+ "Logs" + File.separator + "Acceleration");
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File file = new File(dir, filename);

		FileOutputStream fos;
		byte[] data = log.getBytes();
		try
		{
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();

			CharSequence text = "Log Saved";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (FileNotFoundException e)
		{
			CharSequence text = e.toString();
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (IOException e)
		{
			// handle exception
		}
		finally
		{
			// Update the MediaStore so we can view the file without rebooting.
			// Note that it appears that the ACTION_MEDIA_MOUNTED approach is
			// now blocked for non-system apps on Android 4.4.
			MediaScannerConnection.scanFile(this, new String[]
			{ "file://" + Environment.getExternalStorageDirectory() }, null,
					new MediaScannerConnection.OnScanCompletedListener()
					{
						@Override
						public void onScanCompleted(final String path,
								final Uri uri)
						{
				
						}
					});
		}
	}

	/**
	 * Get the distance between fingers for the touch to zoom.
	 * 
	 * @param event
	 * @return
	 */
	private final float fingerDist(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * Read in the current user preferences.
	 */
	private void readPrefs()
	{
		SharedPreferences prefs = this.getSharedPreferences("lpf_prefs",
				Activity.MODE_PRIVATE);

		this.plotLPFAndDev = prefs.getBoolean("plot_lpf_and_dev", false);
		this.plotLPFWiki = prefs.getBoolean("plot_lpf_wiki", false);
	}

	@Override
	public void onStepsCounted(StepCounterInteractor listener) {
		_steps = listener.getCountedSteps();
	}

	@Override
	public void onSensorChanged(StepCounterInteractor listener) {
		 _accelerationInfo = listener.getAccelerationInfo();
		logData();
		//_steps = listener.getCountedSteps();
	}

	/**
	 * A simple formatter to convert bar indexes into sensor names.
	 */
	private class NoiseIndexFormat extends Format
	{

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo,
				FieldPosition pos)
		{
			Number num = (Number) obj;

			// using num.intValue() will floor the value, so we add 0.5 to round
			// instead:
			int roundNum = (int) (num.floatValue() + 0.5f);
			switch (roundNum)
			{
			case 0:
				toAppendTo.append("Accel");
				break;
			case 1:
				toAppendTo.append("LPFWiki");
				break;
			case 2:
				toAppendTo.append("LPFAndDev");
				break;
			default:
				toAppendTo.append("Unknown");
			}
			return toAppendTo;
		}

		@Override
		public Object parseObject(String string, ParsePosition position)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}
}

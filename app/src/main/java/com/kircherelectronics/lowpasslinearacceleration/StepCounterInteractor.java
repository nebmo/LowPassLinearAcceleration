package com.kircherelectronics.lowpasslinearacceleration;

import nebmo.pedometer.AccelerationInfo;

/**
 * Created by niklas.weidemann on 2014-06-17.
 */
public interface StepCounterInteractor {
	void startListening();
	void stopListener();
	void registerOnStepsCountedListener(OnStepsCountedListener listener);
	void unregisterOnStepsCountedListener(OnStepsCountedListener listener);
	long getCountedSteps();
	AccelerationInfo getAccelerationInfo();
	boolean isListening();
}

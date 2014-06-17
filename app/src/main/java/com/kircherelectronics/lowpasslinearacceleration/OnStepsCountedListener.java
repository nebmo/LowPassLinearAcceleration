package com.kircherelectronics.lowpasslinearacceleration;

/**
 * Created by niklas.weidemann on 2014-06-17.
 */
public interface OnStepsCountedListener {
	void onStepsCounted(StepCounterInteractor listener);

	void onSensorChanged(StepCounterInteractor listener);
}

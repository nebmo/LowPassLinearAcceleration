package nebmo.pedometer;

/**
 * Created by niklas.weidemann on 2014-06-15.
 */
public interface IPedometer extends IStepsCounter {
	void onInput(AccelerationInfo info);

	int getSteps();
}

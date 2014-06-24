package nebmo.pedometer;

/**
 * Created by niklas.weidemann on 2014-06-23.
 */
public interface IStepsCounter {
	double getAvgTimeBetweenSteps();
	int getCadense();
	int getAvgCadense();
}

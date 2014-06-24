package nebmo.pedometer;

/**
 * Created by niklas.weidemann on 2014-06-23.
 */
public class StepsCounterBase implements IStepsCounter {
	protected long[] lastSteps = new long[5];
	protected long startTime;
	protected int lastStepIndex = -1;
	protected int _steps;

	@Override
	public double getAvgTimeBetweenSteps() {
		if(lastStepIndex == 4){
			return (lastSteps[0]-lastSteps[4]) / 4.0;
		}
		return 0;
	}

	@Override
	public int getCadense() {
		double avgTime = getAvgTimeBetweenSteps();
		if(avgTime == 0.0)
			return 0;
		return (int)Math.round((60 * 1000) / getAvgTimeBetweenSteps());
	}

	@Override
	public int getAvgCadense() {
		if(_steps < 5)
			return 0;
		return (int)Math.round((60 * 1000) / ((lastSteps[0] - startTime) / (_steps -1)));
	}

	public void addStep(long timestamp){
		if(_steps == 0)
			startTime = timestamp;
		System.arraycopy(lastSteps,0,lastSteps,1,lastSteps.length -1);
		lastSteps[0] = timestamp;
		lastStepIndex = Math.min(lastStepIndex+1,4);
		_steps++;
	}
}

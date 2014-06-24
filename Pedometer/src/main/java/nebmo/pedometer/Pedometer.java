package nebmo.pedometer;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

//http://www.analog.com/static/imported-files/tech_articles/pedometer.pdf
public class Pedometer extends StepsCounterBase implements IPedometer {
	private List<AccelerationInfo> _arrInfo;

	private double[] new_value = new double[4];
	private double[] old_value = new double[4];
	private Queue queue = new ArrayDeque();
	private int count;
	private double min_value;
	private double max_value;
	private double threshold;
	private double precision = 0.1d;
	private long lastStep;

	@Override
	public void onInput(AccelerationInfo info){
		double value = info.wx;
		if(new_value[0] > max_value)
			max_value = new_value[0];
		if(new_value[0] < min_value)
			min_value = new_value[0];

		count++;
		if(count == 25) {
			threshold = (min_value + max_value) / 2;
			min_value = 0;
			max_value = 0;
			count = 0;
		}
		old_value[0] = new_value[0];
		double diff = Math.abs(value - new_value[0]);
		if(diff > precision){
			new_value[0] = value;
		}
		if(old_value[0] > threshold && threshold > new_value[0]){
			long timediff = info.time - lastStep;
			if(timediff > 200){
				lastStep = info.time;
				addStep(info.time);
			}
		}

	}

	@Override
	public int getSteps(){
		return _steps;
	}

	@Override
	public int getCadense() {
		return super.getCadense();
	}
}


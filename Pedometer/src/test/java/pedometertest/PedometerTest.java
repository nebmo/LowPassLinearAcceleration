package pedometertest;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nebmo.pedometer.AccelerationInfo;
import nebmo.pedometer.Pedometer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by niklas.weidemann on 2014-06-14.
 */
public class PedometerTest {
	@Test
	public void shouldParseFile() throws IOException {
		List<AccelerationInfo> arrAccelerationInfo = readFile("AccelerationFilter-2014-2-8-8-47-27.csv");
		assertEquals(306,arrAccelerationInfo.size());
	}

	private List<AccelerationInfo> readFile(String fileName) throws IOException {
		String str="";
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);

		List<AccelerationInfo> arrAccelerationInfo = new ArrayList<AccelerationInfo>();

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			if (is!=null) {
				while ((str = reader.readLine()) != null) {
					AccelerationInfo info = AccelerationInfo.tryParse(str);
					if(info != null)
						arrAccelerationInfo.add(info);
				}
			}
		} finally {
			try { is.close(); } catch (Throwable ignore) {}
		}
		return arrAccelerationInfo;
	}

	@Test
	public void shouldCreateAccelerationInfoFromString(){
		String info = "0,115,-5.493164E-4,0.3998413,0.60961914,0.047699027,-0.03893718,-0.1586011,0.04769904,-0.03893721,-0.1586011,";
		long expectedTime = 115;
		Double expectedwX = 0.047699027;
		Double expectedwY= -0.03893718;
		Double expectedwZ = -0.1586011;
		AccelerationInfo a = AccelerationInfo.tryParse(info);
		assertThat(a.time, is(equalTo(expectedTime)));
		assertThat(a.wx, is(equalTo(expectedwX)));
		assertThat(a.wy, is(equalTo(expectedwY)));
		assertThat(a.wz, is(equalTo(expectedwZ)));
	}

	/*@Test
	 public void runFile1() throws IOException {
		Pedometer pedometer = new Pedometer();
		List<AccelerationInfo> arrAccelerationInfo = readFile("AccelerationFilter-50.csv");
		for(AccelerationInfo info : arrAccelerationInfo){
			pedometer.onInput(info);
		}
		assertThat(pedometer.getSteps(), is(equalTo(3)));
	}

	@Test
	public void runFile2() throws IOException {
		Pedometer pedometer = new Pedometer();
		List<AccelerationInfo> arrAccelerationInfo = readFile("AccelerationFilter_60.csv");
		for(AccelerationInfo info : arrAccelerationInfo){
			pedometer.onInput(info);
		}
		assertThat(pedometer.getSteps(), is(equalTo(3)));
	}*/

	@Test
	public void runFile2() throws IOException {
		Pedometer pedometer = new Pedometer();
		List<AccelerationInfo> arrAccelerationInfo = readFile("AccelerationFilter-60-40ms.csv");
		for(AccelerationInfo info : arrAccelerationInfo){
			pedometer.onInput(info);
		}
		assertThat(pedometer.getSteps(), is(equalTo(3)));
	}

}
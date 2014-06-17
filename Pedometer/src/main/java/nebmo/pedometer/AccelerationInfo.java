package nebmo.pedometer;

public class AccelerationInfo
{
	public long time;
	public double x;
	public double y;
	public double z;
	public double v;
	public double wx;
	public double wy;
	public double wz;
	public double wv;
	public double adx;
	public double ady;
	public double adz;
	public double adv;
	public double gx;
	public double gy;
	public double gz;
	public static AccelerationInfo tryParse(String info){
		String[] result = info.split(",");
		AccelerationInfo aInfo = new AccelerationInfo();
		try{
			aInfo.time = Long.parseLong(result[1]);
			aInfo.x = Double.parseDouble(result[2]);
			aInfo.y = Double.parseDouble(result[3]);
			aInfo.z = Double.parseDouble(result[4]);
			aInfo.v = Math.sqrt(Math.pow(aInfo.x,2)+Math.pow(aInfo.y,2)+Math.pow(aInfo.z,2));
			aInfo.wx = Double.parseDouble(result[5]);
			aInfo.wy = Double.parseDouble(result[6]);
			aInfo.wz = Double.parseDouble(result[7]);
			aInfo.wv = Math.sqrt(Math.pow(aInfo.wx,2)+Math.pow(aInfo.wy,2)+Math.pow(aInfo.wz,2));
			aInfo.adx = Double.parseDouble(result[8]);
			aInfo.ady = Double.parseDouble(result[9]);
			aInfo.adz = Double.parseDouble(result[10]);
			aInfo.adv = Math.sqrt(Math.pow(aInfo.adx,2)+Math.pow(aInfo.ady,2)+Math.pow(aInfo.adz,2));
		}
		catch (Exception ex){
			return null;
		}
		return aInfo;
	}
}

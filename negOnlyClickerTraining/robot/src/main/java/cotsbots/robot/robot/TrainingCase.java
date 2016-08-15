package cotsbots.robot.robot;

public class TrainingCase {

	public int generation = 0;
	public double[] output;
	public double[] input;
	public boolean isNeg;
	
	public TrainingCase( int g, double[] o, double[] i, boolean n){
		generation = g;
		output = o;
		input = i;
		isNeg = n;
	}
	
}

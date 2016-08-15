package cotsbots.robot.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EvolutionData implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	
	public int numTestCases;
	public int numGenerations;
	public List<Double> averageFitByGeneration;
	public List<Double> bestFitByGeneration;
	public int currentGeneration, forward, left, right;
	public double currentBest;
	public double currentAverage;
	public double[] elite;
	public double[][] elitePopulation;
	public boolean hasNewElite = false;
	public int numRecElites = 0;
	
	
	public EvolutionData(int gen){
		numGenerations = gen;
		averageFitByGeneration = new ArrayList<Double>();
		bestFitByGeneration = new ArrayList<Double>();
	}
	
	@SuppressWarnings("rawtypes")
	public void update(List<Double> average, List<Double> best, int g, double b, double a, int t, int f, int l, int r, int e){
		averageFitByGeneration = average;
		bestFitByGeneration = best;
		numTestCases = t;
		currentGeneration = g;
		currentBest = b;
		currentAverage = a;
		forward = f;
		left = l;
		right = r;
		numRecElites = e;
		
		
	}
}

package cotsbots.robot.data;

import java.io.Serializable;

public class EvolutionConditions implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1397473176223144758L;
	
	public int maxGenerations;
	public boolean distributed = false;
	public boolean offline = false;
	public int numberOfDistBots;
	public boolean sharedTestcases = false;
	public int startCondition; //0 = start with 1TC, 1 = start with 1 of each TC, 2 = start with 3 of each TC, 3 = start with 10 of each TC
	
	
	public EvolutionConditions( int m, boolean d, boolean t, int s, int n ){
		maxGenerations = m;
		distributed = d;
		sharedTestcases = t;
		startCondition = s;
		numberOfDistBots = n;
	}
	public EvolutionConditions(  ){
		
	}

}

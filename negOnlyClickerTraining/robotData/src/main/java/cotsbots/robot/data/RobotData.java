package cotsbots.robot.data;

import java.io.Serializable;

public class RobotData implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public enum state {
		CLEAR, VISIONTRAINING, ANNTRAINING, AUTONOMOUS, TELEOPERATED 
	}
	
	
	public state currentState;
	public boolean evoReady = false;
	public EvolutionConditions Conditions;
	public EvolutionData evoData;
	public int ID;
	
	
	public RobotData(){
		currentState = state.CLEAR;
		Conditions = new EvolutionConditions();
		evoData = new EvolutionData(0);
	}
	
}

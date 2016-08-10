package cotsbots.com.message;

import java.io.Serializable;

import cotsbots.robot.data.RobotData;

public class Message implements Serializable{
	protected static final long serialVersionUID = 1112122200L;
	
	public final int newConnectionRobot 	= 0,
					 moveCommand			= 1,
					 setEvolutionConditions	= 2,
					 changeState			= 3,
					 robotUpdate			= 4,
					 updateRequest			= 5,
					 pause					= 6,
					 evolve					= 7,
					 updateElite			= 8,
					 sendElites				= 9;
	
	
	
	public int msgType;
	public int ID;
	public RobotData data;
	public int direction = 0;
	
	public Message(){
		data = new RobotData();
	}
	
}

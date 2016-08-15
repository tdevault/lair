package cotsbots.robot.client;


import android.app.Activity;
import android.content.Context;
import android.util.Log;


import cotsbots.robot.data.EvolutionConditions;
import cotsbots.robot.data.RobotData;
import cotsbots.robot.data.RobotData.state;
import cotsbots.robot.robot.Robot;

import java.net.*;
import java.io.*;

import cotsbots.com.message.Message;
import cotsbots.graduate.robotcontroller.RobotController.SteeringType;



public class robotClient {
	
	
	
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private Socket socket;
	private String server;
	public boolean connected = false;
	public int ID = -1;
	private int port;
	public Robot myRobot;

	
	public remoteReciever remote;
	
	
	
	public robotClient(   SteeringType drive,Robot r){
		remote = new remoteReciever();
		myRobot = r;

	}
	
	
	public void startRemote(){
		remote.start();
		while(!remote.connected){
			//Log.e("RV", "Remote in loop " +i);
			//i++;
			;
		}
		new RemoteListener().start();

	}
	
	class RemoteListener extends Thread{
		public void run(){
			while(true){
				
				try{
					if (remote.ready()){
						char m = remote.read();
						Log.e("Robo Client", "Message Recieved");
						switch(m){
						case '0'://pause
							Log.e("Robo Client", "pause Recieved");
							myRobot.pause();
							break;
						case '1': // forward
							Log.e("Robo Client", "forward Recieved");
							myRobot.drive(1);
							break;
						
						case '3':// left
							Log.e("Robo Client", "left Recieved");
							myRobot.drive(3);
							break;
						case '4': // right
							Log.e("Robo Client", "right Recieved");
							myRobot.drive(4);
							break;
						
						case '6': // Teleoperate
							myRobot.startEvo();
							break;
						case '7': // Train ANN
							Log.e("Robo Client", "change state training Recieved");
							myRobot.changeState(state.ANNTRAINING);
							break;
					
						case '8': //Autonomous
							Log.e("Robo Client", "change state autonomous Recieved");
							myRobot.changeState(state.AUTONOMOUS);
							break;
						case 'd':
							Log.e("Robot Client", "done sent");
							myRobot.evolver.done = true;
						case 's': //save training
							Log.e("Robo Client", "save Training Received");
							myRobot.saveTrainingSet();
							break;
						case 'a': //save ann
							Log.e("Robo Client", "save ANN Received");
							myRobot.saveANN();
							break;
						case 'l': //load training
							Log.e("Robo Client", "Load  Training Received");
							myRobot.loadTrainingSet();
							break;
						case 'b': //load ann
							Log.e("Robo Client", "Load ANN Received");
							myRobot.loadANN();
							break;
						case 't': //reset
							Log.e("Robo Client", "Evolve10 Received");
							myRobot.evolveForTen();
							break;
						case 'p':
							Log.e("robo Client", "positive received");
							myRobot.positiveClick();
							break;
						case 'n':
							Log.e("robo Client", "negative received");
							myRobot.negativeClick();
							break;

							}
						}
					}catch(Exception e){
						
					}
				}
			 
		}
	}
	
	
	

	
	
}


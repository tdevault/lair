package cotsbots.robot.robot;

import java.text.DecimalFormat;
import java.util.Locale;

import android.util.Log;
import core.NeuralNetwork;
import cotsbots.robot.data.EvolutionConditions;
import cotsbots.robot.data.RobotData;
import nnet.BackPropagation;
import nnet.MultiLayerPerceptron;
import util.TransferFunctionType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class EvolveThread extends Thread implements TextToSpeech.OnInitListener{

	public GANetTrainer trainer;
	public boolean done = false;
	public double fitnessThresh = .2;
	public EvolutionConditions conditions;
	public int maxGen = 100;
	public int startCondition;
	public RobotData myData;
	public NeuralNetwork best;
	public TextToSpeech tts;
	public Activity thisAct;
	public boolean offline = false;
	public boolean distributed = false;
	public int currentGeneration = 0;
	public double[] bestArray;
	public int genCounter = 0;
	public boolean hasNewBest = false;
	public Robot myRobot;
	public NeuralNetwork backPropNet;
	public BackPropagation backProp;
	public boolean doBP = false;
	public int currentMaxBP;
	public boolean bpReady = true;
	DecimalFormat df = new DecimalFormat("#.###");
	
	public EvolveThread(GANetTrainer t, RobotData r, Activity c, Robot robot){
		trainer = t;
		myData = r;
		thisAct = c;
		conditions = myData.Conditions;
		myRobot = robot;
		backPropNet = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, robot.inputs, robot.hidden, robot.outputs );
		backProp = new BackPropagation();
		backProp.setMaxIterations(robot.maxBackPropIterations);
		currentMaxBP = robot.maxBackPropIterations;

		setup();
		setupttt();

	}
	
	public void setupttt(){
		tts = new TextToSpeech(thisAct, this);
		
	}
	
	public void setup(){
		maxGen = conditions.maxGenerations;
		if (conditions.distributed){
			trainer.setUpDistrobuted(conditions.numberOfDistBots);
			distributed = true;
		}
		startCondition = conditions.startCondition;
	}
	
	public void updateElites( double [] [] elites ){
		trainer.updateElites( elites );
	}
	
	public NeuralNetwork getBest(){
		return best;
	}
	
	public void run(){
		
		
		boolean started = false;
		
		Log.e("EvolveThread", "Max Gen: " + maxGen);
		// start with a random ann
		trainer.initializeArray();
		currentGeneration++;
		best = trainer.getBest();
		hasNewBest = true;
		myRobot.ann = best;
		myRobot.annCreated = true;
		Log.e("EvolveThread", "initialized");
		while( !done ){
			//Log.e("EvolveThread", "1");
			if( maxGen == 0 ){
				// do evolution
				//if(!myRobot.annCreated){
				//Log.e("EvolveThread", "2");}
				if( myRobot.numCase >= 4 && !started){
					started = true;
					tts.speak("Evolution Started", TextToSpeech.QUEUE_FLUSH, null);
				}
				

				
				if( started ){
					if ( genCounter >0 ){
						trainer.runGenerationArray();
						currentGeneration++;
						if(currentGeneration %25 == 0){
							Log.e("EvolveThread", "gen:" + currentGeneration);
							double fit = new Double(df.format( trainer.getBestFitness() ) );
							tts.speak("" + currentGeneration + " generations done. best fit " + fit, TextToSpeech.QUEUE_FLUSH, null);

						}
						genCounter--;
						best = trainer.getBest();
						hasNewBest = true;
						myRobot.ann = best;
						myRobot.annCreated = true;
					
					}
				}
			}else{
				// do back propagation
				if( doBP ){
					bpReady = false;
					if(currentMaxBP % 1000 == 0){
						//tts.speak(""+currentMaxBP + " iterations done ", TextToSpeech.QUEUE_FLUSH, null, null);
					}
					backPropNet.learn(myRobot.trainingSet, backProp);
					backProp.setMaxIterations(currentMaxBP + myRobot.maxBackPropIterations);
					currentMaxBP+=myRobot.maxBackPropIterations;
					doBP = false;
					bpReady = true;
					myRobot.ann = backPropNet;
					myRobot.annCreated = true;
				}
			}
		}
		
		
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		//if (status == TextToSpeech.SUCCESS) {
			 
            //int result = tts.setLanguage(Locale.US);
		//}
	}
}

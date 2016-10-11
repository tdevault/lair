package cotsbots.robot.robot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import core.DataSet;
import nnet.MultiLayerPerceptron;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;


import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import util.TransferFunctionType;

import core.Connection;
import core.Layer;
import core.NeuralNetwork;
import core.Neuron;

import cotsbots.graduate.robotcontroller.RobotController;
import cotsbots.graduate.robotcontroller.RobotController.SteeringType;
import cotsbots.robot.data.RobotData;
import cotsbots.robot.data.RobotData.state;






public class Robot{
	
	
	
	public SteeringType driveType;
	public RobotController robotControl;
	public RobotData myData;
	
	public state currentState = state.CLEAR;
	
	public int col = 8, rows = 5;
	
	public NeuralNetwork ann;
	public int inputs = col*rows*3, hidden = 5, outputs = 3, popSize = 40;
	public GANetTrainer ga;
	public EvolveThread evolver;
	public boolean evolving = false;
	public boolean offline = false;
	
	public boolean running = true;
	public boolean paused = false;
	public boolean annCreated = false;
	public boolean hasF = false, hasL = false, hasR = false, hasNF = false, hasNL = false, hasNR = false;
	public int numCase = 0;
	
	public int generationsBetweenTrainings = 5;
	public int maxBackPropIterations = 100;

	public DataSet trainingSet = new DataSet(col*rows*3, outputs);

    public Size smallSize = new Size(800,600);
	
	public int imageFileCounter = 0;
	public long lastTime = System.currentTimeMillis();
	
	
	//public RoadClassifier imageProcessor;

	public int startx = 315, endx = 365, starty = 400, endy = 450;
	
	//double[] output = imageProcessor.calc(m_Rgba,col,rows);
	//imageProcessor.updateModel(m_Rgba);
	public double[] currentMove = new double[3];
	public Activity myActivity;
	private File storage_path;
	private String storage_file = "";
	public String evoDir = "RobotEvolvApp";
	public String driveDir = "RobotDriveApp";
	public long driveTime = System.currentTimeMillis();
	public double[] prob = new double[col*rows*3];
    public Random rng = new Random();
	//public blueRoadClassifier imgProcess = new blueRoadClassifier();
	public boolean drove = false;
	public boolean processLock = false;
	
	public Robot(String ip, SteeringType drive, RobotData data, boolean robotOn, Activity c){
		//imageProcessor = ImageProcessor.getRoadClassifier(modelType, startx, starty, endx, endy);
		
		
		ga = new GANetTrainer(popSize, inputs, hidden, outputs);
		driveType = drive;
		myData = data;
		myActivity = c;
		Log.e("Robot Class", "Robot Started");
		if(robotOn){
			startRobot(ip);
		}
		//startRobotView(c);



	}

	public void startEvo(){
		evolver = new EvolveThread(ga, myData, myActivity, this);
		evolver.start();
	}
	
	//public CameraBridgeViewBase getRobotView(){
	//	return myRobotView.mOpenCvCameraView;
	//}
	

	public void changeState( state newState ){
		
		if( newState == state.ANNTRAINING ){
            currentState = newState;

		}else if( newState == state.AUTONOMOUS ){
			
			currentState = newState;
			ann = evolver.getBest();
			annCreated = true;
		}
		
	}
	public void pause(){
        if( paused ){
            paused = false;
        }else{
            paused = true;
        }

	}


	public void evolveForTen(){
		evolver.genCounter = 10;
	}
	


	public void negativeClick(){
		if ( currentState == state.ANNTRAINING ){
			if(currentMove[0] ==1 ){
				if( !hasNL ) {
					hasNL = true;
					numCase++;
				}
			}else if( currentMove[1] ==1 ){
				if( !hasNF ) {
					hasNF = true;
					numCase++;
				};
			}else if( currentMove[2] == 1 ){
				if( !hasNR ) {
					hasNR = true;
					numCase++;
				}
			}


			ga.addTrainingCase( ga.generation, prob, currentMove, true);
			evolver.genCounter = generationsBetweenTrainings;
			trainingSet.addRow(prob, currentMove);
			if( evolver.bpReady){
				evolver.doBP = true;
			}
		}
	}

	public void drive( int direction ){
	
		

		
		switch( direction ){
		case 0: // stop
			robotControl.driveVehicle(.5, .5, driveType);
			break;
		case 1: // forward
			robotControl.driveVehicle(.5, .75, driveType);
		
			break;
		case 2: // reverse
			robotControl.driveVehicle(.5, .25, driveType);
			break;
		case 3: // left
			robotControl.driveVehicle(0, .75, driveType);
			break;
		case 4: // right
			robotControl.driveVehicle(1, .75, driveType);
			break;
		}
		
		
	}
	public void setMove(int direction){
		currentMove = new double[3];


		if( currentState == state.ANNTRAINING ){

			switch( direction ){
				case 1: // forward
					currentMove[0] = 0;
					currentMove[1] = 1;
					currentMove[2] = 0;
					//hasF = true;
					break;
				case 3: // left
					currentMove[0] = 1;
					currentMove[1] = 0;
					currentMove[2] = 0;
					//hasL = true;
					break;
				case 4: // right
					currentMove[0] = 0;
					currentMove[1] = 0;
					currentMove[2] = 1;
					//hasR = true;
					break;
			}


		}
	}
	
	public Mat frameReceived( Mat frame ){
        /*
        if( needImage == true ){
            imageToSave = Bitmap.createBitmap(m_Rgba.width(), m_Rgba.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m_Rgba, imageToSave);
            //needImage = false;
            hasImage = true;
            bot.saveImage();
        }*/

		processLock = true;
		Mat smallImage = new Mat();
		Imgproc.resize(frame, smallImage, smallSize);


		//Log.e("frame received", "1");
        if( !paused && annCreated ){

            if( currentState == RobotData.state.AUTONOMOUS ){
                //Log.e("AUTO DRIVE", "AUTONOMOUS");




                //RESIZE IMAGE

                //Mat transImage = new Mat();
                //transImage = brc.transformImage(smallImage);
                //change
                prob = calcProbabilities(smallImage, col, rows);
                //prob = brc.calcProbabilities(transImage, col, rows);
                //Imgproc.resize(transImage, m_Rgba, m_Rgba.size());


                ann.setInput(prob);
                ann.calculate();
                double output[] = ann.getOutput();
				/*
                double total = 0;
                for( int i = 0; i < output.length; i++ ){
                    total += output[i];
                }
                double pos = rng.nextDouble()*total;
                if( pos <= output[0] ){
                    drive(3); //left
                }else if( pos <= (output[0]+output[1]) ){
                    drive(1); //forward
                }else if( pos <= (output[0]+output[1]+output[2]) ){
                    drive(4); //right
                }else{
                    Log.e("AUTO DRIVE", "something fucked up");
                }*/
				if( output[0] > output[1] && output[0] >output[2]){
					drive(3);
				}else if( output[1] > output[0] && output[1] >output[2]){
					drive(1);
				}else if( output[2] > output[1] && output[2] >output[0]){
					drive(4);
				}else{
					Log.e("AUTO DRIVE", "something fucked up");
				}
            }else if( currentState == RobotData.state.ANNTRAINING ){
                //Log.e("AUTO DRIVE", "ANNTRAINING");
				if(drove){
					if( System.currentTimeMillis()-driveTime >250){
						drive(0);
						drove = false;
					}
				}

                if(System.currentTimeMillis() - lastTime > 2000 ){


                /*
                    if( needImage == true ){
                        imageToSave = Bitmap.createBitmap(m_Rgba.width(), m_Rgba.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(m_Rgba, imageToSave);
                        //needImage = false;
                        hasImage = true;
                        bot.saveImage();
                    }
                */
                    //RESIZE IMAGE
                    //Mat smallImage = new Mat();
                    //Imgproc.resize(frame, smallImage, smallSize);

                    //Mat transImage = new Mat();
                    //transImage = brc.transformImage(smallImage);
                    //change
                    //prob = calcProbabilities(smallImage, col, rows);
                    prob = calcProbabilities(smallImage, col, rows);
                    //Imgproc.resize(transImage, m_Rgba, m_Rgba.size());

					if( evolver.started ) {
						ann.setInput(prob);
						ann.calculate();
						double output[] = ann.getOutput();
						double total = 0;

						for (int i = 0; i < output.length; i++) {
							total += output[i];
						}
						double pos = rng.nextDouble() * total;
						if (pos <= output[0]) {
							drive(3); //left
							setMove(3);
							drove = true;
							driveTime = System.currentTimeMillis();

						} else if (pos <= (output[0] + output[1])) {
							drive(1); //forward
							setMove(1);
							drove = true;
							driveTime = System.currentTimeMillis();
						} else if (pos <= (output[0] + output[1] + output[2])) {
							drive(4); //right
							setMove(4);
							drove = true;
							driveTime = System.currentTimeMillis();
						} else {
							Log.e("AUTO DRIVE", "something fucked up");
						}



					}else{
						double p = rng.nextDouble();
						if( p <=.33 ){
							drive(3);
							setMove(3);
							drove = true;
							driveTime = System.currentTimeMillis();
						}else if( p <=.66 ){
							drive(1);
							setMove(1);
							drove = true;
							driveTime = System.currentTimeMillis();
						}else {
							drive(4);
							setMove(4);
							drove = true;
							driveTime = System.currentTimeMillis();
						}
					}
					lastTime = System.currentTimeMillis();
                }
            }else{
                Log.e("AUTO DRIVE", "bad state");
            }




        }
		smallImage.release();
        if(!annCreated){
        	drawbox(frame);
		}
		processLock = false;
        return frame;

	}


    public void drawbox(Mat frame){
        int startx = 315, endx = 365, starty = 400, endy = 450;
        double[] color = {255,0,0};
        double[] color2 = {255,0,0,0};
        for ( int x = startx; x < endx; x++){
            for( int y = starty; y < endy; y++ ){
                if( x == startx || y == starty || x == endx-1|| y == endy-1){

                    if (frame.channels() == 3){
                        frame.put(y, x, color);
                    }
                    else{
                        frame.put(y, x, color2);
                    }
                }
            }
        }

    }

    public double[] calcProbabilities( Mat image, int numCols, int numRows ){

        int numInput = numCols*numRows;
        double[] outputProbabilities = new double[numInput*3];

        int intAggregateWidth = ( image.cols() ) / numCols;
        int intAggregateHeight = ( image.rows() ) / numRows;
        int pos = 0;

		DecimalFormat df = new DecimalFormat("#.##");
        for ( int x = 0; x < (image.cols()); x += intAggregateWidth ){
            for ( int y = 0; y < (image.rows()); y += intAggregateHeight ){
                Scalar avg = Core.mean(image.submat(y, y + intAggregateHeight, x, x + intAggregateWidth));
                //outputProbabilities[pos] = Math.round(avg.val[0] / (avg.val[1]+avg.val[2]+1) );
                //outputProbabilities[pos+1] = Math.round(avg.val[1] / (avg.val[0]+avg.val[2]+1));
                //outputProbabilities[pos+2] = Math.round(avg.val[2] / (avg.val[0]+avg.val[1]+1));

				outputProbabilities[pos] = new Double(df.format(avg.val[0] / (avg.val[1] + avg.val[2] + 1) ) );
				outputProbabilities[pos+1] = new Double(df.format(avg.val[1] / (avg.val[0]+avg.val[2]+1) ) );
				outputProbabilities[pos+2] = new Double(df.format(avg.val[2] / (avg.val[0]+avg.val[1]+1) ) );

				//outputProbabilities[pos] = new Double(df.format(avg.val[0]/255));
				//outputProbabilities[pos+1] = new Double(df.format(avg.val[1]/255));
				//outputProbabilities[pos+2] = new Double(df.format(avg.val[2]/255));


                pos+=3;
            }
        }

        return outputProbabilities;

    }

	
	
	public void saveImage() {
		/*while( myRobotView.hasImage == false){
			;
		}
		Bitmap image = myRobotView.imageToSave;
		myRobotView.hasImage = false;
		
		imageFileCounter++;
		storage_file = "Image" + imageFileCounter + ".jpeg";
		ensure(evoDir);
		File file = new File(storage_path, storage_file);
		FileOutputStream out = null;
		try{
			out = new FileOutputStream(file);
			image.compress(Bitmap.CompressFormat.JPEG, 90, out);
		}catch(Exception e){
			Log.e("Robot", "Failed to save image");
		}
		*/
	}

	
	public void startRobot(String mac){
		robotControl = new RobotController(mac);
		
		while(!robotControl.isConnected()){
			;
		}
		Log.e("Robot Class", "Robot Controller Connected");
	}

	
	public void reset(){
		//TODO FIX this
		annCreated = false;
		ann = null; 
	}

	
	
	private void ensure(String dir) {
		Log.e("In Ensure", "Start");
		//storage_path = new File( Environment.getExternalStorageDirectory()+ "/RobotEvolvApp");
		storage_path = new File( Environment.getExternalStorageDirectory()+"/" + dir);
		Log.e("In Ensure", "1");
		boolean success = true;
		Log.e("in ensure",storage_path.toString());
		if (!storage_path.exists()) {
			Log.e("In Ensure", "fail");
		    success = storage_path.mkdir();
		}
		if (success) {
			Log.e("In Ensure", "2");
			File file = new File(storage_path,storage_file);
			if(!file.exists()) {
				Log.e("In Ensure", "3");
				try {
					file.createNewFile();
				} catch (IOException e) {
					Toast.makeText(myActivity,"Error creating file!",Toast.LENGTH_LONG).show();
				}
			}

		} else {
			Log.e("In Ensure", "4");
			
			
		}
	}
	
	
	public void saveTrainingSet() {
		
		Log.e("ROBOT", "IN SAVE");
		final String msg;
	
		msg = ga.toString();
		
		myActivity.runOnUiThread(new Runnable() {
			  public void run() {
				  final EditText input = new EditText(myActivity);
			  
					new AlertDialog.Builder(myActivity)
				    .setTitle("Enter Save File Name")
				    .setMessage("File Name:")
				    .setView(input)
				    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            storage_file = input.getText().toString()+".txt";
				            ensure(evoDir);
				            if (writeToFile(storage_file, msg));
				            	dialog.cancel();
				        }
				    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            
				        }
				    }).show();
					
			  }
		});
	}
	
public void saveANN() {
		
		Log.e("ROBOT", "Save ANN 1");
		final String msg = createANNString();
		Log.e("ROBOT", "Save ANN 2 : " + msg);
		
		myActivity.runOnUiThread(new Runnable() {
			  public void run() {
				  final EditText input = new EditText(myActivity);
			  
					new AlertDialog.Builder(myActivity)
				    .setTitle("Enter Save File Name")
				    .setMessage("File Name:")
				    .setView(input)
				    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            storage_file = input.getText().toString()+".txt";
				            ensure(driveDir);
				            if (writeToFile(storage_file, msg));
				            	dialog.cancel();
				        }
				    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            
				        }
				    }).show();
					
			  }
		});
	}
	
	public String createANNString(){
		String annAsString = "";
		List<Connection> netConnections = new ArrayList<Connection>();
		
			for (Layer layer : ann.getLayers()) {
				
		        for (Neuron neuron : layer.getNeurons()) {
	
		            for (Connection connection : neuron.getInputConnections()) {
		            	netConnections.add(connection);
		            }
		        }
		    }
			for ( int i = 0; i < netConnections.size(); i++ ){
				annAsString += netConnections.get(i).getWeight().getValue();
				if( i != netConnections.size()-1 ){
					annAsString += ",";
				}
			}
		
		
		return annAsString;
	}
	
	public void loadANN() {
		ensure(driveDir);
		Log.e("ROBOT", "Load ANN 1");
		myActivity.runOnUiThread(new Runnable() { 
			public void run() {
				File[] fileList = storage_path.listFiles();
				String[] list = new String[fileList.length];
				
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(myActivity, android.R.layout.simple_spinner_item, list);
				//list[0] = "No Item Selected";
				if (fileList != null){
					int num = 0;
					if(fileList.length < 1){
						num = 1;
					}
		            for(int i = num; i<fileList.length;i++){
		            	list[i] = fileList[i].getName();
		            }
				}
				
				
				AlertDialog.Builder b = new Builder(myActivity);
			    b.setTitle("Select Training File");
			    b.setItems(list, new DialogInterface.OnClickListener(){
			    	public void onClick(DialogInterface dialog, int which) {
			    		String selected_file = ((AlertDialog)dialog).getListView().getItemAtPosition(which).toString();
			    		Log.e("ROBOT", "Load ANN 2");
			    		createANN(readFromFile(selected_file));
					    
			        }
			    });
			    

			    b.show();
			}});
				
	}
	
	public void createANN ( String values ){
		ann = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inputs, hidden, outputs );
		List<Connection> netConnections = new ArrayList<Connection>();
		Log.e("ROBOT", "Create ANN 1");
		for (Layer layer : ann.getLayers()) {
		
	        for (Neuron neuron : layer.getNeurons()) {

	            for (Connection connection : neuron.getInputConnections()) {
	            	netConnections.add(connection);
	            }
	        }
	    }
		Log.e("ROBOT", "Create ANN 2");
		String v[] = values.split(",");
		if( v.length != inputs * hidden + hidden + hidden*outputs + outputs ){
			Log.e("Robot", "Not enough values to create ANN, values: " + v.length);
		}else{
			Log.e("ROBOT", "Create ANN 3 inputs " + netConnections.size());
			for(int i = 0; i < netConnections.size(); i++){
				netConnections.get(i).getWeight().setValue(Double.parseDouble(v[i]));
			}
		}
		annCreated = true;
		//myRobotView.setANN(ann);
		Log.e("ROBOT", "Create ANN 4");
	}
	
	
	public void loadTrainingSet() {
		ensure(evoDir);
		
		myActivity.runOnUiThread(new Runnable() { 
			public void run() {
				File[] fileList = storage_path.listFiles();
				String[] list = new String[fileList.length];
				
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(myActivity, android.R.layout.simple_spinner_item, list);
				//list[0] = "No Item Selected";
				if (fileList != null){
					int num = 0;
					if(fileList.length < 1){
						num = 1;
					}
		            for(int i = num; i<fileList.length;i++){
		            	list[i] = fileList[i].getName();
		            }
				}
				
				
				AlertDialog.Builder b = new Builder(myActivity);
			    b.setTitle("Select Training File");
			    b.setItems(list, new DialogInterface.OnClickListener(){
			    	public void onClick(DialogInterface dialog, int which) {
			    		String selected_file = ((AlertDialog)dialog).getListView().getItemAtPosition(which).toString();
			    		
			    		ga.fromString(readFromFile(selected_file));
					    
			        }
			    });
			    

			    b.show();
			}});
				
	}
	
	private String readFromFile(String filename){
		File file = new File(storage_path,filename);
        StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				text.append(line);
				//text.append('\n');
			}
			br.close();
		}
		catch (IOException e) {
			Toast.makeText(myActivity,"Error reading file!",Toast.LENGTH_LONG).show();
		}
		return text.toString();
	}
	private String readFromFileWithNewline(String filename){
		File file = new File(storage_path,filename);
        StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append('\n');
			}
			br.close();
		}
		catch (IOException e) {
			Toast.makeText(myActivity,"Error reading file!",Toast.LENGTH_LONG).show();
		}
		return text.toString();
	}
	
	
	private boolean writeToFile(String filename, String content){
		File file = new File(storage_path, storage_file);
		try {
//			System.out.println(storage_file);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
		} catch (IOException e) {
			Toast.makeText(myActivity,"Error writing file!",Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}
	
}

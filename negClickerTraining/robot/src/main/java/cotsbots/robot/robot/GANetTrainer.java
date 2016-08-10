package cotsbots.robot.robot;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;

import nnet.MultiLayerPerceptron;
import util.TransferFunctionType;

import core.Connection;
import core.Layer;
import core.NeuralNetwork;
import core.Neuron;

public class GANetTrainer {
	
	
	public int left = 0, right = 0, forward = 0;
	//public List<double[]> testCases = new ArrayList<double[]>();// = new List<Double>;
	//public List<double[]> ans = new ArrayList<double[]>();
	public List<TrainingCase> trainingCases = new ArrayList<TrainingCase>();
	
	public int popSize;
	public int inLayerSize, hidLayerSize, outLayerSize; 
	
	public Random rng = new Random();
	
	public double mutationRate =.2;
	
	public List<NeuralNetwork> population = new ArrayList<NeuralNetwork>();
	public List<Double> fitness = new ArrayList<Double>();
	
	public List<NeuralNetwork> distPopulation = new ArrayList<NeuralNetwork>();
	public List<Double> distFitness = new ArrayList<Double>();
	
	
	public List<Double> averageFitness = new ArrayList<Double>();
	public List<Double> bestFitness = new ArrayList<Double>();
	public double currentBestFitness;
	public double currentAverageFitness;
	
	public NeuralNetwork best;
	public boolean hasBestArray = false;
	public double[]		 bestArray;
	public double bestError = 1000;
	
	public double fitThresh = .4;
	
	boolean distributed = false;
	boolean distributionStarted = false;
	public double[][] elitePopulation;
	public List<Double> eliteFitness = new ArrayList<Double>();
	public int eliteSize;
	public int elitesRecieved = 0;
	
	public double[][] populationArray;
	public int arraySize;
	public int generation = 0;
	
	
	
	GANetTrainer(int pop, int inLay, int hidLay, int outLay){
		popSize = pop;
		inLayerSize = inLay;
		hidLayerSize = hidLay;
		outLayerSize = outLay;
		
	}
	
	/*
	public void initialize(){
		for( int i = 0; i < popSize; i++ ){
			population.add( new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize ) );
			population.get(i).randomizeWeights(-1.0, 1.0);
			fitness.add(0.0);
		}
		calcFitness2();
		findBest();
		findAverage();
		bestFitness.add(currentBestFitness);
		averageFitness.add(currentAverageFitness);
		
	}
	*/
	public void initializeArray(){
		arraySize = inLayerSize * hidLayerSize + hidLayerSize * outLayerSize + hidLayerSize+ outLayerSize;// # of connections + bias
		populationArray = new double[popSize][arraySize];
		bestArray = new double[arraySize];
		
		for( int i = 0; i < popSize; i++ ){
			for ( int j = 0; j < arraySize; j++ ){
				populationArray[i][j] = rng.nextDouble()*2-1;
			}
			fitness.add(0.0);
		}
		calcFitnessArray();
		findBestArray();
		findAverage();
		bestFitness.add(currentBestFitness);
		//Log.e("GA", "Before");
		averageFitness.add(currentAverageFitness);
		//Log.e("GA", "after");
		if(distributed){
			Log.e("GA", "Dist started");
			elitePopulation = new double[eliteSize][arraySize];
			for(int i = 0; i < eliteSize; i++){
				eliteFitness.add(0.0);
			}
			distributionStarted = true;
		}
	}
	
	
public void runGenerationArray(){
		
		double[][] nextGen = new double[popSize][arraySize];
		if(!distributed){
			if( hasBestArray ){
				copyArray(nextGen[0],bestArray, arraySize);
				copyArray(nextGen[1],bestArray, arraySize);
			}
			for( int i = 2; i < popSize; i++ ){
				double[] parent1 = new double[arraySize],
						 parent2 = new double[arraySize],
						 offspring = new double[arraySize];
				parent1 = selectArray();
				parent2 = selectArray();
				offspring = crossoverArray(parent1, parent2);
				offspring = mutateArray(offspring);
				copyArray(nextGen[i],offspring, arraySize);
			}
			for( int i = 0; i < popSize; i++ ){
				copyArray(populationArray[i],nextGen[i], arraySize);
			}
		}else{
			int i = 0;
			if( hasBestArray ){
				copyArray(nextGen[0],bestArray, arraySize);
				i = 1;
			}
			for( ; i < popSize; i++ ){
				double[] parent1 = new double[arraySize],
						 parent2 = new double[arraySize],
						 offspring = new double[arraySize];
				parent1 = selectArray();
				parent2 = selectArray();
				offspring = crossoverArray(parent1, parent2);
				offspring = mutateArray(offspring);
				copyArray(nextGen[i],offspring, arraySize);
			}
			for( int j = 0; j < popSize; j++ ){
				copyArray(populationArray[j],nextGen[j], arraySize);
			}
		}
		
		calcFitnessArray();
		findBestArray();
		findAverage();
		bestFitness.add(currentBestFitness);
		averageFitness.add(currentAverageFitness);
		generation++;
	}
	
public double[] selectArray(){
	
	int position = -1;
	if (elitesRecieved > 0 && rng.nextDouble()<=.1){
		position = rng.nextInt(eliteSize);
		return elitePopulation[position];
	}else{
		for( int i = 0; i < 3; i++ ){
			if( position == -1 ){
				position = rng.nextInt(popSize);
			}else{
				int challenger = rng.nextInt(popSize);
				if( fitness.get(challenger) < fitness.get(position) ){
					position = challenger;
				}
			}
		}
		return populationArray[position];
	}
	
}


public double[] crossoverArray(double[] parent1, double[] parent2){
	
	double[] offspring = new double[arraySize];
	
	for( int i = 0; i < arraySize; i++ ){
		if( rng.nextBoolean() ){
			offspring[i] = parent1[i];
		}else{
			offspring[i] = parent2[i];
		}
	}
	return offspring;
}

public double[] mutateArray(double[] offspring){
	double mutAmount;
	for (int i = 0; i < arraySize; i++) {
		if ( rng.nextDouble() < mutationRate ){
			mutAmount = rng.nextDouble()*.2 -.1;
			offspring[i] += mutAmount;
		}
	}
	return offspring;
}


public void calcFitnessArray(){
	fitness.clear();
	int[] numMoves = calcMoves();
	//Log.e("New Fit", "" + numMoves[0] + " " + numMoves[1] + " " + numMoves[2]);
	NeuralNetwork currentNet = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
	for( int i = 0; i < popSize; i++){
		
		double fitVal = 0;
		copyWeightsToANN(currentNet, populationArray[i]);
		
		for( int j = 0; j < trainingCases.size(); j++ ){
				TrainingCase currentTC = trainingCases.get(j);
				if( currentTC.generation <= generation){
					currentNet.setInput(currentTC.input);
					currentNet.calculate();
					double[] expected = currentTC.output;
					double[] actual = currentNet.getOutput();
					fitVal += calcValue(actual, expected, numMoves, currentTC.isNeg);
				}
		}
		fitness.add(6-fitVal);
	}
	
}


public void calcEliteFitnessArray(){
	//eliteFitness.clear();
	int[] numMoves = calcMoves();
	//Log.e("New Fit", "" + numMoves[0] + " " + numMoves[1] + " " + numMoves[2]);
	NeuralNetwork currentNet = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );

	if( elitesRecieved > 0 ){
		for( int i = 0; i < eliteSize; i++){
			
			double fitVal = 0;
			copyWeightsToANN(currentNet, elitePopulation[i]);
			
			for( int j = 0; j < trainingCases.size(); j++ ){
				TrainingCase currentTC = trainingCases.get(j);
				if( currentTC.generation <= generation){
					currentNet.setInput(currentTC.input);
					currentNet.calculate();
					double[] expected = currentTC.output;
					double[] actual = currentNet.getOutput();
					fitVal += calcValue(actual, expected, numMoves, currentTC.isNeg);
				}
			}
			eliteFitness.remove(i);
			eliteFitness.add(i, 1 - fitVal/3);//.add(1 - fitVal/3);
		}
	}
}


	public int[] calcMoves(){
		int[] moves = new int[6];
		for ( int i = 0; i < trainingCases.size(); i++ ){
			TrainingCase current = trainingCases.get(i);
			if( current.generation <= generation){
				double[] answer = current.output;
				int pos = getWinner(answer);
				if( current.isNeg ){
					pos +=3;
				}
				moves[pos]++;
			}
		}
		return moves;

	}
	public int getWinner(double[] values){
		int winner = 0;
		if( values[0] > values[1] && values[0] > values[2] ){
			winner = 0;
		}else if( values[1] > values[0] && values[1] > values[2] ){
			winner = 1;
		}
		else if( values[2] > values[1] && values[2] > values[0] ){
			winner = 2;
		}
		return winner;
	}


    /*
        TODO
        identify positive and negative cases
        use root mean squared error
        two methods sent to sigmoid:

        1: error is the difference between the values

        2: it can be the difference between the winning value and the other values if the winning value is not as expected, fitness = 0
        in case2, subtract threshhold
     */

	public double calvValueUsingDiffSigmoid( double[] actual, double[] expected, int[] moves ){
		double value = 0.0;
		for ( int i = 0; i < 3; i++ ){
			value += sig( Math.sqrt(Math.pow( actual[i] - expected[i], 2 )) - fitThresh );
		}
		return value;
	}
	public double calvValueUsingSigmoid( double[] actual, double[] expected, int[] moves ){
		double value = 0.0;
		for ( int i = 0; i < 3; i++ ){
			value += sig( Math.sqrt(Math.pow( actual[i] - expected[i], 2 )) );
		}
		return value;
	}
public double sig( double x ){

	return (1 / (1 + Math.exp(-(x)))) ;
}

	public double calcValue(double[] actual, double[] expected, int[] moves, boolean neg){
		double value = 0.0;
		double diff1 = 0.0, diff2 = 0.0;

		int expWin = getWinner(expected);
		double expectVal = actual[expWin];
		if( !neg ){
			if (expWin == 0) {
				diff1 = getDiff(expectVal, actual[1]);
				diff2 = getDiff(expectVal, actual[2]);
			} else if (expWin == 1) {
				diff1 = getDiff(expectVal, actual[0]);
				diff2 = getDiff(expectVal, actual[2]);
			} else if (expWin == 2) {
				diff1 = getDiff(expectVal, actual[0]);
				diff2 = getDiff(expectVal, actual[1]);
			}
		}else {
			if (expWin == 0) {
				diff1 = getDiff(actual[1], expectVal);
				diff2 = getDiff(actual[2], expectVal);
			} else if (expWin == 1) {
				diff1 = getDiff(actual[0], expectVal);
				diff2 = getDiff(actual[2], expectVal);
			} else if (expWin == 2) {
				diff1 = getDiff(actual[0], expectVal);
				diff2 = getDiff(actual[1], expectVal);
			}
			expWin+=3;
		}
		value += calcPoints( diff1, moves[expWin]);
		//Log.e("New Fit", " Diff1 " + diff1 + " " + value);
		value += calcPoints( diff2, moves[expWin]);

		//Log.e("New Fit", " Diff2 " + diff2 + " " + value);
		return value;
	}

	public double calcPoints( double diff, int numMove){
		double points = 0.0;

		if( diff >= fitThresh){
			points += .5/numMove;
		}else{
			points += (.5*(diff/fitThresh))/numMove;
		}


		return points;


	}

	public double getDiff( double expectVal, double actVal ){
		double difference = 0.0;
		difference = expectVal - actVal;
		return difference;
	}





	public void copyArray(double[] ar1, double[] ar2, int size){
	for ( int i = 0; i < size; i++ ){
		ar1[i] = ar2[i];
	}
}


public void copyWeightsToANN( NeuralNetwork net, double[] weights){
	List<Connection> netConnections = new ArrayList<Connection>();
	
	for (Layer layer : net.getLayers()) {
	
        for (Neuron neuron : layer.getNeurons()) {

            for (Connection connection : neuron.getInputConnections()) {
            	netConnections.add(connection);
            }
        }
    }
	if( netConnections.size() != arraySize ){
		Log.e("COPY WEIGHTS", "connections: " + netConnections.size() + " arraySize: " +arraySize);
	}else{
		for ( int i = 0; i < arraySize; i++ ){
			netConnections.get(i).getWeight().setValue(weights[i]);
		}
	}
}

public void findBestArray(){
	int bestPos = -1;
	//String msg = "";
	for ( int i = 0; i < fitness.size(); i++ ){
		//msg += " " + fitness.get(i);
		if( bestPos == -1 ){
			bestPos = i;
		}else{
			if( fitness.get(i) < fitness.get(bestPos)){
				bestPos = i;
			}
		}
	}
	//System.out.println(msg);
	copyArray(bestArray, populationArray[bestPos], arraySize);
	hasBestArray = true;
	currentBestFitness = fitness.get(bestPos);
	bestError = fitness.get(bestPos);
	best = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
	copyWeightsToANN(best, bestArray);
}


public void findVeryBestArray(){
	if( elitesRecieved > 0 ){
		calcEliteFitnessArray();
		int bestPos = -1;
		double bestInPop = 10.0;
		double bestInElite = 10.0;
	
		for ( int i = 0; i < fitness.size(); i++ ){
		
			if( bestPos == -1 ){
				bestPos = i;
				bestInPop = fitness.get(i);
			}else{
				if( fitness.get(i) < fitness.get(bestPos)){
					bestPos = i;
					bestInPop = fitness.get(i);
				}
			}
		}
		int bestElitePos = -1;
	
		for ( int j = 0; j < eliteFitness.size(); j++ ){
			
			if( bestElitePos == -1 ){
				bestElitePos = j;
			}else{
				if( eliteFitness.get(j) < eliteFitness.get(bestElitePos)){
					bestElitePos = j;
					bestInElite = eliteFitness.get(j);
				}
			}
		}
		
		if( bestInPop < bestInElite ){
			copyArray(bestArray, populationArray[bestPos], arraySize);
			hasBestArray = true;
			currentBestFitness = fitness.get(bestPos);
			bestError = fitness.get(bestPos);
			best = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
			copyWeightsToANN(best, bestArray);
		}else{
			copyArray(bestArray, elitePopulation[bestElitePos], arraySize);
			hasBestArray = true;
			currentBestFitness = eliteFitness.get(bestElitePos);
			bestError = eliteFitness.get(bestElitePos);
			best = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
			copyWeightsToANN(best, bestArray);
		}
		
		
	}
	
	
}


/*
	
	public void runGeneration(){
		
		List<NeuralNetwork> nextGen = new ArrayList<NeuralNetwork>();
		if( hasBest() ){
			nextGen.add(copyNetwork(best));
			nextGen.add(copyNetwork(best));
		}
		while( nextGen.size() < popSize ){
			NeuralNetwork parent1, parent2, offspring;
			parent1 = select();
			parent2 = select();
			offspring = crossover(parent1, parent2);
			mutate(offspring);
			nextGen.add(copyNetwork(offspring));
		}
		population.clear();
		for( int i = 0; i < nextGen.size(); i++ ){
			population.add(copyNetwork(nextGen.get(i)));
		}
		calcFitness2();
		findBest();
		findAverage();
		bestFitness.add(currentBestFitness);
		averageFitness.add(currentAverageFitness);
	}
	*/
	public void findAverage() {
		double a = 0;
		for ( int i = 0; i < fitness.size(); i++ ){
			a += fitness.get(i);
		}
		currentAverageFitness = a / fitness.size();
		
	}


	public String printNet( NeuralNetwork myNet ){
		String net = "";
		for (Layer layer : myNet.getLayers()) {
			net += " NEW LAYER \n" + layer.getNeuronsCount();
            for (Neuron neuron : layer.getNeurons()) {
            	net += "\n";
                for (Connection connection : neuron.getInputConnections()) {
                	net += connection.getWeight().value + " ";
                }
                
            }
            
        }
		
		return net;
	}
	
	public NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2){
		
		NeuralNetwork offspring = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
		List<Connection> offspringconnections = new ArrayList<Connection>();
		List<Connection> parent1connections = new ArrayList<Connection>();
		List<Connection> parent2connections = new ArrayList<Connection>();
		
		for (Layer layer : offspring.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {

                for (Connection connection : neuron.getInputConnections()) {
                	offspringconnections.add(connection);
                }
            }
        }
		
		for (Layer layer : parent1.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getInputConnections()) {
                	parent1connections.add(connection);
                }
            }
        }
		for (Layer layer : parent2.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getInputConnections()) {
                	parent2connections.add(connection);
                }
            }
        }
		
		
		for( int i = 0; i < offspringconnections.size(); i++ ){
			if( rng.nextBoolean() ){
				offspringconnections.get(i).getWeight().setValue(parent1connections.get(i).getWeight().value);
			}else{
				offspringconnections.get(i).getWeight().setValue(parent2connections.get(i).getWeight().value);
			}
		}

		return offspring;
	}
	
	public void mutate(NeuralNetwork offspring){
		double mutAmount;
		
		for (Layer layer : offspring.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getInputConnections()) {
                	if ( rng.nextDouble() < mutationRate ){
                		mutAmount = rng.nextDouble()*.2 -.1;
                		connection.getWeight().setValue(connection.getWeight().value + mutAmount);

                	}
                }
            }
        }
	}
	
	
	public NeuralNetwork select(){
		NeuralNetwork selected;
		int position = -1;
		//Log.e("IN SELECT", "POP SIZE: " + population.size() +"   "+"FIT SIZE: "+fitness.size());
		for( int i = 0; i < 3; i++ ){
			if( position == -1 ){
				position = rng.nextInt(popSize);
			}else{
				int challenger = rng.nextInt(popSize);
				if( fitness.get(challenger) < fitness.get(position) ){
					position = challenger;
				}
			}
		}
		
		selected = population.get(position);
		
		return selected;		
	}
	
	public NeuralNetwork copyNetwork( NeuralNetwork inNet ){
		
		NeuralNetwork copiedNet = new MultiLayerPerceptron( TransferFunctionType.SIGMOID, inLayerSize, hidLayerSize, outLayerSize );
		
		List<Connection> innetconnections = new ArrayList<Connection>();
		List<Connection> copiedconnections = new ArrayList<Connection>();
		
		for (Layer layer : inNet.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getInputConnections()) {
                	innetconnections.add(connection);
                }
            }
        }
		
		for (Layer layer : copiedNet.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getInputConnections()) {
                	copiedconnections.add(connection);
                }
            }
        }
		
		for( int i = 0; i < copiedconnections.size(); i++){
			copiedconnections.get(i).getWeight().value = innetconnections.get(i).getWeight().value;
		}
		
		
		return copiedNet;
	}
	/*
	public void calcFitness(){
		fitness.clear();

		for( int i = 0; i < population.size(); i++){
			
			double totalError = 0;
			NeuralNetwork currentNet = population.get(i);
			
			for( int j = 0; j < testCases.size(); j++ ){
				
				double[] testAnswer = ans.get(j);
				currentNet.setInput(testCases.get(j));
				currentNet.calculate();
				double[] currentOut = currentNet.getOutput();
				totalError += calcError( testAnswer, currentOut);
				
			}
			fitness.add(Math.sqrt(totalError)/testCases.size());
		}
		
	}
	
	public double calcError( double[] expected, double[] recieved ){
		double error = 0;
		for (int i = 0; i < outLayerSize; i++){
			error += Math.pow( (expected[i]-recieved[i]), 2);
		}
		return error;
	}
	*/
	public void addTrainingCase( int gen, double[] input, double[] output, boolean neg ){
		if( output[0] > output[1] && output[0] > output[2] ){
			left++;
		}else if( output[1] > output[0] && output[1] > output[2] ){
			forward++;
		}
		else if( output[2] > output[1] && output[2] > output[0] ){
			right++;
		}
		
		trainingCases.add(new TrainingCase(gen, output, input, neg));
		
	}
	
	/*
	public void calcFitness2(){
		fitness.clear();
		int[] numMoves = calcMoves();
		//Log.e("New Fit", "" + numMoves[0] + " " + numMoves[1] + " " + numMoves[2]);
		for( int i = 0; i < population.size(); i++){
			
			double fitVal = 0;
			NeuralNetwork currentNet = population.get(i);
			
			for( int j = 0; j < testCases.size(); j++ ){
				currentNet.setInput(testCases.get(j));
				currentNet.calculate();
				double[] expected = ans.get(j);
				double[] actual = currentNet.getOutput();
				fitVal += calcValue(actual, expected, numMoves);
			
			}
			fitness.add(1 - fitVal/3);
		}
		
		
	}
	*/
	

	public int getNumTestcases(){
		return trainingCases.size();
	}
	
	public void findBest(){
		int bestPos = -1;
		//String msg = "";
		for ( int i = 0; i < fitness.size(); i++ ){
			//msg += " " + fitness.get(i);
			if( bestPos == -1 ){
				bestPos = i;
			}else{
				if( fitness.get(i) < fitness.get(bestPos)){
					bestPos = i;
				}
			}
		}
		//System.out.println(msg);
		best = copyNetwork(population.get(bestPos));
		currentBestFitness = fitness.get(bestPos);
		bestError = fitness.get(bestPos);
	}
	
	public boolean hasBest(){
		if( best != null ){
			return true;
		}
		else{
			return false;
		}
	}
	public NeuralNetwork getBest(){
		return best;
	}


	public void setUpDistrobuted(int numberOfDistBots) {
		Log.e("GA", "Dist setup");
		eliteSize = numberOfDistBots;
		distributed = true;
	}

	public void updateElites(double [][] elites){
		
		if (distributionStarted){
			Log.e("GA", "Elites Recieved");
			elitesRecieved++;
			for( int i = 0; i < eliteSize; i++){
				copyArray(elitePopulation[i], elites[i], arraySize);
			}
			//calcEliteFitnessArray();
		}
	}

	public List<Double> getAverageList() {
		return averageFitness;
	}


	public List<Double> getBestList() {
		return bestFitness;
	}


	public double getBestFitness() {
		return currentBestFitness;
	}


	public double getAverageFitness() {
		return currentAverageFitness;
	}


	public String bestArrayToString(){
		String b = "";
		
		if (hasBest()){
			for(int i = 0; i < bestArray.length; i++){
				if ( i != bestArray.length -1 ){
					b += bestArray[i];
					b += ",";
				}else{
					b += bestArray[i];
				}
			}
		}
		
		return b;
	}
	// ; separates training case " " separates  gen, output, input and , separates values
	public String toString() {
		String tc = "";
		
		for ( int i = 0; i < trainingCases.size(); i++ ){
			
			TrainingCase current = trainingCases.get(i);
			tc += current.generation;
			tc += " ";
			tc += current.isNeg;
			tc += " ";
			for ( int j = 0; j < current.output.length; j++ ){
				tc += current.output[j];
				if( j != current.output.length-1 ){
					tc += ",";
				}
			}
			tc += " ";
			for ( int j = 0; j < current.input.length; j++ ){
				tc += current.input[j];
				if( j != current.input.length-1 ){
					tc += ",";
				}
			}
			
			if( i != trainingCases.size()-1 ){
				tc += ";\n";
			}else{
				tc += ";";
			}
			
		}
		
		
		return tc;
	}
	
	
	
	public void fromString( String m ){
		Log.e("GATrainer", "Loading Training Set");
		String[] lines = m.split(";");
		
		for( int i = 0; i < lines.length ; i++ ){
			String[] line = lines[i].split(" ");
			String n = line[1];
			String[] outValues = line[2].split(",");
			String[] inValues = line[3].split(",");
			int gen;
			boolean neg;
			double[] outp = new double[outValues.length];
			double[] inp = new double[inValues.length];
			
			gen = Integer.parseInt(line[0]);
			neg = Boolean.parseBoolean(n);
			for (int j = 0; j < outValues.length; j++){
				outp[j] = Double.parseDouble(outValues[j]);
			}
			for (int j = 0; j < inValues.length; j++){
				inp[j] = Double.parseDouble(inValues[j]);
			}
			
			addTrainingCase(gen, inp, outp, neg);
			
		}
		
		Log.e("GATrainer", "Finished Loading Training Set");
	}


	

}
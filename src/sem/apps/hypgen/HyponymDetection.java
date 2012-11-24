package sem.apps.hypgen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimMeasure;
import sem.util.FileReader;
import sem.util.Pair;
import sem.util.Tools;

/**
 * Class for running experiments with hyponym detection.
 *
 */
public class HyponymDetection {
	
	/**
	 * Read in the gold standard annotation from the input file.
	 * Also lowercases the labels, and appends and uppercased POS tag to them, to make them match the model.
	 * For example, "Lassie" becomes "lassie_NOUN".
	 * @param inputFile Path to gold standard file
	 * @param pos The POS tag
	 * @return
	 */
	public static LinkedHashMap<Pair<String>,Integer> readGold(String inputFile, String pos){
		FileReader fileReader = new FileReader(inputFile);
		LinkedHashMap<Pair<String>,Integer> goldPairs = new LinkedHashMap<Pair<String>,Integer>();
		// Reading input
		String line, word1, word2;
		String[] lineParts;
		Integer decision;
		while(fileReader.hasNext()){
			line = fileReader.next().trim();
			lineParts = line.split("\\t+");
			if(lineParts.length != 3)
				throw new RuntimeException("Wrong input file format: " + line);
			
			// This is done to match the model which contains lowercased labels. 
			// If you use a different model, you need to change this accordingly.
			word1 = lineParts[0].toLowerCase() + "_" + pos.toUpperCase();
			word2 = lineParts[1].toLowerCase() + "_" + pos.toUpperCase();
			
			decision = Integer.parseInt(lineParts[2]);
			
			Pair<String> wordPair = new Pair<String>(word1, word2);
			goldPairs.put(wordPair, decision);
		}
		fileReader.close();
		
		return goldPairs;
	}
	
	/**
	 * Take the pairs in the input file and assign scores to them using the similarity measure.
	 * @param semModel
	 * @param vectorSpace
	 * @param goldPairs
	 * @param simMeasureType
	 * @return
	 */
	public static LinkedHashMap<Pair<String>,Double> runScoring(SemModel semModel, VectorSpace vectorSpace, LinkedHashMap<Pair<String>,Integer> goldPairs, SimMeasure simMeasure){
		LinkedHashMap<Pair<String>,Double> scoredPairs = new LinkedHashMap<Pair<String>,Double>();		
		LinkedHashMap<Integer,Double> vector1, vector2;
		for(Pair<String> wordPair : goldPairs.keySet()){
			vector1 = vectorSpace.getVector(wordPair.getItem1());
			vector2 = vectorSpace.getVector(wordPair.getItem2());	
			
			double score = simMeasure.sim(vector1, vector2);
			
			if(Double.isInfinite(score) || Double.isNaN(score))
				throw new RuntimeException("Illegal score value: " + score);
			scoredPairs.put(wordPair, score);
		}
		
		return scoredPairs;
	}
	
	public static double runEvaluation(boolean isDistance, LinkedHashMap<Pair<String>,Double> scoredPairs, LinkedHashMap<Pair<String>,Integer> goldPairs){
		HashMap<String,HashSet<Pair<String>>> sets = new HashMap<String,HashSet<Pair<String>>>();
		double sum = 0.0, totalCorrect = 0, correctRetrieved = 0, totalRetrieved = 0;
		
		if(scoredPairs.size() != goldPairs.size())
			throw new RuntimeException("Scoredpairs needs to have the same number of elements as goldpairs");
		
		for(Entry<Pair<String>,Integer> e : goldPairs.entrySet()){
			if(e.getValue().equals(1))
				totalCorrect++;
			if(!sets.containsKey(e.getKey().getItem2()))
				sets.put(e.getKey().getItem2(), new HashSet<Pair<String>>());
			sets.get(e.getKey().getItem2()).add(e.getKey());
		}
		
		// Sorting
		if(isDistance)
			scoredPairs = Tools.sort(scoredPairs, false);
		else
			scoredPairs = Tools.sort(scoredPairs, true);
		
		// Calculating AP and BEP
		for(Entry<Pair<String>,Double> e : scoredPairs.entrySet()){
			totalRetrieved++;
			if(goldPairs.get(e.getKey()).equals(1)){
				correctRetrieved++;
				sum += (correctRetrieved / totalRetrieved);
			}
		}
		double ap = sum / totalCorrect;
		return ap;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Configuration variables
		//String modelPath = "/anfs/bigdisc/mr472/SemTensor/model1"; 
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/model1"; 
		
		SemModel semModel = new SemModel(modelPath, true, false);
		semModel.makeTensorSymmetric();
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		 
		// We run it on many gold standard files in a row. Feel free to change this.
		LinkedHashMap<String,String> inputFiles = new LinkedHashMap<String,String>();
		for(String pos : Arrays.asList("noun", "verb")){
			for(String set : Arrays.asList("train", "dev", "test")){
				String inputFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-detection-" + pos + "-" + set + ".txt";
				inputFiles.put(inputFile, pos);
			}
		}
		inputFiles.put("/anfs/bigdisc/mr472/corpora/LexEntail-Dagan/judgements_filtered.txt", "noun");

		for(SimMeasure simMeasure : SimMeasure.values()){
			System.out.print(simMeasure.getLabel());
			for(String inputFile : inputFiles.keySet()){
				LinkedHashMap<Pair<String>,Integer> goldPairs = readGold(inputFile, inputFiles.get(inputFile));
				LinkedHashMap<Pair<String>,Double> scoredPairs = runScoring(semModel, vectorSpace, goldPairs, simMeasure);
				double ap = runEvaluation(simMeasure.isDistance(), scoredPairs, goldPairs);
				System.out.print("\t" + ap);
			}
			System.out.println();
		}
		

	}

}

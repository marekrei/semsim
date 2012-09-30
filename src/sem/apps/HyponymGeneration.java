package sem.apps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimFinder;
import sem.sim.SimMeasure;
import sem.util.FileReader;
import sem.util.FileWriter;
import sem.util.Tools;

/**
 * Class for running experiments with hyponym generation.
 *
 */
public class HyponymGeneration {
	
	/**
	 * Read in the gold standard file for hyponym generation
	 * @param inputFile
	 * @param pos
	 * @return
	 */
	public static LinkedHashMap<String,LinkedHashSet<String>> readGold(String inputFile, String pos){
		LinkedHashMap<String,LinkedHashSet<String>> gold = new LinkedHashMap<String,LinkedHashSet<String>>();
		
		FileReader fileReader = new FileReader(inputFile);
		String[] lineParts;
		while(fileReader.hasNext()){
			lineParts = fileReader.next().trim().toLowerCase().split("\\t+");
			if(lineParts.length <= 1)
				continue;
			String mainWord = lineParts[0].toLowerCase() + "_" + pos.toUpperCase();
			gold.put(mainWord, new LinkedHashSet<String>());
			for(int i = 1; i < lineParts.length; i++)
				gold.get(mainWord).add(lineParts[i].toLowerCase() + "_" + pos.toUpperCase());
		} 
		fileReader.close();
		
		return gold;
	}
	
	public static double calculateAP(boolean isDistance, LinkedHashMap<String,Double> predictions, LinkedHashSet<String> gold){
		double countReturned = 0.0, countCorrectReturned = 0.0, sumP = 0.0;
		LinkedHashMap<String,Double> scores;
		if(isDistance)
			scores = Tools.sort(predictions, false);
		else
			scores = Tools.sort(predictions, true);
		for(String w : scores.keySet()){
			countReturned++;
			if(gold.contains(w)){
				countCorrectReturned++;
				sumP += (double)countCorrectReturned / (double)countReturned;
			}
		}
		double ap = (sumP / (double)gold.size());
		return ap;
	}
	
	public static double calculateMAP(boolean isDistance, LinkedHashMap<String,LinkedHashMap<String,Double>> predictions, LinkedHashMap<String,LinkedHashSet<String>> gold){
		double sum = 0.0;
		for(String word : gold.keySet()){
			double ap = calculateAP(isDistance, predictions.get(word), gold.get(word));
			sum += ap;
		}
		return sum / (double)gold.size();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length == 6 || args.length == 7){
			String simMeasureLabel = args[0];
			String pos = args[1];
			String inputFile = args[2]; // "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-noun-dev.txt"
			String modelPath = args[3]; // "/anfs/bigdisc/mr472/SemTensor/model1"
			int minFreq = Tools.getInt(args[4], -1);
			int threadCount = Tools.getInt(args[5], -1);
			String outputPath = args.length == 7?args[6]:null;
			
			if(threadCount < 0)
				throw new RuntimeException("Illegal value for threadCount");
			if(minFreq < 0)
				throw new RuntimeException("Illegal value for minFreq");
			
			System.out.println("### Running: " + simMeasureLabel + " " + pos + " " + inputFile + " " + threadCount);
			
			SimMeasure simMeasure = SimMeasure.getType(simMeasureLabel);
			
			SemModel semModel = new SemModel(true, modelPath); 
			semModel.makeTensorSymmetric();
			VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
			
			LinkedHashMap<String,LinkedHashSet<String>> gold = readGold(inputFile, pos);
			
			LinkedHashSet<String> candidateWords = new LinkedHashSet<String>();
			for(String s : semModel.getNodeIndex().getIdMap().keySet()){
				if(semModel.getNodeCount(s) >= minFreq && s.toLowerCase().endsWith("_" + pos.toLowerCase())){
					candidateWords.add(s);
				}
			}
			
			SimFinder simFinder = new SimFinder(vectorSpace);
			LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = simFinder.getScores(new LinkedHashSet<String>(gold.keySet()), candidateWords, simMeasure, false, threadCount);
		
			// Extra output
			if(outputPath != null){
				ArrayList<String> candidateArray = new ArrayList<String>(candidateWords);
				Collections.sort(candidateArray);
				FileWriter fw0 = new FileWriter(outputPath);
				for(String candidate : candidateArray){
					fw0.writeln(candidate);
				}
				for(String mainWord : gold.keySet()){
					fw0.writeln("## " + mainWord);
					for(String candidate : candidateArray){
						fw0.writeln("" + predictions.get(mainWord).get(candidate));
					}
				}
				fw0.close();
				Tools.runCommand("gzip " + outputPath);
			}
			
			// Removing itself
			for(String s : predictions.keySet())
				predictions.get(s).remove(s);
			
			double map = calculateMAP(simMeasure.isDistance(), predictions, gold);
			System.out.println(map);
		}
		else {
			System.out.println("HyponymGeneration <simmeasure> <pos> <inputfile> <modelpath> <minfreq> <threadcount> [outputpath]");
		}

	}

}

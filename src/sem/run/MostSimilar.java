package sem.run;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimFinder;
import sem.sim.SimMeasure;
import sem.util.Tools;

/**
 * Given a word, find most similar words.
 */
public class MostSimilar {
	public static void main(String[] args) {
		if(args.length == 4 || args.length == 5){
			SemModel semModel = new SemModel(args[0], false);
			if(semModel == null)
				throw new RuntimeException("Model is null");
			
			SimMeasure simMeasure = SimMeasure.getType(args[1]);
			if(simMeasure == null)
				throw new RuntimeException("SimMeasureType is null");
			
			int frequencyLimit = Integer.parseInt(args[2]);
			int resultLimit = Integer.parseInt(args[3]);
			
			semModel.makeTensorSymmetric();
			VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
			SimFinder simFinder = new SimFinder(vectorSpace);
			
			LinkedHashSet<String> candidateWords = new LinkedHashSet<String>();
			for(String s : semModel.getNodeIndex().getIdMap().keySet()){
				if(semModel.getNodeCount(s) >= frequencyLimit){
					candidateWords.add(s);
				}
			}
			
			if(args.length == 5){
				LinkedHashMap<String,Double> results = simFinder.getScores(args[4], candidateWords, simMeasure, false);
				int count = 0;
				for(Entry<String,Double> e : Tools.sort(results, !simMeasure.isDistance()).entrySet()){
					System.out.println(e.getKey() + "\t" + e.getValue());
					if(++count >= resultLimit)
						break;
				}
			}
			else {
				try{
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					while(true){
						String line = br.readLine();
						if(line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))
							break;
						LinkedHashMap<String,Double> results = simFinder.getScores(line.trim(), candidateWords, simMeasure, false);
						int count = 0;
						for(Entry<String,Double> e : Tools.sort(results, !simMeasure.isDistance()).entrySet()){
							System.out.println(e.getKey() + "\t" + e.getValue());
							if(++count >= resultLimit)
								break;
						}
					}
				} catch(Exception e){
					throw new RuntimeException(e);
				}
			}
		}
		else {
			System.out.println("MostSimilar <modelpath> <similaritytype> <frequencylimit> <resultlimit> [word1]");
		}
	}

}

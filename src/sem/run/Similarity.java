package sem.run;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimFinder;
import sem.sim.SimMeasure;

/**
 * Find the similarity between two words.
 *
 */
public class Similarity {
	public static void main(String[] args) {
		if(args.length == 2 || args.length == 4){
			SemModel semModel = new SemModel(args[0], false);
			if(semModel == null)
				throw new RuntimeException("Model is null");
			
			SimMeasure simMeasure = SimMeasure.getType(args[1]);
			if(simMeasure == null)
				throw new RuntimeException("SimMeasureType is null");
			
			semModel.makeTensorSymmetric();
			VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
			SimFinder simFinder = new SimFinder(vectorSpace);
			
			if(args.length == 4){
				System.out.println(simFinder.getScore(simMeasure, args[2], args[3]));
			}
			else {
				try{
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					while(true){
						String line = br.readLine();
						if(line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))
							break;
						String[] words = line.split("\\s+");
						if(words.length != 2)
							throw new RuntimeException("Input should contain 2 words separated by whitespace.");
						System.out.println(simFinder.getScore(simMeasure, words[0], words[1]));
					}
				} catch(Exception e){
					throw new RuntimeException(e);
				}
			}
		}
		else {
			System.out.println("Similarity <modelpath> <similaritytype> [word1] [word2]");
		}
	}

}

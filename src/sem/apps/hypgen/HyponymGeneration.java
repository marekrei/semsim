package sem.apps.hypgen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

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

	public static double run(SimMeasure simMeasure, String pos, String inputFile, SemModel semModel, VectorSpace vectorSpace, int minFreq, int threadCount, String outputPath){
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
		return map;
	}
	
	public static void runExperiments(){
		int threadCount = 8;
		int minFreq = 10;
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/model1";
		String outputPath = null;
		
		SemModel semModel = new SemModel(modelPath, true, false); 
		semModel.makeTensorSymmetric();
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		
		for(String dataset : Arrays.asList("dev", "test")){
			for(String pos : Arrays.asList("noun", "verb")){
				for(SimMeasure simMeasure : Arrays.asList(SimMeasure.COSINE, SimMeasure.WEIGHTED_COSINE)){
					String inputFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-" + pos + "-" + dataset + ".txt";
					System.out.print(dataset + "\t" + pos + "\t" + simMeasure.getLabel() + "\t");
					double map = run(simMeasure, pos, inputFile, semModel, vectorSpace, minFreq, threadCount, outputPath);
					System.out.println(map);
				}
			}
		}
	}
	
	public static void printLatexSamples(){
		int threadCount = 8;
		int minFreq = 10;
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/model1";
		
		SemModel semModel = new SemModel(modelPath, true, false); 
		semModel.makeTensorSymmetric();
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		
		LinkedHashMap<String,ArrayList<String>> examples = new LinkedHashMap<String,ArrayList<String>>();
		examples.put("noun", new ArrayList<String>(Arrays.asList("sport_NOUN", "weapon_NOUN", "fabric_NOUN", "parent_NOUN", "politician_NOUN", "procedure_NOUN", "pleasure_NOUN", "narcotic_NOUN", "treatment_NOUN", "linguist_NOUN", "scientist_NOUN", "limitation_NOUN", "nutrient_NOUN", "vegetable_NOUN", "support_NOUN", "attribute_NOUN", "fruit_NOUN", "sex_NOUN")));
		examples.put("verb", new ArrayList<String>(Arrays.asList("travel_VERB", "take_VERB", "distribute_VERB", "sneak_VERB", "guarantee_VERB", "play_VERB", "address_VERB", "meet_VERB", "solve_VERB", "display_VERB", "confirm_VERB")));
		
		SimFinder simFinder = new SimFinder(vectorSpace);
		
		for(String pos : examples.keySet()){
			
			LinkedHashSet<String> candidateWords = new LinkedHashSet<String>();
			for(String s : semModel.getNodeIndex().getIdMap().keySet()){
				if(semModel.getNodeCount(s) >= minFreq && s.toLowerCase().endsWith("_" + pos.toLowerCase())){
					candidateWords.add(s);
				} 
			}
			
			String goldPath = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-"+pos+"-train.txt";
			LinkedHashMap<String,LinkedHashSet<String>> gold = null;
			if((new File(goldPath)).exists()){
				gold = readGold(goldPath, pos);
			}
			
			for(String word : examples.get(pos)){
				System.out.println("%%%% " + word);
				System.out.println("\\begin{table}[h]\n"+
						//"\\footnotesize\n"+
						"\\begin{tabular}{p{3cm}|p{10.5cm}}\n"+
						//"\\hline\n"+
						"\\multicolumn{2}{c}{\\textbf{" + word.substring(0, word.lastIndexOf('_'))+ " (" + pos + ") }} \\\\ \\hline \\hline"); 
				for(SimMeasure simMeasure : Arrays.asList(SimMeasure.COSINE, SimMeasure.DICE_GEN_2, SimMeasure.BAL_AP_INC, SimMeasure.CLARKE_DE, SimMeasure.BAL_PREC, SimMeasure.WEIGHTED_COSINE)){
					//System.out.println("#### : " + word + " " + simMeasure.getLabel());
					System.out.print(Character.toUpperCase(simMeasure.getLabel().charAt(0)) + simMeasure.getLabel().substring(1) + " & ");
					LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = simFinder.getScores(new LinkedHashSet<String>(Arrays.asList(word)), candidateWords, simMeasure, false, threadCount);
					int count = 0;
					ArrayList<String> topexamples = new ArrayList<String>();
					for(Entry<String,Double> e : Tools.sort(predictions.get(word), true).entrySet()){
						if(e.getKey().equals(word))
							continue;
						if(gold != null && gold.containsKey(word) && gold.get(word).contains(e.getKey()))
							topexamples.add("\\textbf{" + e.getKey().substring(0, e.getKey().lastIndexOf('_')) + "}"); 
						else
							topexamples.add(e.getKey().substring(0, e.getKey().lastIndexOf('_')));
						
						if(++count >= 15)
							break;
					}
					System.out.println(Tools.join(topexamples, ", ") + " \\\\ \\hline"); 
				}
				
				System.out.println("\\end{tabular}"+
						//"\\caption{}\n" +
						//"\\label{}\n"+
						"\\end{table}\n\n");
			}
		}
	}
	
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
			
			SimMeasure simMeasure = SimMeasure.getType(simMeasureLabel);
			SemModel semModel = new SemModel(modelPath, true, false); 
			semModel.makeTensorSymmetric();
			VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
			
			double map = run(simMeasure, pos, inputFile, semModel, vectorSpace, minFreq, threadCount, outputPath);
			System.out.println("MAP: " + map);
		}
		else {
			//System.out.println("HyponymGeneration <simmeasure> <pos> <inputfile> <modelpath> <minfreq> <threadcount> [outputpath]");
			//runExperiments();
			printLatexSamples();
		}
	}

}

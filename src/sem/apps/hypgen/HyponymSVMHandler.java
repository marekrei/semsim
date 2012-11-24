package sem.apps.hypgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimMeasure;
import sem.util.FeatureNormaliser;
import sem.util.FileReader;
import sem.util.FileWriter;
import sem.util.Pair;
import sem.util.Tools;

class SystemTask implements Runnable{
	public String command;
	
	public SystemTask(String command){
		this.command = command;
	}
	
	@Override
	public void run() {
		System.out.println("Running: " + command);
		Tools.runCommand(command);
	}
	
	public static void run(ArrayList<String> commands, int numThreads){
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 30, TimeUnit.SECONDS, queue);
		
		for(String command : commands){
			SystemTask task = new SystemTask(command);
			Future<?> future = executor.submit(task);
			futures.add(future);
		}
		
		// Wait to finish
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		futures.clear();
		queue.clear();
		executor.shutdown();
	}
}

public class HyponymSVMHandler {

	public static ArrayList<SimMeasure> measures = new ArrayList<SimMeasure>();
	public static HashMap<String,Integer> features = new HashMap<String,Integer>();
	
	public static Integer getFeatureId(String label){
		Integer id = features.get(label);
		if(id == null){
			id = features.size() + 1;
			if(features.containsKey(id))
				throw new RuntimeException("Conflict in id values");
			features.put(label, id);
		}
		return id;
	}
	
	public static String getKey(Pair<String> pair){
		return pair.getItem1() + "[[[;;;]]]" + pair.getItem2();
	}
	
	public static String getKey(String w1, String w2){
		return w1 + "[[[;;;]]]" + w2;
	}
	
	public static LinkedHashMap<Pair<String>,Double> readScores(String inputFile, LinkedHashMap<Pair<String>,Integer> pairs){
		LinkedHashMap<String,Pair<String>> map = new LinkedHashMap<String,Pair<String>>();
		
		for(Pair<String> pair : pairs.keySet()){
			map.put(getKey(pair), pair);
		}
		
		LinkedHashMap<Pair<String>,Double> scores = new LinkedHashMap<Pair<String>,Double>();
		ArrayList<String> candidateArray = new ArrayList<String>();
		
		String mainWord = null;
		int candidateCount = 0;
		FileReader fileReader = new FileReader(inputFile);
		String key;
		while(fileReader.hasNext()){
			String line = fileReader.next();
			if(line.startsWith("## ")){
				mainWord = line.substring(3);
				candidateCount = 0;
			}
			else{
				if(mainWord == null){
					candidateArray.add(line);
				}
				else{
					key = getKey(candidateArray.get(candidateCount), mainWord);
					if(map.containsKey(key))
						scores.put(map.get(key), Double.parseDouble(line));//Tools.getDouble(line, 0.0));
					candidateCount++;
				}
			}
		}
		fileReader.close();
		return scores;
	}
	
	public static LinkedHashSet<String> getCandidateWords(SemModel semModel, String pos){
		int minFreq = 10;
		LinkedHashSet<String> candidateWords = new LinkedHashSet<String>();
		for(String s : semModel.getNodeIndex().getIdMap().keySet()){
			if(semModel.getNodeCount(s) >= minFreq && s.toLowerCase().endsWith("_" + pos.toLowerCase())){
				candidateWords.add(s);
			}
		}
		return candidateWords;
	}
	
	/**
	 * Create the feature file for SVM training and testing.
	 * If cachePath is specified, it reads similarity scores from the corresponding cache file instead.
	 * The file is generated iteratively, one feature at a time, since all the cache files don't fit into memory at once.
	 * If cachePath is null, the scores are calculated on the fly.
	 */
	public static void createSVMFile(LinkedHashMap<Pair<String>,Integer> goldPairs, String outputFile, String pos, VectorSpace vectorSpace, SemModel semModel, String cachePath){
		String line;
		Double score;
		LinkedHashMap<Integer,Double> vector1 = null, vector2 = null, cacheVector2 = null;
		String cacheNode = null;
		//LinkedHashMap<String,LinkedHashMap<String,Double>> cachedScores = null;
		LinkedHashMap<Pair<String>,Double> cachedScores = null;
		HashSet<Integer> common = new HashSet<Integer>();
		
		
		
		// First printing the answers
		FileWriter fileWriter = new FileWriter(outputFile);
		for(Pair<String> wordPair : goldPairs.keySet()){
			fileWriter.write("" + (goldPairs.get(wordPair).equals(1)?1:-1));
			/*
			vector1 = vectorSpace.getVector(wordPair.getItem1());
			if(cacheVector2 != null && cacheNode != null && cacheNode.equals(wordPair.getItem2()))
				vector2 = cacheVector2;
			else{
				vector2 = vectorSpace.getVector(wordPair.getItem2());
				cacheVector2 = vector2;
			} 
			*/
			vector1 = vectorSpace.getVector(wordPair.getItem1());
			vector2 = vectorSpace.getVector(wordPair.getItem2()); 
			
			// General features
			
			fileWriter.write(" " + getFeatureId("freq1")+":"+semModel.getNodeCount(wordPair.getItem1()));
			fileWriter.write(" " + getFeatureId("freq2")+":"+semModel.getNodeCount(wordPair.getItem2()));
			fileWriter.write(" " + getFeatureId("freqX")+":"+((double)semModel.getNodeCount(wordPair.getItem1())*(double)semModel.getNodeCount(wordPair.getItem2())));
			
			fileWriter.write(" " + getFeatureId("feat1")+":"+vector1.size());
			fileWriter.write(" " + getFeatureId("feat2")+":"+vector2.size());
			fileWriter.write(" " + getFeatureId("featX")+":"+((double)vector1.size() * (double)vector2.size()));
			
			double ratio1 = (double)vector1.size() / (double)semModel.getNodeCount(wordPair.getItem1());
			double ratio2 = (double)vector2.size() / (double)semModel.getNodeCount(wordPair.getItem2());
			
			fileWriter.write(" " + getFeatureId("ratio1")+":"+ratio1);
			fileWriter.write(" " + getFeatureId("ratio2")+":"+ratio2);
			fileWriter.write(" " + getFeatureId("ratioX")+":"+(ratio1*ratio2));
			
			common.clear();
			common.addAll(vector1.keySet());
			common.retainAll(vector2.keySet());
			double common1 = (vector1.size() == 0)?0.0:((double)common.size() / (double)vector1.size());
			double common2 = (vector2.size() == 0)?0.0:((double)common.size() / (double)vector2.size());
			fileWriter.write(" " + getFeatureId("common0")+":"+common.size());
			fileWriter.write(" " + getFeatureId("common1")+":"+common1);
			fileWriter.write(" " + getFeatureId("common2")+":"+common2);
			fileWriter.write(" " + getFeatureId("commonX")+":"+(common1 * common2));
			
			fileWriter.writeln("");
		}
		fileWriter.close();
		
		
		// Now printing all the features, iteratively
		for(SimMeasure simMeasure : measures){ 
			System.out.println("Adding measure: " + simMeasure.getLabel());
			
			Tools.runCommand("rm " + outputFile +".temp");
			Tools.runCommand("mv " + outputFile + " " + outputFile +".temp");
			
			if(cachePath != null){
				cachedScores = loadCachedScores(cachePath, simMeasure.getLabel(), goldPairs);
			}
			else
				cachedScores = null;
			
			FileReader input = new FileReader(outputFile + ".temp");
			FileWriter output = new FileWriter(outputFile);
			
			for(Pair<String> wordPair : goldPairs.keySet()){
				if(!input.hasNext())
					throw new RuntimeException("No lines left in input file");
				line = input.next();
				
				if(cachedScores != null){
					score = cachedScores.get(wordPair);
				}
				else {
					vector1 = vectorSpace.getVector(wordPair.getItem1());
					vector2 = vectorSpace.getVector(wordPair.getItem2());
					score = simMeasure.sim(vector1, vector2);
				}
				
				if(score == null || score.isInfinite() || score.isNaN())
					throw new RuntimeException("Illegal score value: " + score);

				Integer featureId = getFeatureId(simMeasure.getLabel());
				if(featureId == null)
					throw new RuntimeException("Feature ID cannot be null");

				output.writeln(line + " " + featureId + ":" + score);
			}
			
			if(cachedScores != null){
				cachedScores.clear();
				cachedScores = null;
				//System.gc();
			}
			input.close();
			output.close();
		}
		Tools.runCommand("rm " + outputFile +".temp");
	}
	
	public static LinkedHashMap<Pair<String>,Integer> readGenAsPairs(String path, boolean balanced, SemModel semModel, String pos){
		LinkedHashMap<String,LinkedHashSet<String>> genGold = HyponymGeneration.readGold(path, pos);
		LinkedHashSet<String> candidateWords = getCandidateWords(semModel, pos);
		LinkedHashMap<Pair<String>,Integer> genPairs = new LinkedHashMap<Pair<String>,Integer>();

		// Constructing the pairs
		
		if(balanced){
			ArrayList<String> candidateWordArray = new ArrayList<String>(candidateWords);
			HashSet<String> chosen = new HashSet<String>();
			Collections.shuffle(candidateWordArray);
			
			Random random = new Random(1011833826);
			for(String mainWord : genGold.keySet()){
				chosen.clear();
				int count = 0;
				for(String goldWord : genGold.get(mainWord)){
					if(candidateWords.contains(goldWord)){
						count++;
						genPairs.put(new Pair<String>(goldWord, mainWord), 1);
					}
				}
				
				while(count > 0){
					int r = random.nextInt(candidateWordArray.size());
					String s = candidateWordArray.get(r);
					if(genGold.get(mainWord).contains(s) || chosen.contains(s))
						continue;
					chosen.add(s);
					genPairs.put(new Pair<String>(s, mainWord), 0);
					count--;
				}
			}
		}
		else{
			for(String mainWord : genGold.keySet()){
				for(String candidateWord : candidateWords){
					if(genGold.get(mainWord).contains(candidateWord))
						genPairs.put(new Pair<String>(candidateWord, mainWord), 1);
					else
						genPairs.put(new Pair<String>(candidateWord, mainWord), 0);
				}
			}
		}
		
		return genPairs;
	}
	
	public static LinkedHashMap<Pair<String>,Integer> readGenAsPairsSub(String path, SemModel semModel, String pos, int sampleSize, int sampleId){
		LinkedHashMap<String,LinkedHashSet<String>> genGold = HyponymGeneration.readGold(path, pos);
		LinkedHashSet<String> candidateWords = getCandidateWords(semModel, pos);
		LinkedHashMap<Pair<String>,Integer> genPairs = new LinkedHashMap<Pair<String>,Integer>();
		
		long count = 0;
		for(String mainWord : genGold.keySet()){
			for(String candidateWord : candidateWords){
				if(count >= sampleSize * sampleId && count < sampleSize * (sampleId+1)){
					if(genGold.get(mainWord).contains(candidateWord))
						genPairs.put(new Pair<String>(candidateWord, mainWord), 1);
					else
						genPairs.put(new Pair<String>(candidateWord, mainWord), 0);
				}
				count++;
			}
		}
		
		return genPairs;
	}
		
	/*
	public static LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,Double>>> loadCachedScores(String cachePrefix){
		LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,Double>>> cachedScores = new LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,Double>>>();
		for(SimMeasure simMeasure : measures){
			String file = cachePrefix + "-"+simMeasure.getLabel()+".list.gz";
			cachedScores.put(simMeasure.getLabel(), readScores(file));
		}
		return cachedScores;
	}
	*/
	public static LinkedHashMap<Pair<String>,Double> loadCachedScores(String cachePrefix, String measureLabel, LinkedHashMap<Pair<String>,Integer> pairs){
		String file = cachePrefix + "-"+ measureLabel+".list.gz";
		return readScores(file, pairs);
	}
	
	public static LinkedHashMap<Pair<String>,Double> readOutputDet(String inputFile, LinkedHashMap<Pair<String>,Integer> goldPairs){
		LinkedHashMap<Pair<String>,Double> scoredPairs = new LinkedHashMap<Pair<String>,Double>();
		
		FileReader fileReader = new FileReader(inputFile);
		for(Pair<String> pair : goldPairs.keySet()){
			if(!fileReader.hasNext())
				throw new RuntimeException("Input file has run out");
			String line = fileReader.next().trim();
			if(line.length() == 0)
				throw new RuntimeException("Empty line");
			Double score = Double.parseDouble(line);
			scoredPairs.put(pair, score); 
		}
		if(scoredPairs.size() != goldPairs.size() || fileReader.hasNext())
			throw new RuntimeException("Scoredpairs size does not match goldpairs size: " + scoredPairs.size() + " " + goldPairs.size() + " " + fileReader.hasNext());
		fileReader.close();
		return scoredPairs;
	}
	
	public static LinkedHashMap<String,LinkedHashMap<String,Double>> readOutputGen(String inputFile, LinkedHashMap<String,LinkedHashSet<String>> genGold, String pos, SemModel semModel){
		LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = new LinkedHashMap<String,LinkedHashMap<String,Double>>();
		LinkedHashSet<String> candidateWords = getCandidateWords(semModel, pos);
		
		FileReader fileReader = new FileReader(inputFile);
		long nanCount = 0;
		for(String mainWord : genGold.keySet()){
			predictions.put(mainWord, new LinkedHashMap<String,Double>());
			for(String candidateWord : candidateWords){
				if(!fileReader.hasNext())
					throw new RuntimeException("Input file has run out");
				String line = fileReader.next().trim();
				if(line.length() == 0)
					throw new RuntimeException("Empty line");
				
				if(line.equals("nan")){
					predictions.get(mainWord).put(candidateWord, 0.0);
					nanCount++;
				}
				else{
					Double score = Double.parseDouble(line);
					predictions.get(mainWord).put(candidateWord, score);
				}
			}
		}
		int c = 0;
		while(fileReader.hasNext()){
			c++;
			fileReader.next();
		}
		if(c > 0) 
			throw new RuntimeException("Error: Outputfile had " + c + " more lines.");
		
		fileReader.close();
		if(nanCount > 0)
			System.out.println("Warning: NANCount was " + nanCount);
		return predictions;
	}
	
	public static void runSVMInParallel(String svmCommand, String testFile, String modelFile, String predictionsFile, String logFile, int chunks, int numThreads){
		if(chunks > 10)
			throw new RuntimeException("This code needs to be modifed for chunks > 10. 'split' suffixes won't be read correctly.");
		
		// Let's split the testFile
		Tools.runCommand("y=`wc -l < "+testFile+" | awk '{ print 1+int($1/"+chunks+") }'`;split -d -a 1 -l $y "+testFile+" "+testFile+".");
		
		// Creating the commands for svm
		ArrayList<String> commands = new ArrayList<String>();
		for(int i = 0; i < chunks; i++){
			commands.add(svmCommand + " " + testFile + "." + i + " " + modelFile + " " + predictionsFile + "." + i + " > " + logFile + "." + i);
		}
		SystemTask.run(commands, numThreads);
		
		// Collecting output
		Tools.runCommand("rm " + predictionsFile);
		Tools.runCommand("rm " + logFile);
		for(int i = 0; i < chunks; i++){
			Tools.runCommand("cat " + predictionsFile + "." + i + " >> " + predictionsFile);
			Tools.runCommand("cat " + logFile + "." + i + " >> " + logFile);
			Tools.runCommand("rm " + predictionsFile + "." + i);
			Tools.runCommand("rm " + logFile + "." + i);
			Tools.runCommand("rm " + testFile + "." + i);
		}
	}
	
	public static double run(String pos, String trainSet, String testSet, String trainMode, String testMode) {
		
		System.out.println("######## RUNNING EXPERIMENT");
		System.out.println("POS: " + pos + ", TRAINSET: " + trainSet + ", TESTSET: " + testSet + ", TRAINMODE: " + trainMode + ", TESTMODE: " + testMode);

		
		// aki
		String prefix = "/local/scratch-4/hy260/tmp/";
		String svmPath = "~/apps/svm_light_aki/";
		int numThreads = 4; 
		/*
		// maun
		String prefix = "/local/scratch/mr472/tmp/"; 
		String svmPath = "~/apps/svm_light/";
		int numThreads = 8;
		*/
		
		
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/model1"; 

		SemModel semModel = new SemModel(modelPath, true, false);
		semModel.makeTensorSymmetric();
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		int normalisationType = 1; 
		int kernel = 0;
		
		String trainFile = null, testFile = null;
		
		// TRAINING 
		
		// ------ Creating the training file
		if(trainMode.equals("det") && !trainSet.equals("none")){
			System.out.println("Creating the training file (det)...");
			trainFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-detection-" + pos + "-" + trainSet + ".txt";
			LinkedHashMap<Pair<String>,Integer> goldTrain = HyponymDetection.readGold(trainFile, pos);
			createSVMFile(goldTrain, prefix + "train", pos, vectorSpace, semModel, null);
		}
		else if(trainMode.startsWith("gen") && !trainSet.equals("none")){ 
			System.out.println("Creating the training file (gen)...");
			trainFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-"+pos+"-"+trainSet+".txt";
			LinkedHashMap<Pair<String>,Integer> goldTrain = readGenAsPairs(trainFile, true, semModel, pos);
			String cachePref = "/anfs/bigdisc/mr472/condor/hypgen/extra/" + pos + "-" + trainSet;
			createSVMFile(goldTrain, prefix + "train", pos, vectorSpace, semModel, cachePref);
		}
		
		// FEATURE NORMALISATION AND TRAINING
		if(!trainSet.equals("none")){
			// -------- Feature normalisation
			System.out.println("Feature normalisation...");
			Tools.runCommand("mv " + prefix + "train " + prefix + "train_orig");
			FeatureNormaliser.run(normalisationType, prefix + "train_orig", prefix + "train_orig", prefix + "train");
			
			// -------- Running SVM training
			System.out.println("Running SVM training...");
			Tools.runCommand(svmPath+"/svm_learn -t " + kernel + " " + prefix + "/train " + prefix + "/model > " + prefix + "/train.log");
			System.out.println("CMD: " +svmPath+"/svm_learn -t 2 " + prefix + "/train " + prefix + "/model > " + prefix + "/train.log");
		}

		// PREDICTION
		LinkedHashMap<Pair<String>,Integer> goldTest;
		String cachePath;
		if(testMode.equals("det")){
			System.out.println("Running testing (det)...");
			testFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-detection-"+pos+"-"+testSet+".txt";
			goldTest = HyponymDetection.readGold(testFile, pos);
			cachePath = null;
			
			// Creating the SVM File
			System.out.println("Creating the testing file...");
			createSVMFile(goldTest, prefix + "test", pos, vectorSpace, semModel, cachePath);
			 
			// -------- Feature normalisation
			System.out.println("Feature normalisation...");
			Tools.runCommand("mv " + prefix + "test " + prefix + "test_orig");
			FeatureNormaliser.run(normalisationType, prefix + "train_orig", prefix + "test_orig", prefix + "test");
			
			// --------- Running SVM prediction
			System.out.println("Running SVM prediction...");
			Tools.runCommand(svmPath+"/svm_classify " + prefix + "/test " + prefix + "/model " + prefix + "/predictions > "+prefix + "/predictions.log");
			System.out.println("Finished SVM prediction");
		}
		else {
			System.out.println("Running testing (gen)...");
			testFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-"+pos+"-"+testSet+".txt";
			//goldTest = readGenAsPairs(testFile, false, semModel, pos);
			cachePath = "/anfs/bigdisc/mr472/condor/hypgen/extra/" + pos + "-" + testSet;
			
			Tools.runCommand("rm " + prefix + "/predictions");
			//LinkedHashMap<Pair<String>,Integer> tempGoldTest = new LinkedHashMap<Pair<String>,Integer>();
			int count = 0, sampleSize = 2000000, sampleId = 0;
			
			while(true){
				goldTest = readGenAsPairsSub(testFile, semModel, pos, sampleSize, sampleId);
				if(goldTest.size() == 0)
					break;
				
				// Creating the SVM File
				System.out.println("Creating the testing file, sample " +(sampleId)+"...");
				createSVMFile(goldTest, prefix + "test."+sampleId, pos, vectorSpace, semModel, cachePath);
				  
				// -------- Feature normalisation
				System.out.println("Feature normalisation...");
				Tools.runCommand("mv " + prefix + "test."+sampleId + " " + prefix + "test_orig."+sampleId);
				FeatureNormaliser.run(normalisationType, prefix + "train_orig", prefix + "test_orig."+sampleId, prefix + "test."+sampleId);
				
				// --------- Running SVM prediction
				System.out.println("Running SVM prediction...");
				//Tools.runCommand(svmPath+"/svm_classify " + prefix + "/test " + prefix + "/model " + prefix + "/predictions."+sampleId +" > "+prefix + "/predictions."+sampleId +".log");
				runSVMInParallel(svmPath+"/svm_classify", prefix + "/test."+sampleId, prefix + "/model", prefix + "/predictions."+sampleId, prefix + "/predictions." + sampleId + ".log", numThreads, numThreads);
				Tools.runCommand("cat "+prefix + "/predictions."+sampleId+" >> " + prefix + "/predictions"); 
				Tools.runCommand("rm "+prefix + "/test."+sampleId);
				Tools.runCommand("rm "+prefix + "/test_orig."+sampleId);
				System.out.println("Finished SVM prediction");
				
				goldTest = null;
				sampleId++;
			}
		}
		
		// --------- Clearing some memory
		vectorSpace = null;
		System.gc();
		
		// --------- Reading in output
		double result = 0.0;
		if(testMode.equals("det")){
			System.out.println("Reading in the output (det)...");
			LinkedHashMap<Pair<String>,Integer> goldDetTest = HyponymDetection.readGold(testFile, pos);
			LinkedHashMap<Pair<String>,Double> scoredPairs = readOutputDet(prefix + "predictions", goldDetTest);
			double ap = HyponymDetection.runEvaluation(false, scoredPairs, goldDetTest);
			result = ap;
			System.out.println("AP: " + ap);
		}
		else if(testMode.equals("gen")){
			System.out.println("Reading in the output (gen)...");
			LinkedHashMap<String,LinkedHashSet<String>> goldGenTest = HyponymGeneration.readGold(testFile, pos);
			LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = readOutputGen(prefix + "predictions", goldGenTest, pos, semModel);
			double map = HyponymGeneration.calculateMAP(false, predictions, goldGenTest);
			result = map;
			System.out.println("MAP: " + map);
		}  
		
		System.out.println("### RESULT");
		System.out.println("POS: " + pos + ", TRAINSET: " + trainSet + ", TESTSET: " + testSet + ", TRAINMODE: " + trainMode + ", TESTMODE: " + testMode);
		System.out.println("" + result);
		System.gc();
		
		return result;
	}

	public static void main(String[] args) {
		measures.addAll(Arrays.asList(	SimMeasure.COSINE,
										SimMeasure.PEARSON,
										SimMeasure.SPEARMAN,
										SimMeasure.JACCARD_SET,
										SimMeasure.LIN,
										SimMeasure.DICE_SET,
										SimMeasure.OVERLAP_SET,
										SimMeasure.COSINE_SET,
										SimMeasure.JACCARD_GEN,
										SimMeasure.DICE_GEN,
										SimMeasure.DICE_GEN_2,
										SimMeasure.KENDALLS_TAU,
										SimMeasure.CLARKE_DE,
										SimMeasure.WEEDS_PREC,
										SimMeasure.WEEDS_REC,
										SimMeasure.WEEDS_F,
										SimMeasure.AP,
										SimMeasure.AP_INC,
										SimMeasure.BAL_AP_INC,
										SimMeasure.LIN_D,
										SimMeasure.BAL_PREC,
										SimMeasure.KL_DIVERGENCE,
										SimMeasure.KL_DIVERGENCE_R,
										SimMeasure.JS_DIVERGENCE,
										SimMeasure.ALPHA_SKEW,
										SimMeasure.ALPHA_SKEW_R,
										SimMeasure.MANHATTAN,
										SimMeasure.EUCLIDEAN,
										SimMeasure.CHEBYSHEV,
										SimMeasure.WEIGHTED_COSINE));
	/*
		// aki
		run("noun", "train", "train", "gen", "gen");
		System.gc();
		run("noun", "train", "train", "det", "gen");
		System.gc();
		*/
		/*
		// maun
		for(String trainMode : Arrays.asList("det", "gen")){
			for(String pos : Arrays.asList("noun", "verb")){
				run(pos, "train", "train", trainMode, "det");
				System.gc();
				run(pos, "none", "dev", trainMode, "det");
				System.gc(); 
				run(pos, "none", "test", trainMode, "det");
				System.gc();
			}
		}
		*/
		/*
		//maun2
		for(String trainMode : Arrays.asList("det", "gen")){
			run("noun", "train", "dev", trainMode, "gen");
			System.gc();
			run("noun", "none", "test", trainMode, "gen");
			System.gc();
			run("verb", "train", "train", trainMode, "gen");
			System.gc();
			run("verb", "none", "dev", trainMode, "gen");
			System.gc();
			run("verb", "none", "test", trainMode, "gen");
			System.gc();
		}
		*/
		
		// aki2
		String trainMode = "gen";
		run("verb", "train", "train", trainMode, "gen");
		System.gc();
		run("verb", "none", "dev", trainMode, "gen");
		System.gc();
		run("verb", "none", "test", trainMode, "gen");
		System.gc();
		run("noun", "train", "test", trainMode, "gen");
		System.gc();
	}
}

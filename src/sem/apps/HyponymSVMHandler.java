package sem.apps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimMeasure;
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
	
	public static void normaliseFeatures(int normType, String referenceFile, String inputFile, String outputFile){
		HashMap<Integer,Double> sums = new HashMap<Integer,Double>();
		HashMap<Integer,Double> squaresums = new HashMap<Integer,Double>();
		HashMap<Integer,Double> max = new HashMap<Integer,Double>();
		HashMap<Integer,Double> min = new HashMap<Integer,Double>();
		HashMap<Integer,Integer> count = new HashMap<Integer,Integer>();
		
		// Collecting statistics from the reference file
		FileReader refReader = new FileReader(referenceFile);
		while(refReader.hasNext()){
			String[] line = refReader.next().trim().split("\\s+");
			if(line.length <= 0)
				continue;
			for(int i = 1; i < line.length; i++){
				String[] f = line[i].trim().split(":");
				if(f.length != 2)
					throw new RuntimeException("Illegal number of chunks");
				Integer id = Integer.parseInt(f[0]);
				Double value = Double.parseDouble(f[1]);
				
				if(!sums.containsKey(id))
					sums.put(id, 0.0);
				sums.put(id, sums.get(id) + value);
				
				if(!squaresums.containsKey(id))
					squaresums.put(id, 0.0);
				squaresums.put(id, squaresums.get(id) + Math.pow(value, 2.0));
				
				if(!max.containsKey(id) || max.get(id) < value)
					max.put(id, value);
				
				if(!min.containsKey(id) || min.get(id) > value)
					min.put(id, value);
				
				if(!count.containsKey(id))
					count.put(id, 0);
				count.put(id, count.get(id) + 1);
			}
		}
		refReader.close();
		
		// Transforming the input file
		FileReader inputReader = new FileReader(inputFile);
		FileWriter outputWriter = new FileWriter(outputFile);
		while(inputReader.hasNext()){
			String[] line = inputReader.next().trim().split("\\s+");
			if(line.length <= 0)
				continue;
			outputWriter.write(line[0]);
			for(int i = 1; i < line.length; i++){
				String[] f = line[i].trim().split(":");
				if(f.length != 2)
					throw new RuntimeException("Illegal number of chunks");
				Integer id = Integer.parseInt(f[0]);
				Double value = Double.parseDouble(f[1]);
				
				if(!count.containsKey(id))
					continue;
				
				if(normType == 0){
					outputWriter.write(" " + id + ":" + value);
				}
				else if(normType == 1){
					if(count.get(id) < 2)
						continue;
					double mean = sums.get(id) / count.get(id);
					double range = max.get(id) - min.get(id);
					if(range == 0.0)
						continue;
					double newValue = (value - mean) / range;
					outputWriter.write(" " + id + ":" + newValue);
				}
				else if(normType == 2){
					if(count.get(id) < 2)
						continue;
					double mean = sums.get(id) / count.get(id);
					double sdev = squaresums.get(id) - (2.0 * mean * sums.get(id)) + (count.get(id) * mean * mean);
					if(sdev == 0.0)
						continue;
					if(sdev < 0.0)
						throw new RuntimeException("Problems");
					sdev = Math.sqrt(sdev / (count.get(id) - 1));
					double newValue = (value - mean) / sdev;
					outputWriter.write(" " + id + ":" + newValue);
				}
			} 
			outputWriter.writeln("");
		}
		inputReader.close();
		outputWriter.close();
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
	/*
	public static void createSVMFile_old(LinkedHashMap<Pair<String>,Integer> goldPairs, String outputFile, String pos, VectorSpace vectorSpace, SemModel semModel){
		FileWriter fileWriter = new FileWriter(outputFile);
		
		LinkedHashMap<Integer,Double> vector1 = null, vector2 = null;
		Integer featureId;
		for(Pair<String> wordPair : goldPairs.keySet()){
			fileWriter.write("" + (goldPairs.get(wordPair).equals(1)?1:-1));
			
			vector1 = vectorSpace.getVector(wordPair.getItem1());
			vector2 = vectorSpace.getVector(wordPair.getItem2());
			
			// General features
			/*
			fileWriter.write(" " + getFeatureId("freq1")+":"+semModel.getNodeCount(wordPair.getItem1()));
			fileWriter.write(" " + getFeatureId("freq2")+":"+semModel.getNodeCount(wordPair.getItem2()));
			fileWriter.write(" " + getFeatureId("freqX")+":"+(semModel.getNodeCount(wordPair.getItem1())*semModel.getNodeCount(wordPair.getItem2())));
			
			fileWriter.write(" " + getFeatureId("feat1")+":"+vector1.size());
			fileWriter.write(" " + getFeatureId("feat2")+":"+vector2.size());
			fileWriter.write(" " + getFeatureId("featX")+":"+(vector1.size() * vector2.size()));
			
			fileWriter.write(" " + getFeatureId("ratio1")+":"+((double)vector1.size() / semModel.getNodeCount(wordPair.getItem1())));
			fileWriter.write(" " + getFeatureId("ratio2")+":"+((double)vector2.size() / semModel.getNodeCount(wordPair.getItem2())));
			fileWriter.write(" " + getFeatureId("ratio1")+":"+(((double)vector1.size() / semModel.getNodeCount(wordPair.getItem1())) * ((double)vector2.size() / semModel.getNodeCount(wordPair.getItem2()))));
			*
			
			// Similarity measure features
			
			for(SimMeasure simMeasure : measures){
				double score = simMeasure.sim(vector1, vector2);
				
				if(Double.isInfinite(score) || Double.isNaN(score))
					throw new RuntimeException("Illegal score value: " + score);
				
				String featureLabel = simMeasure.getLabel();
				featureId = getFeatureId(featureLabel);
				if(featureId == null)
					throw new RuntimeException("Feature ID cannot be null");

				fileWriter.write(" " + featureId + ":" + score);
			}

			fileWriter.writeln("");
		}
		fileWriter.close();
	}
*/
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
			double common1 = (double)common.size() / vector1.size();
			double common2 = (double)common.size() / vector2.size();
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
	
	public static LinkedHashMap<String,LinkedHashMap<String,Double>> readOutputGen(String inputFile, LinkedHashMap<String,LinkedHashSet<String>> genGold, String pos, VectorSpace vectorSpace, SemModel semModel){
		LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = new LinkedHashMap<String,LinkedHashMap<String,Double>>();
		LinkedHashSet<String> candidateWords = getCandidateWords(semModel, pos);
		
		FileReader fileReader = new FileReader(inputFile);
		for(String mainWord : genGold.keySet()){
			predictions.put(mainWord, new LinkedHashMap<String,Double>());
			for(String candidateWord : candidateWords){
				if(!fileReader.hasNext())
					throw new RuntimeException("Input file has run out");
				String line = fileReader.next().trim();
				if(line.length() == 0)
					throw new RuntimeException("Empty line");
				Double score = Double.parseDouble(line);
				predictions.get(mainWord).put(candidateWord, score);
			}
		}
		fileReader.close();
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

		
		//String prefix = "/anfs/bigdisc/mr472/tmp/";
		String prefix = "/local/scratch/mr472/tmp/"; 
		String modelPath = "/anfs/bigdisc/mr472/SemTensor/model1"; 
		SemModel semModel = new SemModel(true, modelPath);
		semModel.makeTensorSymmetric();
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		/*
		String pos = "verb";
		String trainSet = "train";
		String testSet = "test";
		String trainMode = "det";
		String testMode = "det"; 
		*/
		int normalisationType = 1;
		String svmPath = "~/apps/svm_light/";
		
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
		
		if(!trainSet.equals("none")){
			// -------- Feature normalisation
			System.out.println("Feature normalisation...");
			Tools.runCommand("mv " + prefix + "train " + prefix + "train_orig");
			normaliseFeatures(normalisationType, prefix + "train_orig", prefix + "train_orig", prefix + "train");
			
			// -------- Running SVM training
			System.out.println("Running SVM training...");
			Tools.runCommand(svmPath+"/svm_learn -t 2 " + prefix + "/train " + prefix + "/model > " + prefix + "/train.log");
			System.out.println("CMD: " +svmPath+"/svm_learn -t 2 " + prefix + "/train " + prefix + "/model > " + prefix + "/train.log");
		}
		// TESTING
		/*
		if(testMode.equals("det")){
			System.out.println("Creating the testing file (det)...");
			testFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-detection-"+pos+"-"+testSet+".txt";
			LinkedHashMap<Pair<String>,Integer> goldTest = HyponymDetection.readGold(testFile, pos);
			createSVMFile(goldTest, prefix + "test", pos, vectorSpace, semModel, null);
			
			// -------- Feature normalisation
			System.out.println("Feature normalisation...");
			Tools.runCommand("mv " + prefix + "test " + prefix + "test_orig");
			normaliseFeatures(normalisationType, prefix + "train_orig", prefix + "test_orig", prefix + "test");
			
			// --------- Running SVM prediction
			System.out.println("Running SVM prediction...");
			Tools.runCommand("~/apps/svm_light/svm_classify " + prefix + "/test " + prefix + "/model " + prefix + "/predictions > "+prefix + "/predictions.log");
			
		}
		else if(testMode.equals("gen")){
			System.out.println("Creating the testing file (gen)...");
			testFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-"+pos+"-"+testSet+".txt";
			LinkedHashMap<Pair<String>,Integer> goldPairs = readGenAsPairs(testFile, false, semModel, pos);
			//LinkedHashMap<String,LinkedHashSet<String>> goldGenTest = HyponymGeneration.readGold(testFile, pos);
			String cachePref = "/anfs/bigdisc/mr472/condor/hypgen/extra/" + pos + "-" + testSet;
			createSVMFileGen(false, goldGenTest, prefix + "test", pos, vectorSpace, semModel, cachePref);
			
			// -------- Feature normalisation
			System.out.println("Feature normalisation...");
			Tools.runCommand("mv " + prefix + "test " + prefix + "test_orig");
			normaliseFeatures(normalisationType, prefix + "train_orig", prefix + "test_orig", prefix + "test");
			
			// --------- Running SVM prediction
			System.out.println("Running SVM prediction...");
			Tools.runCommand("~/apps/svm_light/svm_classify " + prefix + "/test " + prefix + "/model " + prefix + "/predictions > "+prefix + "/predictions.log");
			
		}
		*/

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
			normaliseFeatures(normalisationType, prefix + "train_orig", prefix + "test_orig", prefix + "test");
			
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
				normaliseFeatures(normalisationType, prefix + "train_orig", prefix + "test_orig."+sampleId, prefix + "test."+sampleId);
				
				// --------- Running SVM prediction
				System.out.println("Running SVM prediction...");
				//Tools.runCommand(svmPath+"/svm_classify " + prefix + "/test " + prefix + "/model " + prefix + "/predictions."+sampleId +" > "+prefix + "/predictions."+sampleId +".log");
				runSVMInParallel(svmPath+"/svm_classify", prefix + "/test."+sampleId, prefix + "/model", prefix + "/predictions."+sampleId, prefix + "/predictions." + sampleId + ".log", 4, 4);
				Tools.runCommand("cat "+prefix + "/predictions."+sampleId+" >> " + prefix + "/predictions"); 
				Tools.runCommand("rm "+prefix + "/test."+sampleId);
				Tools.runCommand("rm "+prefix + "/test_orig."+sampleId);
				System.out.println("Finished SVM prediction");
				
				goldTest = null;
				sampleId++;
			}
		}
		
		/*
		int numThreads = 8;
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 30, TimeUnit.SECONDS, queue);
		LinkedHashMap<Pair<String>,Integer> tempGoldTest = new LinkedHashMap<Pair<String>,Integer>();
		int count = 0, sampleSize = 50000, sampleId = 0;
		for(Entry<Pair<String>,Integer> e : goldTest.entrySet()){
			tempGoldTest.put(e.getKey(), e.getValue());
			count++;
			
			if(count == goldTest.size() || tempGoldTest.size() % sampleSize == 0){
				SVMGeneratorTask svmGeneratorTask = new SVMGeneratorTask(tempGoldTest, sampleId, normalisationType, pos, prefix, cachePath, svmPath, semModel, vectorSpace);
				Future<?> future = executor.submit(svmGeneratorTask);
				futures.add(future);
				sampleId++;
				tempGoldTest = new LinkedHashMap<Pair<String>,Integer>(); 
			}
		}
		
		// Wait to finish
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Combining predictions
		Tools.runCommand("rm " + prefix + "/predictions");
		for(int i = 0; i < sampleId; i++){
			Tools.runCommand("cat " + prefix + "/predictions."+i+" >> " + prefix + "/predictions");
			Tools.runCommand("rm " + prefix + "/predictions."+i);
			Tools.runCommand("rm " + prefix + "/test."+i);
		}
		*/
		
		
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
			LinkedHashMap<String,LinkedHashMap<String,Double>> predictions = readOutputGen(prefix + "predictions", goldGenTest, pos, vectorSpace, semModel);
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
		/*
		measures.addAll(Arrays.asList(	SimMeasure.COSINE,
				SimMeasure.PEARSON,
				SimMeasure.SPEARMAN));
		
		run("noun", "none", "train", "det", "gen");
		
		if(true)
			return;
		*/
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


		for(String trainMode : Arrays.asList("det", "gen")){
			for(String pos : Arrays.asList("noun", "verb")){
				run(pos, "train", "train", trainMode, "gen");
				System.gc();
				run(pos, "none", "dev", trainMode, "gen");
				System.gc();
				run(pos, "none", "test", trainMode, "gen");
				System.gc();
				run(pos, "none", "train", trainMode, "det");
				System.gc();
				run(pos, "none", "dev", trainMode, "det");
				System.gc();
				run(pos, "none", "test", trainMode, "det");
				System.gc(); 
			}
		}
		
	}
	
	public static void tests(){
		long t = System.currentTimeMillis();
		String goldFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-detection-noun-train.txt";
		/*
		String cacheFile1 = "/anfs/bigdisc/mr472/condor/hypgen/extra/noun-train-cosine.list.gz";
		String cacheFile2 = "/anfs/bigdisc/mr472/condor/hypgen/extra/noun-train-kendallsTau.list.gz";
		String testFile = "/anfs/bigdisc/mr472/tmp/test.temp";
		String outputFile = "/anfs/bigdisc/mr472/tmp/test.temp.dummy";
		 */
		
		String cacheFile1 = "/local/scratch/mr472/tmp/noun-train-cosine.list.gz";
		String cacheFile2 = "/local/scratch/mr472/tmp/noun-train-kendallsTau.list.gz";
		String testFile = "/local/scratch/mr472/tmp/test.temp";
		String outputFile = "/local/scratch/mr472/tmp/test.temp.dummy";
		
		LinkedHashMap<Pair<String>,Integer> goldTest = HyponymDetection.readGold(goldFile, "noun");
		LinkedHashMap<Pair<String>,Integer> subGoldTest = new LinkedHashMap<Pair<String>,Integer>();
		
		System.out.println("Reading gold: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		
		int count = 0, limit = 4000000;
		for(Entry<Pair<String>,Integer> e : goldTest.entrySet()){
			subGoldTest.put(e.getKey(), e.getValue());
			count++;
			if(count >= limit)
				break;
		}
		
		System.out.println("Splitting gold: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		
		LinkedHashMap<Pair<String>,Double> cachedScores;
		cachedScores = readScores(cacheFile1, subGoldTest);
		
		System.out.println("Reading cache 1: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		
		cachedScores = null;
		cachedScores = readScores(cacheFile2, subGoldTest);
		
		System.out.println("Reading cache 2: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		
		FileReader input = new FileReader(testFile);
		String line;
		while(input.hasNext()){
			line = input.next();
		}
		input.close();
		
		System.out.println("Reading test: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
		
		FileReader input2 = new FileReader(testFile);
		FileWriter output = new FileWriter(outputFile);
		String line2;
		while(input2.hasNext()){
			line2 = input2.next();
			output.writeln(line2 + " roflmao"); 
		}
		input2.close();
		output.close();
		
		System.out.println("Reading and writing test: " + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
	}
}

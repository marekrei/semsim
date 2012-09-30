package sem.sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sem.model.VectorSpace;
import sem.util.Pair;

class SimGeneratorTask implements Runnable{
	String mainWord;
	LinkedHashSet<String> candidateWords;
	SimMeasure simMeasure;
	boolean findHypernyms;
	VectorSpace vectorSpace;
	LinkedHashMap<String,Double> result;
	
	
	/**
	 * Create a new task for finding most similar items.
	 * @param mainWord
	 * @param candidateWords
	 * @param simMeasureType
	 * @param findHypernyms Value FALSE will put the mainWord in the second argument position of the similarity measure, and find hyponyms. Setting it to TRUE will put mainWord in the first argument position and find hypernyms. This only affects non-symmetric measures.
	 * @param vectorSpace
	 * @param result
	 */
	public SimGeneratorTask(String mainWord, LinkedHashSet<String> candidateWords, SimMeasure simMeasure, boolean findHypernyms, VectorSpace vectorSpace, LinkedHashMap<String,Double> result){
		this.mainWord = mainWord;
		this.candidateWords = candidateWords;
		this.simMeasure = simMeasure;
		this.findHypernyms = findHypernyms;
		this.vectorSpace = vectorSpace;
		this.result = result;
	}

	@Override
	public void run() { 
		LinkedHashMap<Integer,Double> mainVector = vectorSpace.getVector(mainWord);
		for(String candidateWord : candidateWords){
			double val;
			if(!this.findHypernyms)
				val = this.simMeasure.sim(vectorSpace.getVector(candidateWord), mainVector);
			else
				val = this.simMeasure.sim(mainVector, vectorSpace.getVector(candidateWord));

			result.put(candidateWord, val);
		}
	}
}

/**
 * A class that can perform various tasks related to finding similarities or most similar items.
 * For example, you can get the (directional) similarity between two items for hyponym detection. Or you can specify main words and candidate words, and run hyponym generation. In this case, work on different main words can be distributed to multiple cores.
 */
public class SimFinder {
	private VectorSpace vectorSpace;
	
	public SimFinder(VectorSpace vectorSpace){
		this.vectorSpace = vectorSpace;
	}
	
	/**
	 * Get the similarity between two items, using the specified similarity measure.
	 * @param simMeasureType
	 * @param label1
	 * @param label2
	 * @return
	 */
	public double getScore(SimMeasure simMeasure, String label1, String label2){
		LinkedHashMap<Integer,Double> vector1 = vectorSpace.getVector(label1);
		LinkedHashMap<Integer,Double> vector2 = vectorSpace.getVector(label2);
		
		double score = simMeasure.sim(vector1, vector2);
		if(Double.isInfinite(score) || Double.isNaN(score))
			throw new RuntimeException("Illegal score value: " + score);
		
		return score;
	}
	
	/**
	 * Get the similarity for pairs of items, using the specified similarity measure.
	 * @param simMeasureType
	 * @param pairs
	 * @return
	 */
	public LinkedHashMap<Pair<String>,Double> getScores(SimMeasure simMeasure, ArrayList<Pair<String>> pairs){
		LinkedHashMap<Pair<String>,Double> scores = new LinkedHashMap<Pair<String>,Double>();
		for(Pair<String> pair : pairs){
			scores.put(pair, getScore(simMeasure, pair.getItem1(), pair.getItem2()));
		}
		return scores;
	}
	
	/**
	 * Find the all the similarity scores for the cartesian product of "main words" and "candidate words".
	 * For example, if we want to find hyponyms for "liquid", we would put the word "liquid" in the set of main words, and a large number of other words into the candidate set.
	 * This function then find the similarity scores between "liquid" and all the other words.
	 * If several main words are specified together, and numThreads is set to > 1, each main word is parallelized to a different thread.
	 * The output is scored but unsorted.
	 * @param mainWords
	 * @param candidateWords
	 * @param simMeasureType
	 * @param findHypernyms Value FALSE will put the mainWord in the second argument position of the similarity measure, and find hyponyms. Setting it to TRUE will put mainWord in the first argument position and find hypernyms. This only affects non-symmetric measures.
	 * @param numThreads
	 * @return
	 */
	public LinkedHashMap<String,LinkedHashMap<String,Double>> getScores(LinkedHashSet<String> mainWords, LinkedHashSet<String> candidateWords, SimMeasure simMeasure, boolean findHypernyms, int numThreads){
		LinkedHashMap<String,LinkedHashMap<String,Double>> results = new LinkedHashMap<String,LinkedHashMap<String,Double>>();
		
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 30, TimeUnit.SECONDS, queue);
		
		for(String mainWord : mainWords){
			LinkedHashMap<String,Double> result = new LinkedHashMap<String,Double>();
			results.put(mainWord, result);
			SimGeneratorTask task = new SimGeneratorTask(mainWord, candidateWords, simMeasure, findHypernyms, vectorSpace, result);
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
		
		return results;
	}
	
	/**
	 * A simplified function for finding the similarities for only one main words.
	 * @param mainWord
	 * @param candidateWords
	 * @param simMeasureType
	 * @param findHypernyms Value FALSE will put the mainWord in the second argument position of the similarity measure, and find hyponyms. Setting it to TRUE will put mainWord in the first argument position and find hypernyms. This only affects non-symmetric measures.
	 * @return
	 */
	public LinkedHashMap<String,Double> getScores(String mainWord, LinkedHashSet<String> candidateWords, SimMeasure simMeasure, boolean findHypernyms){
		LinkedHashSet<String> mainWords = new LinkedHashSet<String>();
		mainWords.add(mainWord);
		
		LinkedHashMap<String,LinkedHashMap<String,Double>> results = getScores(mainWords, candidateWords, simMeasure, findHypernyms, 1);
		return results.get(mainWord);
	}
}

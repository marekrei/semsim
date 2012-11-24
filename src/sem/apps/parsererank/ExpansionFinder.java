package sem.apps.parsererank;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimFinder;
import sem.sim.SimMeasure;
import sem.util.StringMap;
import sem.util.Tools;
import sem.util.FileReader;

/**
 * A class for generating expansion terms/hyponyms
 *
 */
public class ExpansionFinder {

	public static String generalPos(String pos){
		if(pos.equals("NOPOS"))
			return pos;
		else if(pos.length() > 1)
			return pos.substring(0, 1);
		else
			return pos;
	}

	
	public static void run(String simMeasureType, boolean findHypernyms, int wordLimit, int threadLimit, String semModelPath, String lemmaMapPath, String existingPath){
		int minCandidateCount = 100;
		
		SemModel semModel = new SemModel(semModelPath, false); 
		semModel.makeTensorSymmetric();
		StringMap lemmaMap = new StringMap(lemmaMapPath);
		
		//
		// Loading the existing file
		//
		LinkedHashMap<String,LinkedHashMap<String,Double>> existing = new LinkedHashMap<String,LinkedHashMap<String,Double>>();
		if(existingPath != null && (new File(existingPath)).exists()){
			FileReader fr = new FileReader(existingPath);
			String[] lineBits;
			while(fr.hasNext()){
				lineBits = fr.next().trim().split("\\s+");
				if(lineBits.length >= 1){
					if(lineBits.length % 2 == 1){
						existing.put(lineBits[0], new LinkedHashMap<String,Double>());
						for(int i = 1; i < lineBits.length; i+=2){
							existing.get(lineBits[0]).put(lineBits[i], Tools.getDouble(lineBits[i+1], 0.0));
						}
					}
					else
						throw new RuntimeException("Problem with the existing file");
				}
			}
			fr.close();
		}
		
		//
		// Printing the existing words
		//
		
		for(String mainWord : existing.keySet()){
			System.out.print(mainWord);
			int count = 0;
			for(Entry<String,Double> e : Tools.sort(existing.get(mainWord), true).entrySet()){
				System.out.print("\t" + e.getKey() + "\t" + e.getValue());
				if(++count >= wordLimit)
					break;
			}
			System.out.println();
		}
		
		//
		// Choosing main lemmas
		//
		
		HashSet<String> mainLemmas = new HashSet<String>();
		ArrayList<String> stopWords = new ArrayList<String>(Arrays.asList("a","able","about","across","after","all","almost","also","am","among","an","and","any","are","as","at","be","because","been","but","by","can","cannot","could","dear","did","do","does","either","else","ever","every","for","from","get","got","had","has","have","he","her","hers","him","his","how","however","i","if","in","into","is","it","its","just","least","let","like","likely","may","me","might","most","must","my","neither","no","nor","not","of","off","often","on","only","or","other","our","own","rather","said","say","says","she","should","since","so","some","than","that","the","their","them","then","there","these","they","this","tis","to","too","twas","us","wants","was","we","were","what","when","where","which","while","who","whom","why","will","with","would","yet","you","your"));
		for(String lemma : lemmaMap.values()){
			lemma = lemma.toLowerCase();
			if(!stopWords.contains(lemma))
				mainLemmas.add(lemma);
		}
		
		//
		// Mapping main lemmas to words in the model
		//
		
		LinkedHashMap<String,LinkedHashSet<String>> mainWords = new LinkedHashMap<String,LinkedHashSet<String>>();
		String lemma, pos, gpos;
		for(Entry<String,Integer> e : semModel.getNodeIndex().getIdMap().entrySet()){
			pos = e.getKey().substring(e.getKey().lastIndexOf('_')+1);
			lemma = e.getKey().substring(0, e.getKey().lastIndexOf('_')).toLowerCase();
			gpos = generalPos(pos);
			if(!stopWords.contains(lemma) && mainLemmas.contains(lemma) && !existing.containsKey(e.getKey())){
				if(!mainWords.containsKey(gpos))
					mainWords.put(gpos, new LinkedHashSet<String>());
				mainWords.get(gpos).add(e.getKey());
			}
		}
		
		//
		// Choosing candidate ids
		//
		
		LinkedHashMap<String,LinkedHashSet<String>> candidateWords = new LinkedHashMap<String,LinkedHashSet<String>>();
		for(Entry<String,Integer> e : semModel.getNodeIndex().getIdMap().entrySet()){
			lemma = e.getKey().substring(0, e.getKey().lastIndexOf('_')).toLowerCase();
			pos = e.getKey().substring(e.getKey().lastIndexOf('_')+1);
			gpos = generalPos(pos);
			
			if(!stopWords.contains(lemma) 
					&& (semModel.getNodeIndex().getCount(e.getValue()) >= minCandidateCount 
							//|| (mainIds.containsKey(gpos) && mainIds.get(gpos).contains(e.getValue()))
					)){
				if(!candidateWords.containsKey(gpos))
					candidateWords.put(gpos, new LinkedHashSet<String>());
				candidateWords.get(gpos).add(e.getKey());
			}
		} 

		//
		// Finding similar words
		//
		
		VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
		SimFinder simFinder = new SimFinder(vectorSpace);
		SimMeasure simMeasure = SimMeasure.getType(simMeasureType);
		int count = 0;
		
		for(String p : mainWords.keySet()){
			LinkedHashMap<String,LinkedHashMap<String,Double>> scores = simFinder.getScores(mainWords.get(p), candidateWords.get(p), simMeasure, findHypernyms, threadLimit);
			
			for(String mainWord : mainWords.get(p)){
				System.out.print(mainWord);
				count = 0;
				for(Entry<String,Double> e : Tools.sort(scores.get(mainWord), true).entrySet()){
					System.out.print("\t" + e.getKey() + "\t" + e.getValue());
					if(++count >= wordLimit)
						break;
				}
				System.out.println();
			}
			
			scores = null;
			System.gc();
		}
	}
	
	public static void main(String[] args){
		if(args.length == 6 || args.length == 7){
			
			String simMeasureType = args[0];
			boolean findHypernyms = args[1].equalsIgnoreCase("true")?true:false;
			int wordLimit = Tools.getInt(args[2], -1);
			int threadLimit = Tools.getInt(args[3], 1);
			String semModelPath = args[4];
			String lemmaMapPath = args[5];
			String existingPath = null;
			if(args.length >= 7)
				existingPath = args[6];
			
			run(simMeasureType, findHypernyms, wordLimit, threadLimit, semModelPath, lemmaMapPath, existingPath);
			System.gc();
			
		}
		else{
			System.out.println("Usage: <similarity measure> <findhypernyms (true/false)> <word limit> <thread limit> <SemSim path> <lemma map path> [existing file]");
			System.out.println("Outputs similar words for each word");
		}
	}

}

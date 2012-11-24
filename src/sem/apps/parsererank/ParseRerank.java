package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import sem.exception.GraphFormatException;
import sem.graph.Graph;
import sem.graph.Node;
import sem.grapheditor.AddNodesGraphEditor;
import sem.grapheditor.BypassAdpGraphEditor;
import sem.grapheditor.BypassConjGraphEditor;
import sem.grapheditor.CombineSubjObjGraphEditor;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LemmatiserGraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.graphreader.ParsevalGraphReader;
import sem.graphreader.RaspXmlGraphReader;
import sem.model.SemModel;
import sem.util.Tools;

/**
 * The main class for running experiments with parse reranking
 *
 */
public class ParseRerank {
	private static void log(String str){
		System.out.println(str);
	}

	public static LinkedHashMap<Graph,Double> filterDuplicates(LinkedHashMap<Graph,Double> graphs, boolean adjustScores){
		LinkedHashMap<Graph,Double> newGraphs = new LinkedHashMap<Graph,Double>();
		HashSet<String> hashes = new HashSet<String>();
		double score = graphs.size();
		for(Entry<Graph,Double> e : Tools.sort(graphs, true).entrySet()){
			String hash = Canonicaliser.getString(e.getKey());
			if(!hashes.contains(hash)){
				if(adjustScores)
					newGraphs.put(e.getKey(), score);
				else
					newGraphs.put(e.getKey(), e.getValue());
				hashes.add(hash);
				score--;
			}
		}
		return newGraphs;
	}
	
	public static ArrayList<LinkedHashMap<Graph,Double>> filterDuplicates(ArrayList<LinkedHashMap<Graph,Double>> sentences, boolean adjustScores){
		ArrayList<LinkedHashMap<Graph,Double>> newSentences = new ArrayList<LinkedHashMap<Graph,Double>>();
		for(LinkedHashMap<Graph,Double> map : sentences)
			newSentences.add(filterDuplicates(map, adjustScores));
		return newSentences;
	}
	
	public static ArrayList<LinkedHashMap<Graph,Double>> loadOriginalGraphs(String inputPath, boolean filterDuplicates){
		log("# Reading in the original parses...");
		ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		int graphCount;
		
		try {
			RaspXmlGraphReader inputReader = new RaspXmlGraphReader(inputPath, RaspXmlGraphReader.NODES_TOKENS, true, false);
			graphCount = 0;
			while(inputReader.hasNext()){
				ArrayList<Graph> sentence = inputReader.nextSentence();
				
				LinkedHashMap<Graph,Double> graphMap = new LinkedHashMap<Graph,Double>();
				double score = sentence.size();
				for(Graph graph : sentence){
					graphMap.put(graph, score);
					score -= 1.0;
				}
				originalGraphs.add(graphMap);
				graphCount += sentence.size();
			}
			log("Read in " + originalGraphs.size() + " sentences, " + graphCount + " graphs.");
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		
		//
		// Filtering duplicates
		//
		
		if(filterDuplicates){
			log("# Filtering duplicates...");
			ArrayList<LinkedHashMap<Graph,Double>> filteredGraphs = filterDuplicates(originalGraphs, true);
			graphCount = 0;
			for(LinkedHashMap<Graph,Double> sentence : filteredGraphs)
				graphCount += sentence.size();
			System.out.println("Ended up with " + filteredGraphs.size() + " sentences, " + graphCount + " graphs.");
			originalGraphs = filteredGraphs;
		}
		return originalGraphs;
	}
	
	public static ArrayList<Graph> loadGoldGraphs(String goldPath){
		log("# Reading in gold standard...");
		ArrayList<Graph> goldGraphs = new ArrayList<Graph>();
		try {
			ParsevalGraphReader goldReader = new ParsevalGraphReader(goldPath, false, true);
			while(goldReader.hasNext()){
				// This is to match the gold standard, which has semicolons escaped for some reason
				Graph g = goldReader.next();
				for(Node node : g.getNodes()){
					if(node.getLemma().equals("\\;"))
						node.setLemma(";");
				}
				goldGraphs.add(g);
			}
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		log("Read in " + goldGraphs.size() + " graphs / sentences.");
		return goldGraphs;
	}
	
	public static ArrayList<LinkedHashMap<Graph,Double>> runRescoring(ArrayList<LinkedHashMap<Graph,Double>> originalGraphs, String edgeScorerType, int parseScorerMethod, ArrayList<GraphEditor> graphEditors, SemModel semModel, String expansionMapPath, int expansionLimit, int numThreads){
		
		//
		// Creating projections
		//
		
		System.out.println("# Creating projected graphs...");
		ArrayList<LinkedHashMap<Graph,Double>> projectedGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		LinkedHashMap<Graph,Graph> projectionMap = new LinkedHashMap<Graph,Graph>();
		
		for(LinkedHashMap<Graph,Double> sentence : originalGraphs){
			LinkedHashMap<Graph,Double> sentence2 = new LinkedHashMap<Graph,Double>();
			for(Entry<Graph,Double> e : sentence.entrySet()){
				Graph projectedGraph = e.getKey().clone();
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(projectedGraph);
				sentence2.put(projectedGraph, e.getValue());
				projectionMap.put(projectedGraph, e.getKey());
			}
			projectedGraphs.add(sentence2);
		}

		//
		// Rescoring the projections
		//
		
		System.out.println("# Rescoring the projections...");
		ArrayList<LinkedHashMap<Graph,Double>> scoredProjectedGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		GraphScorer parseScorer = new GraphScorer(numThreads);
		EdgeScorer edgeScorer = new EdgeScorer(edgeScorerType, semModel, expansionMapPath, expansionLimit);
		for(LinkedHashMap<Graph,Double> sentence : projectedGraphs){
			LinkedHashMap<Graph,Double> scoredSentence = new LinkedHashMap<Graph,Double>();
			scoredProjectedGraphs.add(scoredSentence);
			parseScorer.submitTask(sentence, scoredSentence, edgeScorer, parseScorerMethod);
		}
		parseScorer.waitToFinish();
		parseScorer.stopThreadPool();
		
		
		//
		// Mapping projections back to the original graphs 
		//
		
		System.out.println("# Mapping scored projections to original graphs...");
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		for(LinkedHashMap<Graph,Double> sentence : scoredProjectedGraphs){
			LinkedHashMap<Graph,Double> newSentence = new LinkedHashMap<Graph,Double>();
			for(Entry<Graph,Double> e : Tools.sort(sentence, true).entrySet()){
				newSentence.put(projectionMap.get(e.getKey()), e.getValue());
			}
			scoredGraphs.add(newSentence);
		}
		
		return scoredGraphs;
	}
	
	public static String formatResults(LinkedHashMap<String,Double> results){
		String strResult = "";
		for(Entry<String,Double> result : results.entrySet())
			strResult += result.getKey() + "\t";
		strResult += "\n";
		for(Entry<String,Double> result : results.entrySet())
			strResult += result.getValue() + "\t";
		return strResult;
	}
	
	public static LinkedHashMap<String,Double> run(SemModel semModel, String datasetPath, String expansionListPath, String posMapPath, String edgeScorerType, int graphScorerType, int numThreads){
		// Making sure that all the exceptions are caught and the process is halted
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
		        e.printStackTrace();
		        System.exit(1);
		    }
		});
		
		String inputPath = datasetPath + "/parsed.xml";
		String goldPath = datasetPath + "/gold.rasp";
		String lemmaMapPath = datasetPath + "/lemmas.map";
		int edgeMatchType = ParseEvaluator.MATCH_HIERARCHICAL;
		int expansionLimit = 10;
		
		ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				new LemmatiserGraphEditor(lemmaMapPath),
				//new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_NONE),
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor(),
				new BypassConjGraphEditor(posMapPath),
				new BypassAdpGraphEditor(3, posMapPath),
				new CombineSubjObjGraphEditor(4, posMapPath),
				new AddNodesGraphEditor(4)//,
				//new ReverseEdgesGraphEditor(),
				//new NullEdgesGraphEditor()
				));
		
		
		ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = loadOriginalGraphs(inputPath, true);
		ArrayList<Graph> goldGraphs = loadGoldGraphs(goldPath);
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs = null;
		
		if(edgeScorerType.equalsIgnoreCase("BASELINE")){
			scoredGraphs = originalGraphs;
		}
		else if(edgeScorerType.equalsIgnoreCase("UPPERBOUND")){
			scoredGraphs = ParseEvaluator.createUpperBound(originalGraphs, goldGraphs, edgeMatchType);
		}
		else{
			scoredGraphs = runRescoring(originalGraphs, 
										edgeScorerType, 
										graphScorerType,
										graphEditors,
										semModel,
										expansionListPath,
										expansionLimit, 
										numThreads);
		}
		
		LinkedHashMap<String,Double> results = ParseEvaluator.run(scoredGraphs, goldGraphs, edgeMatchType);
		LinkedHashMap<String,Double> significanceResults = ParseEvaluator.calculateStatisticalSignificance(originalGraphs, scoredGraphs, goldGraphs, edgeMatchType);
		
		
		results.putAll(significanceResults);
		log(formatResults(results));
		
		log(ParseEvaluator.getTypeStatistics(scoredGraphs, goldGraphs, edgeMatchType));
		
		return results;
	}
	
	public static void runExperiments(){
		for(String dataset : Arrays.asList("devsub", "test", "genia")){
			String datasetPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/";
			String modelName = dataset.equalsIgnoreCase("genia")?"model4":"model3";
			String modelPath = "/anfs/bigdisc/mr472/semsim_models/" + modelName;
			String expansionListPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion/expansion-" + dataset + "-weightedCosine-hyponyms-" + modelName + ".txt.gz";
			String posMapPath = "/auto/homes/mr472/Documents/Projects/SemSim/tagsets/claws2-universal.txt";
			int graphScorerType = GraphScorer.COMBINE_NODEAVG;
			int numThreads = 8;
			
			SemModel semModel = new SemModel(modelPath, true, true); 
			
			
			for(String edgeScorerType : Arrays.asList("I", "BASELINE", "UPPERBOUND", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2")){
				System.out.println("##### " + dataset + " " + edgeScorerType);
				run(semModel, datasetPath, expansionListPath, posMapPath, edgeScorerType, graphScorerType, numThreads);
			}
			
			semModel = null;
			System.gc();
		}
	}
	
	public static void main(String[] args){
		runExperiments();
	}
}

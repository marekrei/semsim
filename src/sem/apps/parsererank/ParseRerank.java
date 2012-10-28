package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;
import sem.grapheditor.AddNodesGraphEditor;
import sem.grapheditor.BypassAdpGraphEditor;
import sem.grapheditor.BypassConjGraphEditor;
import sem.grapheditor.CombineSubjObjGraphEditor;
import sem.grapheditor.ConvertPosGraphEditor;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LemmatiserGraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.graphreader.GraphFormatException;
import sem.graphreader.ParsevalGraphReader;
import sem.graphreader.RaspXmlGraphReader;
import sem.graphvis.GraphVisualiser;
import sem.graphwriter.TSVGraphWriter;
import sem.graphwriter.TikzDependencyGraphWriter;
import sem.model.SemModel;
import sem.util.FileReader;
import sem.util.Tools;

public class ParseRerank {

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
	
	public static ArrayList<LinkedHashMap<Graph,Double>> loadOriginalParses(String inputPath, int nodeType, boolean filterDuplicates){
		
		//
		// Reading in the original parses
		//
		
		System.out.println("# Reading in the original parses...");
		ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		int graphCount;
		
		try {
			RaspXmlGraphReader inputReader = new RaspXmlGraphReader(inputPath, nodeType, true, false);
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
			System.out.println("Read in " + originalGraphs.size() + " sentences, " + graphCount + " graphs.");
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		
		//
		// Filtering duplicates
		//
		
		if(filterDuplicates){
			System.out.println("# Filtering duplicates...");
			ArrayList<LinkedHashMap<Graph,Double>> filteredGraphs = filterDuplicates(originalGraphs, true);
			graphCount = 0;
			for(LinkedHashMap<Graph,Double> sentence : filteredGraphs)
				graphCount += sentence.size();
			System.out.println("Ended up with " + filteredGraphs.size() + " sentences, " + graphCount + " graphs.");
			originalGraphs = filteredGraphs;
		}
		return originalGraphs;
	}
	
	public static ArrayList<Graph> loadGoldParses(String goldPath){
		//
		// Reading in gold standard
		//
		
		System.out.println("# Reading in gold standard...");
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
		System.out.println("Read in " + goldGraphs.size() + " graphs / sentences.");
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
		ParseScorer parseScorer = new ParseScorer(numThreads);
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
	
	public static LinkedHashMap<String,Double> runEvaluation(ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs, ArrayList<Graph> goldGraphs, int edgeMatchType){
		System.out.println("## Running evaluation...");
		LinkedHashMap<String,Double> results = null;
		//for(int edgeMatchType : Arrays.asList(1)){//, 1, 2, 3)){
			//System.out.println("Edge Match Type: " + edgeMatchType);
			results = ParseEvaluator.run(scoredGraphs, goldGraphs, edgeMatchType);
			/*for(Entry<String,Double> result : results.entrySet())
				System.out.print(result.getKey() + "\t");
			System.out.println();
			for(Entry<String,Double> result : results.entrySet())
				System.out.print(result.getValue() + "\t");
			System.out.println();
		//}*/
		return results;
	}
	
	public static void printEvaluation(LinkedHashMap<String,Double> results){
		for(Entry<String,Double> result : results.entrySet())
			System.out.print(result.getKey() + "\t");
		System.out.println();
		for(Entry<String,Double> result : results.entrySet())
			System.out.print(result.getValue() + "\t");
		System.out.println();
	}
	
	////////// TEMP
	public static ArrayList<LinkedHashMap<Graph,Double>> runPostScoring(ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs, int method, double lambda){
		
		int METHOD_MAX = 1;
		int METHOD_AVERAGE = 2;
		int METHOD_MEDIAN = 3;
		int METHOD_TOKENS = 4;
		int METHOD_POS = 5;
		
		ArrayList<String> noDepPos = new ArrayList<String>(Arrays.asList("[[ellippos]]", "\"", "!", ")", ".", "-", ";", ":", "?", "CST"));
		
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs2 = new ArrayList<LinkedHashMap<Graph,Double>>();
		for(LinkedHashMap<Graph,Double> map : scoredGraphs){
			double maxSize = 0, sizesSum = 0;
			ArrayList<Double> sizes = new ArrayList<Double>();
			for(Entry<Graph,Double> e : Tools.sort(map, true).entrySet()){
				if(e.getKey().getNodes().size() > maxSize)
					maxSize = e.getKey().getNodes().size();
				sizes.add((double)e.getKey().getEdges().size());
				sizesSum += (double)e.getKey().getEdges().size();
			}
			double averageSize = sizesSum / (double) map.size();
			
			Collections.sort(sizes);
			double medianSize = sizes.get(0);
			if(sizes.size() > 2)
				medianSize = sizes.get((sizes.size() +1) / 2);
			
			LinkedHashMap<Graph,Double> map2 = new LinkedHashMap<Graph,Double>();
			
			for(Entry<Graph,Double> e : map.entrySet()){
				double tokensSize = e.getKey().getNodes().size();
				double posSize = 0.0;
				for(Node node : e.getKey().getNodes())
					if(!noDepPos.contains(node.getPos()))
						posSize++;
				if(posSize > 0.0)
					posSize--;
				
				double edgeCount = e.getKey().getEdges().size();
				double d = 0.0;
				if(method == METHOD_MAX)
					d = Math.abs(maxSize - edgeCount);
				else if(method == METHOD_AVERAGE)
					d = Math.abs(averageSize - edgeCount);
				else if(method == METHOD_MEDIAN)
					d = Math.abs(medianSize - edgeCount);
				else if(method == METHOD_TOKENS)
					d = Math.abs(tokensSize - edgeCount);
				else if(method == METHOD_POS)
					d = Math.abs(posSize - edgeCount);
				else
					throw new RuntimeException("Unknown value: " + method);
				
				map2.put(e.getKey(), e.getValue() * Math.pow(lambda, d));
			}
			
			scoredGraphs2.add(Tools.sort(map2,true));
		}
		return scoredGraphs2;
	}
	///////////////////
	
	public static void run() {
		
		//
		// Making sure that all the exceptions are caught and the process halted
		//
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
		        e.printStackTrace();
		        System.exit(1);
		    }
		});
		
		int numThreads = 8;
		int expansionLimit = 10;
		int edgeMatchType = 1;
		
		for(String dataset : Arrays.asList("dev")){
			String modelName = "model3";
			if(dataset.equals("genia"))
				modelName = "model4";
			String modelPath = "/anfs/bigdisc/mr472/semsim_models/" + modelName;
			SemModel semModel = new SemModel(modelPath, true, true); 
			int nodeType = RaspXmlGraphReader.NODES_TOKENS;
			boolean symmetric = false;
						for(String simMeasure : Arrays.asList("weightedCosine")){//"balAPInc", "cosine", "clarkeDE", "diceGen2")){//, "diceGen2", "clarkeDE")){
							for(boolean hypernyms : Arrays.asList(false)){//, true)){  
								for(int parseScorerMethod : Arrays.asList(8)){//, ParseScorer.COMBINE_AVG, ParseScorer.COMBINE_NODEAVG4, ParseScorer.COMBINE_NODEAVG5, ParseScorer.COMBINE_NODEAVG6)){//, ParseScorer.COMBINE_SUM)){//ParseScorer.COMBINE_SUM, ParseScorer.COMBINE_AVG, ParseScorer.COMBINE_SUM, ParseScorer.COMBINE_AVG, ParseScorer.COMBINE_NODEAVG1, ParseScorer.COMBINE_NODEAVG2, ParseScorer.COMBINE_NODEAVG3, ParseScorer.COMBINE_NODEAVG4)){
									for(String edgeScorerType : Arrays.asList("RES", "CMB1NEW", "CMB2NEW", "CMB6NEW", "CMB7NEW")){//"CES1NEW", "CES2NEW", "CMB1NEW", "CMB2NEW", "CMB3NEW", "CMB4NEW", "CMB5NEW")){//"RES", "CES1NEW", "CES2NEW", "ECES1NEW", "ECES2NEW", "CMB1NEW", "CMB2NEW", "CMB3NEW", "CMB4NEW", "CMB5NEW", "NECES1NEW", "NECES2NEW", "CES1NEW1", "CES2NEW1", "CES1NEW2", "CES2NEW2")){
									//for(String edgeScorerType : Arrays.asList("RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES2B1", "CES2B2", "CES2B3", "CES2B4", "CES1B4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4")){//, "CMB5", "CMB6", "CMB7")){
									///for(String edgeScorerType : Arrays.asList("RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES2B1", "CES2B2", "CES2B3", "CES2B4", "CES1B4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4", "CMB5", "CMB6", "CMB7")){
									//for(String edgeScorerType : Arrays.asList("I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES1S1", "CES2S1", "CES2B1", "CES2B2", "CES2B3", "CES2B4", "CES1b4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4", "CMB5", "CMB6", "CMB7")){
									//for(String edgeScorerType : Arrays.asList("I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES1S1", "CES2S1", "CES2B1", "CES2B2", "CES2I1", "CES2I2", "CES2I3", "CES2I4", "CES2I5", "CES2I6", "CES2I7", "CES2B3", "CES2B4", "CES1B4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4", "CMB5", "CMB6", "CMB7")){
									//for(String edgeScorerType : Arrays.asList("ECES2")){//"ECES1", "ECES2", "CMB2")){//"BASELINE", "UPPERBOUND")){//, //, "ECES1", "ECES2", "CMB1", "CMB2")){//, "CES2B3", "CES2B4", "CES2S1", "CES2I1", "CES2I2", "CES2I3", "CES2I4", "CES2I5", "CES2I6", "CES2I7")){//"CES1B4", "CMB3", "CMB4", "CMB5")){//"I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2")){
										for(int postScorerMethod : Arrays.asList(0)){//, 1, 2, 3, 4, 5)){
											for(double postScorerLambda : Arrays.asList(1)){//0.95)){//, 0.99)){//0.9, 0.95, 0.99)){
											 	
												String inputPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/parsed_1000.xml";
												String goldPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/gold.rasp";
												String lemmaMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/lemmas.map";
												//String expansionMapPath = "/auto/homes/mr472/Documents/PhD/SemSimEval/parse_rerank/"+dataset+"/expansion_balAPinc_S1-P0L1T1C1A3V4N4.txt";
												//String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion_old/expansion_"+dataset+"_balAPinc_S1-P0L1T1C1A3V4N4.txt";
												String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion/expansion-" + dataset + "-" + simMeasure + "-" + (hypernyms?"hypernyms":"hyponyms") + "-" + modelName + ".txt.gz";
												
												ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
														new LemmatiserGraphEditor(lemmaMapPath),
														new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_NONE),
														new LowerCaseGraphEditor(),
														new NumTagsGraphEditor(),
														new BypassConjGraphEditor(),
														new BypassAdpGraphEditor(3),
														new CombineSubjObjGraphEditor(4),
														new AddNodesGraphEditor(4)//,
														//new ReverseEdgesGraphEditor(),
														//new NullEdgesGraphEditor()
														));
												
												String labels = "Model\tSymmetric\tNodeType\tSimMeasure\tHypernyms\tDataset\tEdgeScorerType\tParseScorerMethod\tPostScorerMethod\tPostScorerlambda";
												
												String description = modelName + "\t" + symmetric + "\t" + nodeType +"\t" + simMeasure + "\t" + hypernyms + "\t" + 
																	dataset + "\t" + edgeScorerType + "\t" + parseScorerMethod + "\t" + postScorerMethod + "\t" + postScorerLambda;
												System.out.println("#### RUNNING: " + description);
												System.out.println("Model: " + modelName);
												System.out.println("Dataset: " + dataset);
												System.out.println("SimMeasure: " + simMeasure);
												System.out.println("Hypernyms: " + hypernyms);
												System.out.println("EdgeScorer: " + edgeScorerType);
												System.out.println("ParseScorerMethod: " + parseScorerMethod);
												System.out.println("PostScoring: " + postScorerMethod + "\t" + postScorerLambda);
												
												ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = loadOriginalParses(inputPath, nodeType, true);
												ArrayList<Graph> goldGraphs = loadGoldParses(goldPath);
												
												if(edgeScorerType.equals("BASELINE")){
													printEvaluation(runEvaluation(originalGraphs, goldGraphs, edgeMatchType));
												}
												else if(edgeScorerType.equalsIgnoreCase("UPPERBOUND")){
													ArrayList<LinkedHashMap<Graph,Double>> upperBoundGraphs = ParseEvaluator.createUpperBound(originalGraphs, goldGraphs, edgeMatchType);
													
													printEvaluation(runEvaluation(upperBoundGraphs, goldGraphs, edgeMatchType));
												}
												else{
													ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs = runRescoring(originalGraphs, 
																														edgeScorerType, 
																														parseScorerMethod,
																														graphEditors, 
																														semModel, 
																														expansionMapPath, 
																														expansionLimit, 
																														numThreads);
					
													if(postScorerMethod > 0)
														scoredGraphs = runPostScoring(scoredGraphs, postScorerMethod, postScorerLambda);
													LinkedHashMap<String,Double> results = runEvaluation(scoredGraphs, goldGraphs, edgeMatchType);
													printEvaluation(results);
													
													
													for(Entry<String,Double> e : results.entrySet()){
														labels += "\t" + e.getKey();
														description += "\t" + e.getValue();
													}
													
													System.out.println("##### RES: " + description.trim());
												}
											}
										}
									}
								}
							}
						}
						
					//}
				//}		
				semModel = null;
				System.gc();
			//}
		}
	
	}
	
	public static void runX(){
		String dataset = "test";
		String modelName = "model3";
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/" + modelName;
		SemModel semModel = new SemModel(modelPath, true, true); 
		int nodeType = RaspXmlGraphReader.NODES_TOKENS;
		boolean symmetric = false;
		
		String simMeasure = "weightedCosine";
		boolean hypernyms = false;
		int edgeMatchType = 1;
		
		String edgeScorerType = "CMB2NEW";
		int parseScorerMethod = 8;//ParseScorer.COMBINE_SUM;
		int expansionLimit = 10;
		int numThreads = 8;
		
		String inputPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/parsed_1000.xml";
		String goldPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/gold.rasp";
		String lemmaMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/lemmas.map";
		//String expansionMapPath = "/auto/homes/mr472/Documents/PhD/SemSimEval/parse_rerank/"+dataset+"/expansion_balAPinc_S1-P0L1T1C1A3V4N4.txt";
		//String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion_old/expansion_"+dataset+"_balAPinc_S1-P0L1T1C1A3V4N4.txt";
		String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion/expansion-" + dataset + "-" + simMeasure + "-" + (hypernyms?"hypernyms":"hyponyms") + "-" + modelName + ".txt.gz";
		
		ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				new LemmatiserGraphEditor(lemmaMapPath),
				new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_NONE),
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor(),
				new BypassConjGraphEditor(),
				new BypassAdpGraphEditor(3),
				new CombineSubjObjGraphEditor(4),
				new AddNodesGraphEditor(4)//,
				//new ReverseEdgesGraphEditor(),
				//new NullEdgesGraphEditor() 
				));
		
		ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = loadOriginalParses(inputPath, nodeType, true);
		ArrayList<Graph> goldGraphs = loadGoldParses(goldPath);
		
		
		
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs = runRescoring(originalGraphs, 
				edgeScorerType, 
				parseScorerMethod,
				graphEditors, 
				semModel, 
				expansionMapPath, 
				expansionLimit, 
				numThreads);
		/*
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs_eces2 = runRescoring(originalGraphs, 
				"ECES2NEW", 
				parseScorerMethod,
				graphEditors, 
				semModel, 
				expansionMapPath, 
				expansionLimit, 
				numThreads);
		
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs_res = runRescoring(originalGraphs, 
				"RES", 
				parseScorerMethod,
				graphEditors, 
				semModel, 
				expansionMapPath, 
				expansionLimit, 
				numThreads);
		
		ArrayList<LinkedHashMap<Graph,Double>> originalGraphsSub = new ArrayList<LinkedHashMap<Graph,Double>>();
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphsSub = new ArrayList<LinkedHashMap<Graph,Double>>();
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphsSub_eces2 = new ArrayList<LinkedHashMap<Graph,Double>>();
		ArrayList<LinkedHashMap<Graph,Double>> scoredGraphsSub_res = new ArrayList<LinkedHashMap<Graph,Double>>();
		ArrayList<Graph> goldGraphsSub = new ArrayList<Graph>();
		
		/*
		for(int i = 0; i < 14; i++){
			System.out.println("---------------- Iteration " + i);
			originalGraphsSub.clear();
			scoredGraphsSub.clear();
			scoredGraphsSub2.clear();
			goldGraphsSub.clear();
			
			for(int j = 0; j < goldGraphs.size(); j++){
				if(j >= 10*i && j < 10*(i+1)){
					continue;
				}
				else {
					originalGraphsSub.add(originalGraphs.get(j));
					scoredGraphsSub.add(scoredGraphs.get(j));
					scoredGraphsSub2.add(scoredGraphs2.get(j));
					goldGraphsSub.add(goldGraphs.get(j));
				}
			}
			
			printEvaluation(ParseEvaluator.run(originalGraphsSub, goldGraphsSub, edgeMatchType));
			printEvaluation(ParseEvaluator.run(scoredGraphsSub2, goldGraphsSub, edgeMatchType));
			printEvaluation(ParseEvaluator.run(scoredGraphsSub, goldGraphsSub, edgeMatchType));
		} 
		*/
		/*
		for(int i = 0; i < goldGraphs.size(); i++){
			originalGraphsSub.clear();
			scoredGraphsSub.clear();
			scoredGraphsSub_eces2.clear();
			scoredGraphsSub_res.clear();
			goldGraphsSub.clear();
			
			originalGraphsSub.add(originalGraphs.get(i));
			scoredGraphsSub.add(scoredGraphs.get(i));
			scoredGraphsSub_eces2.add(scoredGraphs_eces2.get(i));
			scoredGraphsSub_res.add(scoredGraphs_res.get(i));
			goldGraphsSub.add(goldGraphs.get(i));
			
			LinkedHashMap<String,Double> results = ParseEvaluator.run(scoredGraphsSub, goldGraphsSub, edgeMatchType);
			LinkedHashMap<String,Double> results_eces2 = ParseEvaluator.run(scoredGraphsSub_eces2, goldGraphsSub, edgeMatchType);
			LinkedHashMap<String,Double> results_res = ParseEvaluator.run(scoredGraphsSub_res, goldGraphsSub, edgeMatchType);
			
			
			if(results.get("fmeasure") < results_eces2.get("fmeasure")){
				System.out.println("###################### MISMATCH: " + i +  " : " + originalGraphs.get(i).size()  + " " + scoredGraphs.get(i).size() + " " + scoredGraphs_eces2.get(i).size() + " : " + results.get("fmeasure") + " " + results_eces2.get("fmeasure") + " " + results_res.get("fmeasure") + " : " + (results.get("fmeasure") - results_eces2.get("fmeasure")));
				System.out.println("GOLD:");
				goldGraphs.get(i).print();
				System.out.println("SCORED:");
				for(Entry<Graph,Double> e : Tools.sort(scoredGraphs_eces2.get(i), true).entrySet()){
					System.out.println("Score eces2: " + e.getValue());
					break;
				}
				for(Entry<Graph,Double> e : Tools.sort(scoredGraphs_res.get(i), true).entrySet()){
					System.out.println("Score res: " + e.getValue());
					break;
				}
				for(Entry<Graph,Double> e : Tools.sort(scoredGraphs.get(i), true).entrySet()){
					System.out.println("Score: " + e.getValue() + " " + scoredGraphs_eces2.get(i).get(e.getKey()) + " " + scoredGraphs_res.get(i).get(e.getKey()));
					e.getKey().print();
					break;
				}
			}
		}
		
		*/
		System.out.println("Original:");
		printEvaluation(ParseEvaluator.run(originalGraphs, goldGraphs, edgeMatchType));
		System.out.println("Scored:");
		printEvaluation(ParseEvaluator.run(scoredGraphs, goldGraphs, edgeMatchType));
		/*System.out.println("Scored res:");
		printEvaluation(ParseEvaluator.run(scoredGraphs_res, goldGraphs, edgeMatchType));
		System.out.println("Scored eces2:");
		printEvaluation(ParseEvaluator.run(scoredGraphs_eces2, goldGraphs, edgeMatchType));*/
		
		ParseEvaluator.calculateStatisticalSignificance(originalGraphs, scoredGraphs, goldGraphs, edgeMatchType);
		
	}
	
	public static void runCLI(String modelName, String dataset, String simMeasure, boolean hypernyms, int parseScorerMethod){
		
		//
		// Making sure that all the exceptions are caught and the process halted
		//
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
		        e.printStackTrace();
		        System.exit(1);
		    }
		});
		
		int numThreads = 8;
		int expansionLimit = 10;
		int edgeMatchType = 1;
		
		boolean symmetric = false;
		String modelPath = "/anfs/bigdisc/mr472/semsim_models/" + modelName;
		SemModel semModel = new SemModel(modelPath, true, true); 
		
	
		for(int nodeType : Arrays.asList(RaspXmlGraphReader.NODES_TOKENS)){//, RaspXmlGraphReader.NODES_ALL)){
			for(String edgeScorerType : Arrays.asList("RES", "CES1NEW", "CES2NEW", "ECES1NEW", "ECES2NEW", "CMB1NEW", "CMB2NEW", "CMB3NEW", "CMB4NEW", "CMB5NEW", "NECES1NEW", "NECES2NEW", "CES1NEW1", "CES2NEW1", "CES1NEW2", "CES2NEW2", "GECES2NEW")){
			//for(String edgeScorerType : Arrays.asList("I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES1S1", "CES2S1", "CES2B1", "CES2B2", "CES2B3", "CES2B4", "CES1b4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4", "CMB5", "CMB6", "CMB7")){
			//for(String edgeScorerType : Arrays.asList("I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2", "CES1S1", "CES2S1", "CES2B1", "CES2B2", "CES2I1", "CES2I2", "CES2I3", "CES2I4", "CES2I5", "CES2I6", "CES2I7", "CES2B3", "CES2B4", "CES1B4", "CES2B5", "CES1B5", "CES2C1", "CES1C1", "CMB3", "CMB4", "CMB5", "CMB6", "CMB7")){
			//for(String edgeScorerType : Arrays.asList("ECES2")){//"ECES1", "ECES2", "CMB2")){//"BASELINE", "UPPERBOUND")){//, //, "ECES1", "ECES2", "CMB1", "CMB2")){//, "CES2B3", "CES2B4", "CES2S1", "CES2I1", "CES2I2", "CES2I3", "CES2I4", "CES2I5", "CES2I6", "CES2I7")){//"CES1B4", "CMB3", "CMB4", "CMB5")){//"I", "RES", "CES1", "CES2", "ECES1", "ECES2", "CMB1", "CMB2")){
				for(int postScorerMethod : Arrays.asList(0)){//, 1, 2, 3, 4, 5)){
					for(double postScorerLambda : Arrays.asList(1)){//0.95)){//, 0.99)){//0.9, 0.95, 0.99)){
												
						String inputPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/parsed_1000.xml";
						String goldPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/gold.rasp";
						String lemmaMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/" + dataset + "/lemmas.map";
						//String expansionMapPath = "/auto/homes/mr472/Documents/PhD/SemSimEval/parse_rerank/"+dataset+"/expansion_balAPinc_S1-P0L1T1C1A3V4N4.txt";
						//String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion_old/expansion_"+dataset+"_balAPinc_S1-P0L1T1C1A3V4N4.txt";
						String expansionMapPath = "/anfs/bigdisc/mr472/corpora/ParseRerank/expansion/expansion-" + dataset + "-" + simMeasure + "-" + (hypernyms?"hypernyms":"hyponyms") + "-" + modelName + ".txt.gz";
						
						ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
								new LemmatiserGraphEditor(lemmaMapPath),
								new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_NONE),
								new LowerCaseGraphEditor(),
								new NumTagsGraphEditor(),
								new BypassConjGraphEditor(),
								new BypassAdpGraphEditor(3),
								new CombineSubjObjGraphEditor(4),
								new AddNodesGraphEditor(4)//,
								//new ReverseEdgesGraphEditor(),
								//new NullEdgesGraphEditor()
								));
						
						String labels = "Model\tSymmetric\tNodeType\tSimMeasure\tHypernyms\tDataset\tEdgeScorerType\tParseScorerMethod\tPostScorerMethod\tPostScorerlambda";
						
						String description = modelName + "\t" + symmetric + "\t" + nodeType +"\t" + simMeasure + "\t" + hypernyms + "\t" + 
											dataset + "\t" + edgeScorerType + "\t" + parseScorerMethod + "\t" + postScorerMethod + "\t" + postScorerLambda;
						System.out.println("#### RUNNING: " + description);
						System.out.println("Model: " + modelName);
						System.out.println("Dataset: " + dataset);
						System.out.println("SimMeasure: " + simMeasure);
						System.out.println("Hypernyms: " + hypernyms);
						System.out.println("EdgeScorer: " + edgeScorerType);
						System.out.println("ParseScorerMethod: " + parseScorerMethod);
						System.out.println("PostScoring: " + postScorerMethod + "\t" + postScorerLambda);
						
						ArrayList<LinkedHashMap<Graph,Double>> originalGraphs = loadOriginalParses(inputPath, nodeType, true);
						ArrayList<Graph> goldGraphs = loadGoldParses(goldPath);
						
						if(edgeScorerType.equals("BASELINE")){
							printEvaluation(runEvaluation(originalGraphs, goldGraphs, edgeMatchType));
						}
						else if(edgeScorerType.equalsIgnoreCase("UPPERBOUND")){
							ArrayList<LinkedHashMap<Graph,Double>> upperBoundGraphs = ParseEvaluator.createUpperBound(originalGraphs, goldGraphs, edgeMatchType);
							
							printEvaluation(runEvaluation(upperBoundGraphs, goldGraphs, edgeMatchType));
						}
						else{
							ArrayList<LinkedHashMap<Graph,Double>> scoredGraphs = runRescoring(originalGraphs, 
																								edgeScorerType, 
																								parseScorerMethod,
																								graphEditors, 
																								semModel, 
																								expansionMapPath, 
																								expansionLimit, 
																								numThreads);

							if(postScorerMethod > 0)
								scoredGraphs = runPostScoring(scoredGraphs, postScorerMethod, postScorerLambda);
							LinkedHashMap<String,Double> results = runEvaluation(scoredGraphs, goldGraphs, edgeMatchType);
							printEvaluation(results);
							
							
							for(Entry<String,Double> e : results.entrySet()){
								labels += "\t" + e.getKey();
								description += "\t" + e.getValue();
							}
							System.out.println(labels);
							System.out.println("##### RES: " + description.trim());
						}
					}
				}
			}
		}
	
		semModel = null;
		System.gc();
	}
	
	public static void createCondorFile(){
		
		
		System.out.println("universe       = java");
		System.out.println("executable     = semsim.jar");
		System.out.println("jar_files      = semsim.jar lib/trove-3.0.2.jar");
		System.out.println("requirements   = Memory >= 19000");
		System.out.println("java_vm_args   = -Xmx17G -Dfile.encoding=UTF-8");
		System.out.println("nice_user = True");
				
				
		for(String dataset : Arrays.asList("test")){
			String modelName = "model3";
			if(dataset.equalsIgnoreCase("genia"))
				modelName = "model4";
			
			for(String simMeasure : Arrays.asList("balAPInc", "weightedCosine")){
				for(boolean hypernyms : Arrays.asList(false, true)){
					for(int parseScorerMethod : Arrays.asList(0,2,3,4,5,6,8,9)){
						System.out.println("arguments      = sem.apps.parsererank.ParseRerank " + modelName + " " + dataset + " " + simMeasure + " " + hypernyms + " " + parseScorerMethod);
						System.out.println("output         = output3/output-"+dataset+"-"+modelName+"-"+simMeasure+"-"+(hypernyms?"hypernyms":"hyponyms")+"-"+parseScorerMethod + ".txt");
						System.out.println("error          = error3/error-"+dataset+"-"+modelName+"-"+simMeasure+"-"+(hypernyms?"hypernyms":"hyponyms")+"-"+parseScorerMethod + ".txt");
						System.out.println("queue\n");
					}
				}
			}
		}
	}
	
	public static void runY(){
		String dataset = "test";
		String goldPath = "/mnt/maun/anfs/bigdisc/mr472/corpora/ParseRerank/"+dataset+"/gold.rasp";
		ArrayList<Graph> goldGraphs = loadGoldParses(goldPath);
		
		FileReader fr = new FileReader("/mnt/maun/anfs/bigdisc/mr472/corpora/ParseRerank/"+dataset+"/tokenised.txt");
		
		
		
		ArrayList<Graph> goldGraphs2 = new ArrayList<Graph>();
		
		double sum = 0.0, lsum = 0.0, tsum = 0.0, esum = 0.0;
		
		for(Graph g : goldGraphs){
			HashMap<String,Node> map = new HashMap<String,Node>();
			Graph g2 = new Graph();
			goldGraphs2.add(g2);
			
			String line = fr.next();
			
			if(line == null)
				throw new RuntimeException("oeh");
			
			String[] bits = line.trim().split("\\s+");
			
			for(int i = 1; i < bits.length; i++){
				Node n = new Node(bits[i], "NONE");
				g2.addNode(n);
				if(!map.containsKey(bits[i])){
					map.put(bits[i], n);
				}
			}
			for(Edge edge : g.getEdges()){
				Node n1 = map.get(edge.getHead().getLemma());
				Node n2 = map.get(edge.getDep().getLemma());
				if(n1 == null){
					n1 = new Node(edge.getHead().getLemma(), "NONE");
					g2.addNode(n1);
				}
				if(n2 == null){
					n2 = new Node(edge.getDep().getLemma(), "NONE");
					g2.addNode(n2);
				}
				g2.addEdge(new Edge(edge.getLabel(), n1, n2));
			}
			
			for(Node n : g2.getNodes()){
				if(n.getLemma().equals("&"))
					n.setLemma("AMP");
			}
			sum += (double)g.getEdges().size() / (double)(bits.length - 1);
			tsum += bits.length-1;
			esum += g.getEdges().size();
		}
		System.out.println("Average number of edges per token: " + (sum / (double)goldGraphs.size()));
		System.out.println("Average token count: " + (tsum / (double)goldGraphs.size()));
		System.out.println("Average edge count: " + (esum / (double)goldGraphs.size()));
		System.out.println("Edges per token: " + (esum / tsum));
		
		
		
		//GraphVisualiser gv = new GraphVisualiser(false);
		//gv.displayGraphs(goldGraphs2);
		
		TikzDependencyGraphWriter gw = new TikzDependencyGraphWriter("/home/marek/Desktop/tikz/tempdevgraphs.tex", true, false, true);
		int count = 0;
		for(Graph g : goldGraphs2){
			if(++count > 130)
				break;
			gw.write(g);
			
		}
		gw.close();
	}
	
	public static void main(String[] args){
		/*
		if(args.length == 5)
			runCLI(args[0], args[1], args[2], Boolean.parseBoolean(args[3]), Integer.parseInt(args[4]));
		else
			runX();
		*/
		 
		//createCondorFile();
		
		runX();
	}

}

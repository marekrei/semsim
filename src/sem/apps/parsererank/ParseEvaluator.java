package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.util.Tools;

/**
 * Functions for evaluating parsing and parse reranking
 *
 */
public class ParseEvaluator {
	
	public static final int MATCH_UNLABELLED = 0;
	public static final int MATCH_HIERARCHICAL = 1;
	public static final int MATCH_HIERARCHICALNORM = 2;
	public static final int MATCH_LABELLED = 3;
	
	
	/**
	 * Finds whether the two lemmas match. A match is defined as either:
	 * 
	 * a) Both lemmas are equal
	 * OR
	 * b) neither lemma is "_" and one lemma is an ellip (matches to the static ellip node in the Graph class)
	 * 
	 * This allows matching ellipses to other lemmas, following the RASP evaluation schema.
	 * For more information, see: ﻿Watson, R. (2006). RASP Evaluation Schemes.
	 * The function is symmetric.
	 * @param lemma1 Lemma 1
	 * @param lemma2 Lemma 2
	 * @return True if a match is found, false otherwise
	 */
	public static double lemmaMatch(String lemma1, String lemma2){
		if(lemma1.equals(lemma2)
				||	(!lemma1.equals("_") && !lemma2.equals("_") 
						&& (lemma1.equals(Graph.ellip.getLemma()) || lemma2.equals(Graph.ellip.getLemma())))){
			return 1.0;
		}
		return 0.0;
	}
	
	/**
	 * Takes the test and gold edges, returns a non-negative value showing how well the test edge matches the gold edge.
	 * @param testEdge Test edge
	 * @param goldEdge Gold standard edge
	 * @param edgeMatchType Edge matching type
	 * @return The similarity of the test edge to the gold standard edge
	 */
	public static double edgeMatch(Edge testEdge, Edge goldEdge, int edgeMatchType){
		if(edgeMatchType == MATCH_UNLABELLED){
			return lemmaMatch(testEdge.getHead().getLemma(), goldEdge.getHead().getLemma())
					* lemmaMatch(testEdge.getDep().getLemma(), goldEdge.getDep().getLemma());
		}
		else if(edgeMatchType == MATCH_HIERARCHICAL || edgeMatchType == MATCH_HIERARCHICALNORM || edgeMatchType == MATCH_LABELLED){
			return lemmaMatch(testEdge.getHead().getLemma(), goldEdge.getHead().getLemma())
					* lemmaMatch(testEdge.getDep().getLemma(), goldEdge.getDep().getLemma())
					* edgeLabelMatch(testEdge.getLabel(), goldEdge.getLabel(), edgeMatchType);
		}
		else {
			System.err.println("Unknown edge match type n ParseEvaluator:edgeLabelMatch(): " + edgeMatchType);
			System.exit(1);
			return 0.0;
		}
	}
	
	/**
	 * Returns a value representing the similarity between the two edge labels, given the edge matching criteria.
	 * MATCH_UNLABELLED - always returns 1.0, as the match does not depend on the labels
	 * MATCH_HIERARCHICAL - Returns the number of GR types that subsume both edge labels
	 * MATCH_HIERARCHICALNORM - The same, only normalised by the number of types that subsume the gold standard edge, giving a value between 0 and 1
	 * MATCH_LABELLED - Returns 1.0 if the labels are equal, 0.0 otherwise
	 * 
	 * @param edgeLabelTest Test edge label
	 * @param edgeLabelGold Gold standard edge label
	 * @param edgeMatchType Edge matching type
	 * @return A non-negative value showing how well the test edge matches the gold edge
	 */
	public static double edgeLabelMatch(String edgeLabelTest, String edgeLabelGold, int edgeMatchType){
		
		ArrayList<String> goldLabelTypes = new ArrayList<String>(RaspGrTypeHierarchy.getAncestors(edgeLabelGold));
		goldLabelTypes.add(edgeLabelGold);
		
		ArrayList<String> testLabelTypes = new ArrayList<String>(RaspGrTypeHierarchy.getAncestors(edgeLabelTest));
		testLabelTypes.add(edgeLabelTest);
		
		
		if(edgeMatchType == MATCH_UNLABELLED)
			return 1.0;
		else if(edgeMatchType == MATCH_HIERARCHICAL || edgeMatchType == MATCH_HIERARCHICALNORM){
			double score = 0.0;
			for(String t : goldLabelTypes){
				if(testLabelTypes.contains(t))
					score += 1.0;
			}
			if(edgeMatchType == MATCH_HIERARCHICALNORM)
				score /= (double)(goldLabelTypes.size());
			return score;
		}
		else if(edgeMatchType == MATCH_LABELLED){
			if(edgeLabelTest.equals(edgeLabelGold))
				return 1.0;
			return 0.0;
		}
		else {
			System.out.println("Unknown edgeMatchType");
			System.exit(1);
			return 0.0;
		}
	}
	
	/**
	 * Finds the highest possible score when mapping edges from the test graph to the gold standard graph.
	 * @param testGraph Test graph
	 * @param goldGraph Gold standard graph
	 * @param edgeMatchType Edge matching type
	 * @return Maximum mapping score (sum of individual edge scores)
	 */
	/*public static double findEdgeMappingScore(Graph testGraph, Graph goldGraph, int edgeMatchType){
		ArrayList<HashMap<Integer,Double>> scoreMatrix = new ArrayList<HashMap<Integer,Double>>();
		double score;
		System.out.print("MAP: ");
		for(Edge goldEdge : goldGraph.getEdges()){
			if(goldEdge.getLabel().equals("passive"))
				continue;
			
			HashMap<Integer,Double> map = new HashMap<Integer,Double>();
			scoreMatrix.add(map);
			for(int i = 0; i < testGraph.getEdges().size(); i++){
				if(testGraph.getEdges().get(i).getLabel().equals("passive"))
					continue;
				score = edgeMatch(testGraph.getEdges().get(i), goldEdge, edgeMatchType);
				if(score > 0.0){
					map.put(i, score);
					//System.out.print(testGraph.getEdges().get(i).toString() + " (" + score + "), ");
				}
			}
			System.out.print(map.size() + " "); 
		}
		System.out.println();
		
		int ccc = 0;
		System.out.print("RMAP:");
		for(int i = 0; i < testGraph.getEdges().size(); i++){
			int count = 0;
			int j = 0;
			for(Edge goldEdge : goldGraph.getEdges()){
				if(goldGraph.getEdges().get(j).getLabel().equals("passive"))
					continue;
				if(scoreMatrix.get(j).containsKey(i))
					count ++;
				if(count == 2)
					ccc ++;
				j++;
			}
			System.out.print(" "+count);
		}
		System.out.println();
		
		if(ccc >= 2){
			System.out.println("Suitable candidate. Printing. Testgraph -----------------");
			testGraph.print();
			System.out.println("Goldgraph--------------------");
			goldGraph.print();
		}
		
		double overallScore = findEdgeMappingScore(0, scoreMatrix, new HashSet<Integer>());
		return overallScore;
	}
	
	*/
	// This is replication of Rebecca's code
	public static double findEdgeMappingScore(Graph testGraph, Graph goldGraph, int edgeMatchType){
		ArrayList<Edge> availableEdges = new ArrayList<Edge>();
		
		// The ellipses are placed at the bottom to maximize better matching.
		// The ncsubj relations are placed at the bottom to match the official RASP evaluation code.
		if(testGraph != null){
			for(Edge e : testGraph.getEdges()){
				if(!e.getHead().getLabel().equals(Graph.ellip.getLabel()) && !e.getDep().getLabel().equals(Graph.ellip.getLabel()) && !e.getLabel().equals("ncsubj"))
					availableEdges.add(e);
			}
			for(Edge e : testGraph.getEdges()){
				if(!e.getLabel().equals("ncsubj") && !availableEdges.contains(e))
					availableEdges.add(e);
			}
			for(Edge e : testGraph.getEdges()){
				if(e.getLabel().equals("ncsubj") && !availableEdges.contains(e))
					availableEdges.add(e);
			}
		}
		
		Edge goldEdge, testEdge, bestTestEdge;
		double testEdgeScore, bestTestEdgeScore, overallScore = 0.0;
		for(int j = 0; j < goldGraph.getEdges().size(); j++){
			goldEdge = goldGraph.getEdges().get(j);
			
			if(goldEdge.getLabel().equals("passive"))
				continue;
			bestTestEdge = null;
			bestTestEdgeScore = -1000;
			for(int k = 0; k < availableEdges.size(); k++){
				testEdge = availableEdges.get(k);
				if(testEdge.getLabel().equals("passive"))
					continue;
				testEdgeScore = edgeMatch(testEdge, goldEdge, edgeMatchType);
				if(testEdgeScore > 0 && testEdgeScore > bestTestEdgeScore){
					bestTestEdgeScore = testEdgeScore;
					bestTestEdge = testEdge;
				}
			}
			if(bestTestEdge != null){
				overallScore += bestTestEdgeScore;
				availableEdges.remove(bestTestEdge);
			}
		}
		return overallScore;
	}
	
	/**
	 * Finds the maximum mapping score to the graph itself. Needed for normalization.
	 * @param graph Graph
	 * @param edgeMatchType Edge mapping type
	 * @return The graph similarity score to itself (sum of the edge similarities)
	 */
	public static double findEdgeMappingScore(Graph graph, int edgeMatchType){
		
		double score = 0.0;
		for(Edge edge : graph.getEdges()){
			if(edge.getLabel().equals("passive"))
				continue;
			score += edgeMatch(edge, edge, edgeMatchType);
		}
		return score;
	}
	
	
	/**
	 * Calculates the precision of test parses against the gold standard parses.
	 * @param testParses Test parses
	 * @param goldParses Gold parses
	 * @param edgeMatchType Edge mapping type
	 * @return Precision
	 */
	public static double getPrecision(ArrayList<Graph> testParses, ArrayList<Graph> goldParses, int edgeMatchType){
		if(testParses.size() != goldParses.size())
			throw new RuntimeException("Mismatching number of parses in ParseEvaluator:getPrecision()");
		
		double edgeMatchSum = 0.0;
		double testSum = 0.0;
		
		for(int i = 0; i < goldParses.size(); i++){
			edgeMatchSum += findEdgeMappingScore(testParses.get(i), goldParses.get(i), edgeMatchType);
			testSum += findEdgeMappingScore(testParses.get(i), edgeMatchType);
		}
		
		double prec = edgeMatchSum / testSum;
		return prec;
	}
	
	public static double getPrecision(Graph testGraph, Graph goldGraph, int edgeMatchType){
		double edgeMatchSum = 0.0;
		double testSum = 0.0;
		
		edgeMatchSum += findEdgeMappingScore(testGraph, goldGraph, edgeMatchType);
		testSum += findEdgeMappingScore(testGraph, edgeMatchType);
		
		double prec = edgeMatchSum / testSum;
		return prec;
	}
	
	
	/**
	 * Calculates the recall of test parses against the gold standard parses.
	 * @param testParses Test parses
	 * @param goldParses Gold parses
	 * @param edgeMatchType Edge mapping type
	 * @return Recall
	 */
	public static double getRecall(ArrayList<Graph> testParses, ArrayList<Graph> goldParses, int edgeMatchType){
		if(testParses.size() != goldParses.size())
			throw new RuntimeException("Mismatching number of parses in ParseEvaluator:getPrecision()");
		
		double edgeMatchSum = 0.0;
		double goldSum = 0.0;
		
		for(int i = 0; i < goldParses.size(); i++){
			edgeMatchSum += findEdgeMappingScore(testParses.get(i), goldParses.get(i), edgeMatchType);
			goldSum += findEdgeMappingScore(goldParses.get(i), edgeMatchType);
		}
		
		double prec = edgeMatchSum / goldSum;
		return prec;
	}
	
	public static double getRecall(Graph testGraph, Graph goldGraph, int edgeMatchType){
		double edgeMatchSum = 0.0;
		double goldSum = 0.0;
		
		edgeMatchSum += findEdgeMappingScore(testGraph, goldGraph, edgeMatchType);
		goldSum += findEdgeMappingScore(goldGraph, edgeMatchType);

		double prec = edgeMatchSum / goldSum;
		return prec;
	}
	
	/**
	 * Calculates the F-Measure of test parses against the gold standard parses.
	 * @param testParses Test parses
	 * @param goldParses Gold parses
	 * @param edgeMatchType Edge mapping type
	 * @return F-measure
	 */
	public static double getFMeasure(ArrayList<Graph> testParses, ArrayList<Graph> goldParses, int edgeMatchType){
		double prec = getPrecision(testParses, goldParses, edgeMatchType);
		double rec = getRecall(testParses, goldParses, edgeMatchType);
		
		return getFMeasure(prec, rec);
	}
	
	public static double getFMeasure(double prec, double rec){
		double fmeasure = 2.0 * prec * rec / (prec + rec);
		if(prec + rec == 0.0)
			fmeasure = 0.0;
		return fmeasure;
	}
	
	public static double getFMeasure(Graph testGraph, Graph goldGraph, int edgeMatchType){
		double prec = getPrecision(testGraph, goldGraph, edgeMatchType);
		double rec = getRecall(testGraph, goldGraph, edgeMatchType);
		return getFMeasure(prec, rec);
	}
	
	

	public static int getCountBest(ArrayList<LinkedHashMap<Graph,Double>>testParses, ArrayList<Graph> goldParses, int grTypeMatch){
		if(testParses.size() != goldParses.size())
			throw new RuntimeException("Mismatch in number of parses at ParseEvaluator.getCountBest(): " + testParses.size() + " " + goldParses.size());
		
		double score, topScore, bestScore;
		int bestTopCount = 0;
		for(int i = 0; i < goldParses.size(); i++){
			if(testParses.get(i) == null)
				continue;
			
			topScore = -1;
			bestScore = -1;
			for(Entry<Graph,Double> e : Tools.sort(testParses.get(i), true).entrySet()){
				score = getFMeasure(e.getKey(), goldParses.get(i), grTypeMatch);
				if(topScore < 0)
					topScore = score;
				if(score > bestScore)
					bestScore = score;
			}
			if(topScore > -1 && topScore == bestScore)
				bestTopCount++;
		}
		return bestTopCount;
	}
	
	public static double getAvgSpearmans(ArrayList<LinkedHashMap<Graph,Double>>testParses, ArrayList<Graph> goldParses, int grTypeMatch){
		if(testParses.size() != goldParses.size())
			throw new RuntimeException("Mismatch in number of parses at ParseEvaluator.getSpearmans(): " + testParses.size() + " " + goldParses.size());
		
		double sum = 0, fscore = 0, correlation = 0;
		for(int i = 0; i < goldParses.size(); i++){
			LinkedHashMap<Graph,Double> parses = Tools.sort(testParses.get(i), true);
			LinkedHashMap<Graph,Double> parsesRated = new LinkedHashMap<Graph,Double>();
			int k = 0;
			for(Graph g : parses.keySet()){
				fscore = getFMeasure(g, goldParses.get(i), grTypeMatch);
				parsesRated.put(g, fscore);
			}
			/*
			// Temp
			for(Entry<Graph,Double> e : testParses.get(i).entrySet())
				System.out.println("X" + i + " : " + e.getValue());
			for(Entry<Graph,Double> e : parses.entrySet())
				System.out.println("Y" + i + " : " + e.getValue());
			for(Entry<Graph,Double> e : parsesRated.entrySet())
				System.out.println("Z" + i + " : " + e.getValue());
			*/
			
			correlation = Tools.spearman(parses, parsesRated);
			
			if(Double.isNaN(correlation))
				correlation = 0.0;
			
			sum += correlation;
		}
		return sum / (double)goldParses.size();
	}
	
	public static ArrayList<LinkedHashMap<Graph,Double>> createUpperBound(ArrayList<LinkedHashMap<Graph,Double>> testGraphs, ArrayList<Graph> goldGraphs, int edgeMatchType){
		if(testGraphs.size() != goldGraphs.size())
			throw new RuntimeException("Graph numbers do not match");
		
		ArrayList<LinkedHashMap<Graph,Double>> upperBoundGraphs = new ArrayList<LinkedHashMap<Graph,Double>>();
		LinkedHashMap<Graph,Double> tempMap;
		for(int i = 0; i < goldGraphs.size(); i++){
			tempMap = new LinkedHashMap<Graph,Double>();
			for(Entry<Graph,Double> e : testGraphs.get(i).entrySet()){
				tempMap.put(e.getKey(), getFMeasure(e.getKey(), goldGraphs.get(i), edgeMatchType));
			}
			tempMap = Tools.sort(tempMap, true);
			upperBoundGraphs.add(tempMap);
		}
		
		return upperBoundGraphs;
	}
	
	
	public static LinkedHashMap<String,Double> run(ArrayList<LinkedHashMap<Graph,Double>> testParses, ArrayList<Graph> goldParses, int edgeMatchType){
		ArrayList<Graph> topTestParses = new ArrayList<Graph>();
		for(LinkedHashMap<Graph,Double> map : testParses){
			if(map.keySet().size() == 0){
				topTestParses.add(new Graph());
			}
			else{
				for(Graph g : Tools.sort(map, true).keySet()){
					topTestParses.add(g);
					break;
				}
			}
		}
		
		LinkedHashMap<String,Double> results = new LinkedHashMap<String,Double>();
		results.put("precision", getPrecision(topTestParses, goldParses, edgeMatchType));
		results.put("recall", getRecall(topTestParses, goldParses, edgeMatchType));
		results.put("fmeasure", getFMeasure(results.get("precision"), results.get("recall")));
		results.put("avgspearmans", getAvgSpearmans(testParses, goldParses, edgeMatchType));
		results.put("best", (double)getCountBest(testParses, goldParses, edgeMatchType));

		return results;
	}
	
	public static ArrayList<Graph> getTopGraphs(ArrayList<LinkedHashMap<Graph,Double>> graphs){
		ArrayList<Graph> topGraphs = new ArrayList<Graph>();
		for(LinkedHashMap<Graph,Double> map : graphs){
			if(map.keySet().size() == 0){
				//topGraphs.add(new Graph());
				throw new RuntimeException("Missing graphs");
			}
			else{
				for(Graph g : Tools.sort(map, true).keySet()){
					topGraphs.add(g);
					break;
				}
			}
		}
		return topGraphs;
	}
	
	public static LinkedHashMap<String,Double> calculateStatisticalSignificance(ArrayList<LinkedHashMap<Graph,Double>> baselineGraphs, ArrayList<LinkedHashMap<Graph,Double>> testGraphs, ArrayList<Graph> goldGraphs, int grTypeMatch){
		LinkedHashMap<String,Double> results = new LinkedHashMap<String,Double>();
		
		ArrayList<Graph> topBaselineGraphs = getTopGraphs(baselineGraphs);
		ArrayList<Graph> topTestGraphs = getTopGraphs(testGraphs);
		
		if(topBaselineGraphs.size() != topTestGraphs.size() || topBaselineGraphs.size() != goldGraphs.size()){
			throw new RuntimeException("Mismatch in parse array sizes in ParseRerank.calculate...()");
		}
		
		// Creating a cache of parse scores
		HashMap<Graph,Double> correctScores = new HashMap<Graph,Double>();
		HashMap<Graph,Double> totalScores = new HashMap<Graph,Double>();
		for(int i = 0; i < goldGraphs.size(); i++){
			if(topBaselineGraphs.get(i) == null || topTestGraphs.get(i) == null)
				throw new RuntimeException("Graph cannot be null");
			correctScores.put(topBaselineGraphs.get(i), findEdgeMappingScore(topBaselineGraphs.get(i), goldGraphs.get(i), grTypeMatch));
			totalScores.put(topBaselineGraphs.get(i), findEdgeMappingScore(topBaselineGraphs.get(i), grTypeMatch));
			correctScores.put(topTestGraphs.get(i), findEdgeMappingScore(topTestGraphs.get(i), goldGraphs.get(i), grTypeMatch));
			totalScores.put(topTestGraphs.get(i), findEdgeMappingScore(topTestGraphs.get(i), grTypeMatch));
			
			totalScores.put(goldGraphs.get(i), findEdgeMappingScore(goldGraphs.get(i), grTypeMatch));
		}
		
		// Calculating realdiff
		double fa, fb, preca, precb, reca, recb, correcta, correctb, totala, totalb, totalgold;
		correcta = correctb = totala = totalb = totalgold = 0.0;
		for(int i = 0; i < goldGraphs.size(); i++){
			correcta += correctScores.get(topBaselineGraphs.get(i));
			correctb += correctScores.get(topTestGraphs.get(i));
			totala += totalScores.get(topBaselineGraphs.get(i));
			totalb += totalScores.get(topTestGraphs.get(i));
			totalgold += totalScores.get(goldGraphs.get(i));
		}
		preca = correcta / totala;
		precb = correctb / totalb;
		reca = correcta / totalgold;
		recb = correctb / totalgold;
		fa = 2.0 * preca *reca / (preca + reca);
		fb = 2.0 * precb *recb / (precb + recb);
		double realDiff = Math.abs(fb-fa);
		
		results.put("fa", fa);
		results.put("fb", fb);
		results.put("realdiff", realDiff);
		
		// Random shuffles
		int c = 0, R = 1000000;
		Random generator = new Random();
		double random, randomDiff;
		
		
		for(int i = 0; i < R; i++){
			correcta = correctb = totala = totalb = totalgold = 0.0;
			for(int j = 0; j < goldGraphs.size(); j++){
				random = generator.nextDouble();
				if(random < 0.5){
					//a.add(baselineParses.get(j));
					//b.add(testParses.get(j));
					correcta += correctScores.get(topBaselineGraphs.get(j));
					correctb += correctScores.get(topTestGraphs.get(j));
					totala += totalScores.get(topBaselineGraphs.get(j));
					totalb += totalScores.get(topTestGraphs.get(j));
				}
				else {
					//a.add(testParses.get(j));
					//b.add(baselineParses.get(j));
					correctb += correctScores.get(topBaselineGraphs.get(j));
					correcta += correctScores.get(topTestGraphs.get(j));
					totalb += totalScores.get(topBaselineGraphs.get(j));
					totala += totalScores.get(topTestGraphs.get(j));
				}
				
				totalgold += totalScores.get(goldGraphs.get(j));
			}
			
			preca = correcta / totala;
			precb = correctb / totalb;
			reca = correcta / totalgold;
			recb = correctb / totalgold;
			fa = 2.0 * preca *reca / (preca + reca);
			fb = 2.0 * precb *recb / (precb + recb);

			randomDiff = Math.abs(fa - fb);
			if(randomDiff >= realDiff){
				c++;
			}
			//System.out.println("Shuffle " + i + " : " + randomDiff);
		}
		
		double p = ((double)c + 1.0)/((double)R + 1.0);
		
		results.put("c", (double)c);
		results.put("iter", (double)R);
		results.put("p", p);
		
		return results;
	}
	
	
	public static String getTypeStatistics(ArrayList<LinkedHashMap<Graph,Double>> testGraphs, ArrayList<Graph> goldGraphs, int edgeMatchType){
		ArrayList<Graph> topTestGraphs = getTopGraphs(testGraphs);
		
		LinkedHashMap<String,Double> sums = new LinkedHashMap<String,Double>();
		LinkedHashMap<String,Double> testSums = new LinkedHashMap<String,Double>();
		LinkedHashMap<String,Double> goldSums = new LinkedHashMap<String,Double>();
		
		if(topTestGraphs.size() != goldGraphs.size())
			throw new RuntimeException("Mismatch in number of graphs: " + topTestGraphs.size() + " " + goldGraphs.size());
		
		for(String type : RaspGrTypeHierarchy.getTypes()){
			sums.put(type, 0.0);
			testSums.put(type, 0.0);
			goldSums.put(type, 0.0);
		}
		
		for(int i = 0; i < goldGraphs.size(); i++){
			Graph testGraph = topTestGraphs.get(i);
			Graph goldGraph = goldGraphs.get(i);
			
			for(Edge e : goldGraph.getEdges()){
				if(e.getLabel().equals("passive"))
					continue;
				for(String t : RaspGrTypeHierarchy.getSubsumed(e.getLabel()))
					goldSums.put(t, goldSums.get(t)+1.0);
			}
			
			ArrayList<Edge> availableEdges = new ArrayList<Edge>();
			
			// The ellipses are placed at the bottom to maximize better matching.
			// The ncsubj relations are placed at the bottom to match the official RASP evaluation code.
			for(Edge e : testGraph.getEdges()){
				if(!e.getHead().getLabel().equals(Graph.ellip.getLabel()) && !e.getDep().getLabel().equals(Graph.ellip.getLabel()) && !e.getLabel().equals("ncsubj"))
					availableEdges.add(e);
			}
			for(Edge e : testGraph.getEdges()){
				if(!e.getLabel().equals("ncsubj") && !availableEdges.contains(e))
					availableEdges.add(e);
			}
			for(Edge e : testGraph.getEdges()){
				if(e.getLabel().equals("ncsubj") && !availableEdges.contains(e))
					availableEdges.add(e);
			}
			for(Edge e : testGraph.getEdges()){
				if(e.getLabel().equals("passive"))
					continue;
				for(String t : RaspGrTypeHierarchy.getSubsumed(e.getLabel()))
					testSums.put(t, testSums.get(t)+1.0);
			}
			
			Edge goldEdge, testEdge, bestTestEdge;
			double testEdgeScore, bestTestEdgeScore, overallScore = 0.0;
			for(int j = 0; j < goldGraph.getEdges().size(); j++){
				goldEdge = goldGraph.getEdges().get(j);
				
				if(goldEdge.getLabel().equals("passive"))
					continue;
				bestTestEdge = null;
				bestTestEdgeScore = Double.MIN_VALUE;
				for(int k = 0; k < availableEdges.size(); k++){
					testEdge = availableEdges.get(k);
					if(testEdge.getLabel().equals("passive"))
						continue;
					testEdgeScore = edgeMatch(testEdge, goldEdge, edgeMatchType);
					if(testEdgeScore > 0 && testEdgeScore > bestTestEdgeScore){
						bestTestEdgeScore = testEdgeScore;
						bestTestEdge = testEdge;
					}
				}
				if(bestTestEdge != null){
					List<String> goldLabelTypes = RaspGrTypeHierarchy.getSubsumed(goldEdge.getLabel());
					List<String> testLabelTypes = RaspGrTypeHierarchy.getSubsumed(bestTestEdge.getLabel());
					
					for(String t : goldLabelTypes){
						if(testLabelTypes.contains(t))
							sums.put(t, sums.get(t)+1.0);
					}
					
					overallScore += bestTestEdgeScore;
					availableEdges.remove(bestTestEdge);
				}
			}
		}
		
		
		ArrayList<String> edgeLabels = new ArrayList<String>(sums.keySet());
		Collections.sort(edgeLabels);
		String results = "Label\tCorrect\tTest\tGold\tPrec\tRec\tFmeasure\n";
		for(String edgeLabel : edgeLabels){
			double prec = sums.get(edgeLabel) / testSums.get(edgeLabel);
			double rec = sums.get(edgeLabel) / goldSums.get(edgeLabel);
			double f = 2 * prec * rec / (prec + rec);
			results += (edgeLabel  + "\t" + sums.get(edgeLabel)  + "\t" + testSums.get(edgeLabel) + "\t" + goldSums.get(edgeLabel) + "\t" + prec + "\t" + rec + "\t" + f);
			results += "\n";
		}
		return results;
	}
}

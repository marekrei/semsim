package sem.apps.parsererank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.model.SemModel;
import sem.util.FileReader;
import sem.util.Tools;

/**
 * Implements various alternative methods for assigning a score to every edge in a graph
 *
 */
public class EdgeScorer {
	
	protected SemModel semModel;
	protected HashMap<String,LinkedHashMap<String,Double>> wordExpansionMap;
	protected int expansionLimit;
	protected String edgeScorerType;
	
	public EdgeScorer(String edgeScorerType, SemModel semModel, String expansionMapPath, int expansionLimit){
		this.semModel = semModel;
		this.wordExpansionMap = null;
		this.expansionLimit = expansionLimit;
		this.edgeScorerType = edgeScorerType;
		if(expansionMapPath != null){
			this.loadExpansionMap(expansionMapPath);
		}
	}
	
	public synchronized void loadExpansionMap(String path){
		if(path == null)
			throw new RuntimeException("Expansion map path cannot be null");
		
		HashMap<String,LinkedHashMap<String,Double>> tempWordExpansionMap = new HashMap<String,LinkedHashMap<String,Double>>();
		FileReader fr = new FileReader(path);
		String line;
		String[] lineParts;
		
		while(fr.hasNext()){
			line = fr.next();
			lineParts = line.trim().split("\\s+");
			LinkedHashMap<String,Double> tMap = new LinkedHashMap<String,Double>();
			
			if((lineParts.length-1)%2 != 0)
				throw new RuntimeException("Abnormal number of items in line: " + lineParts.length + " : " + line);
			
			for(int i = 1; i < lineParts.length; i += 2){
				tMap.put(lineParts[i], Tools.getDouble(lineParts[i+1], 0.0));
			}
			tempWordExpansionMap.put(lineParts[0], Tools.sort(tMap, true));
		}
		this.wordExpansionMap = tempWordExpansionMap;
	}
	
	public static String convertToString(String headLabel, String edgeLabel, String depLabel){
		return (edgeLabel!=null?edgeLabel:"!!NULL!!") + "\t" + (headLabel!=null?headLabel:"!!NULL!!") + "\t" + (depLabel!=null?depLabel:"!!NULL!!");
	}
	
	public static String convertToString(Edge edge){
		return convertToString(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
	}
	
	public double getEdgeCount(String headLabel, String edgeLabel, String depLabel){
		return this.semModel.getTripleCount(headLabel, edgeLabel, depLabel);
	}
	
	public double getItemCount(String label){
		return this.semModel.getNodeCount(label);
	}

	public double getCooccurrenceCount(String label1, String label2){
		return this.semModel.getLocationMatchCount(label1, label2);
	}
	
	public double getTotalEdgeCount(){
		return this.semModel.getTotalEdgeCount();
	}
	
	public double getTotalNodeCount(){
		return this.semModel.getTotalNodeCount();
	}
		
	public LinkedHashMap<String,Double> getSimilarWords(String w){
		if(this.wordExpansionMap == null)
			throw new RuntimeException("Expansion map cannot be null");
		if(!this.wordExpansionMap.containsKey(w))
			return new LinkedHashMap<String,Double>();
		return wordExpansionMap.get(w);
	}
	
	//
	// Edge scoring functions start here
	//
	
	public LinkedHashMap<Edge,Double> i(LinkedHashMap<Graph,Double> parses){
		double score, prob1, prob2, prob3;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				prob1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) / getEdgeCount(null, null, null);
				prob2 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), null) / getEdgeCount(null, null, null);
				prob3 = getEdgeCount(null, null, edge.getDep().getLabel()) / getEdgeCount(null, null, null);
				if(prob2 == 0.0 || prob3 == 0.0)
					score = 0.0;
				else
					score = prob1 / (prob2 * prob3);
				//score = Math.log(score);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores; 
	}
	
	public LinkedHashMap<Edge,Double> res(LinkedHashMap<Graph,Double> parses){
		
		String edgeString;
		HashMap<String,Double> edgeScoresTemp = new HashMap<String,Double>();
		double parseScore, total = 0.0;
		HashSet<String> observedEdges = new HashSet<String>();
		
		int parseNum = 0;
		for(Entry<Graph,Double> e : parses.entrySet()){
			parseNum++;
			observedEdges.clear();
			parseScore = ((double)1.0 / (double)(parseNum));
			
			for(Edge edge : e.getKey().getEdges()){
				edgeString = convertToString(edge);
				if(!observedEdges.contains(edgeString)){
					observedEdges.add(edgeString);
					edgeScoresTemp.put(edgeString, (edgeScoresTemp.containsKey(edgeString)?edgeScoresTemp.get(edgeString):0.0) + parseScore);
				}
			}
			
			total += parseScore;
		}
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge)) / total));
			}
		}
		return edgeScores;
	}
	
	public double ces1(String head, String relation, String dep){
		double N = getTotalNodeCount();
		double K = getTotalEdgeCount();
		double score1 = getEdgeCount(head, relation, dep) / K;
		double score2 = (getItemCount(head) / N) *  (getItemCount(dep) / N);
		if(score2 != 0.0)
			return (score1 / score2);
		return 0.0;
	}
	
	public LinkedHashMap<Edge,Double> ces1(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, ces1(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()));
			}
		}
		return edgeScores;
	}
	
	public double ces2(String head, String relation, String dep){
		double score1 = getEdgeCount(head, relation, dep);
		double score2 = getCooccurrenceCount(head, dep);
		if(score2 != 0.0)
			return (score1 / score2);
		return 0.0;
	}
	
	public LinkedHashMap<Edge,Double> ces2(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, ces2(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()));
			}
		}
		return edgeScores;
	}
	
	public double eces(String head, String relation, String dep, String method, boolean includeMainWord, double lambda){
		double score = 0.0, sum, weightSum, weight;
		int count;
		
		LinkedHashMap<String,Double> substitutes;
		
		// Replacing head
		substitutes = getSimilarWords(head);
		if(includeMainWord)
			substitutes.put(head, 1.0);
		else
			substitutes.remove(head);

		sum = 0.0;
		weightSum = 0.0;
		count = 0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			
			if(method.equalsIgnoreCase("ces1"))
				sum += weight * ces1(substitute.getKey(), relation, dep);
			else if(method.equalsIgnoreCase("ces2"))
				sum += weight * ces2(substitute.getKey(), relation, dep);
			else
				throw new RuntimeException("Unknown method: " + method);
			
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum;
		
		
		// Replacing dependent
		substitutes = getSimilarWords(dep);
		if(includeMainWord)
			substitutes.put(dep, 1.0);
		else
			substitutes.remove(dep);

		sum = 0.0;
		weightSum = 0.0;
		count=0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			
			if(method.equalsIgnoreCase("ces1"))
				sum += weight * ces1(head, relation, substitute.getKey());
			else if(method.equalsIgnoreCase("ces2"))
				sum += weight * ces2(head, relation, substitute.getKey());
			else
				throw new RuntimeException("Unknown method: " + method);
			
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum;
		
		score = score /2.0;
		return score;
	}
	
	public LinkedHashMap<Edge,Double> eces1(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, eces(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), "ces1", true, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> eces2(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, eces(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), "ces2", true, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> cmb1(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1(parses);
		LinkedHashMap<Edge,Double> c = ces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)3.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> cmb2(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = eces1(parses);
		LinkedHashMap<Edge,Double> c = eces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)3.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public LinkedHashMap<Edge,Double> run(LinkedHashMap<Graph,Double> parses){
		java.lang.reflect.Method method;
		String methodName = null;
		try{
			methodName = this.edgeScorerType.toLowerCase();
			method = EdgeScorer.class.getDeclaredMethod(methodName, parses.getClass());
			LinkedHashMap<Edge,Double> scores = (LinkedHashMap<Edge,Double>)method.invoke(this, parses);
			for(Double score : scores.values()){
				if(score.isNaN())
					throw new RuntimeException("Edge score is NaN");
				if(score.isInfinite())
					throw new RuntimeException("Edge score is infinite");
			} 
			
			return scores;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}

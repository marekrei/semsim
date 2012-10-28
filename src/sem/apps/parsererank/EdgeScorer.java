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
	
	public LinkedHashMap<String,Double> getSimilarWords(String w){
		if(this.wordExpansionMap == null)
			throw new RuntimeException("Expansion map cannot be null");
		if(!this.wordExpansionMap.containsKey(w))
			return new LinkedHashMap<String,Double>();
		return wordExpansionMap.get(w);
	}
	
	// Edge scoring functions
	
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
	
	public static String convertToString(Edge edge, Graph graph){
		String headLabel = edge.getHead().getLabel();
		if(!headLabel.equals(Graph.nil.getLabel()) && !headLabel.equals(Graph.ellip.getLabel()))
			headLabel += ":" + graph.getNodes().indexOf(edge.getHead());
		
		String depLabel = edge.getDep().getLabel();
		if(!depLabel.equals(Graph.nil.getLabel()) && !depLabel.equals(Graph.ellip.getLabel()))
			depLabel += ":" + graph.getNodes().indexOf(edge.getDep());
		
		return convertToString(headLabel, edge.getLabel(), depLabel);
	}
	
	/*
	public LinkedHashMap<Edge,Double> res_new(LinkedHashMap<Graph,Double> parses){
		boolean normalise = true;
		int parseNum = 1;
		String edgeString;
		HashMap<String,Double> edgeScoresTemp = new HashMap<String,Double>();
		double parseScore, max = 0.0;
		
		for(Entry<Graph,Double> e : Tools.sort(parses, true).entrySet()){			
			parseScore = ((double)1.0 / (double)(parseNum));
			for(Edge edge : e.getKey().getEdges()){
				edgeString = convertToString(edge, e.getKey());
				edgeScoresTemp.put(edgeString, (edgeScoresTemp.containsKey(edgeString)?edgeScoresTemp.get(edgeString):0.0) + parseScore);
			}
			max += parseScore;
			parseNum++;
		}
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				if(normalise)
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge, e.getKey())) / max));
				else
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge, e.getKey()))));
			}
		}
		return edgeScores;
	}
	*/
	
	public LinkedHashMap<Edge,Double> res(LinkedHashMap<Graph,Double> parses){
		boolean normalise = true;
		int parseNum = 1;
		String edgeString;
		HashMap<String,Double> edgeScoresTemp = new HashMap<String,Double>();
		double parseScore, max = 0.0;
		HashSet<String> edgs = new HashSet<String>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){			
			parseScore = ((double)1.0 / (double)(parseNum));
			edgs.clear();
			for(Edge edge : e.getKey().getEdges()){
				edgeString = convertToString(edge);
				if(!edgs.contains(edgeString)){
					edgs.add(edgeString);
					edgeScoresTemp.put(edgeString, (edgeScoresTemp.containsKey(edgeString)?edgeScoresTemp.get(edgeString):0.0) + parseScore);
				}
			}
			
			max += parseScore;
			parseNum++;
		}
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				if(normalise)
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge)) / max));
				else
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge))));
			}
		}
		return edgeScores;
	}
	
	
	public LinkedHashMap<Edge,Double> res_orig(LinkedHashMap<Graph,Double> parses){
		boolean normalise = true;
		int parseNum = 1;
		String edgeString;
		HashMap<String,Double> edgeScoresTemp = new HashMap<String,Double>();
		double parseScore, max = 0.0;
		
		for(Entry<Graph,Double> e : parses.entrySet()){			
			parseScore = ((double)1.0 / (double)(parseNum));
			for(Edge edge : e.getKey().getEdges()){
				edgeString = convertToString(edge);
				edgeScoresTemp.put(edgeString, (edgeScoresTemp.containsKey(edgeString)?edgeScoresTemp.get(edgeString):0.0) + parseScore);
			}
			max += parseScore;
			parseNum++;
		}
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				if(normalise)
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge)) / max));
				else
					edgeScores.put(edge, (edgeScoresTemp.get(convertToString(edge))));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> ces1(LinkedHashMap<Graph,Double> parses){
		double N = semModel.getTotalNodeCount();
		//double K = semModel.getLocationMatchCount(null, null);
		double K = semModel.getTotalEdgeCount();
		//double K = 6.335079062E9;
		// TODO: Look into this
		
		double score, score1, score2;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				score1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) / K;
				score2 = getItemCount(edge.getHead().getLabel()) *  getItemCount(edge.getDep().getLabel()) / (N*N);
				if(score2 == 0.0)
					score = 0.0;
				else
					score = (score1 / score2);
				//System.out.println(edge.toString() + " :\t" + getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) + "\t" + getItemCount(edge.getHead().getLabel()) + "\t" + getItemCount(edge.getDep().getLabel()) + "\t" + N + "\t" + K + "\t: " + score);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> ces2(LinkedHashMap<Graph,Double> parses){
		double score, score1, score2;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				score1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				score2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				if(score2 == 0.0)
					score = 0.0;
				else
					score = (score1 / score2);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> eces1(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES1");
		
		double score = 0.0;
		double countW1, countW2, weightedSum, weightSum, count, countSub, countWi, weight;
		//double K = semSim.getValueMap().get("imaginary_count_edges");
		//double N = semSim.getValueMap().get("total_count_nodes");
		double K = this.semModel.getTotalEdgeCount();
		double N = this.semModel.getTotalNodeCount();
		//double K = semModel.getLocationMatchCount(null, null);
		//double K = 6.335079062E9;
		// TODO: Look into this
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				score = 0.0;
				
				countW1 = getItemCount(edge.getHead().getLabel());
				countW2 = getItemCount(edge.getDep().getLabel());
				
				weightedSum = 0.0; weightSum = 0.0; count = 0.0;
				if(wordExpansionMap.containsKey(edge.getHead().getLabel()))
				{
					for(Entry<String,Double> substitute : getSimilarWords(edge.getHead().getLabel()).entrySet()){
						if(count >= this.expansionLimit)
							break;
						countSub = getEdgeCount(substitute.getKey(), edge.getLabel(), edge.getDep().getLabel());
						countWi = getItemCount(substitute.getKey());
						weight = substitute.getValue();

						if(countWi*countW2 != 0.0)
							weightedSum += weight * (countSub / K) / ((countWi/N) * (countW2 / N));
	
						weightSum += weight;
						count++;
					}
				}

				if(weightSum != 0.0)
					score += weightedSum / weightSum;

				weightedSum = 0.0; weightSum = 0.0; count = 0.0;
				if(wordExpansionMap.containsKey(edge.getDep().getLabel()))
				{
					for(Entry<String,Double> substitute : getSimilarWords(edge.getDep().getLabel()).entrySet()){
						if(count >= this.expansionLimit)
							break;
						
						countSub = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), substitute.getKey());
						countWi = getItemCount(substitute.getKey());
						weight = substitute.getValue();

						if(countW1 * countWi != 0.0)
							weightedSum += weight * (countSub / K) / ((countW1 / N) * (countWi/N));
	
						weightSum += weight;
						count++;
					}
				}
	
				if(weightSum != 0.0)
					score += weightedSum / weightSum;

				score /= 2.0;
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> eces2(LinkedHashMap<Graph,Double> parses){
		if(this.wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES2");
		
		double score = 0.0;
		double countCooc, weightedSum, weightSum, count, countSub, countWi, weight;
		//double K = semSim.getValueMap().get("imaginary_count_edges");
		//double N = semSim.getValueMap().get("total_count_nodes");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				score = 0.0;
				weightedSum = 0.0; weightSum = 0.0; count = 0.0;
				if(this.wordExpansionMap.containsKey(edge.getHead().getLabel())){
					for(Entry<String,Double> substitute : getSimilarWords(edge.getHead().getLabel()).entrySet()){
						if(count >= this.expansionLimit)
							break;
						
						countSub = getEdgeCount(substitute.getKey(), edge.getLabel(), edge.getDep().getLabel());
						countCooc = getCooccurrenceCount(substitute.getKey(), edge.getDep().getLabel());
						weight = substitute.getValue();

						if(countCooc != 0.0)
							weightedSum += weight * (countSub / countCooc);
	
						weightSum += weight;
						count++;
					}
				}

				if(weightSum != 0.0)
					score += weightedSum / weightSum;

				weightedSum = 0.0; weightSum = 0.0; count = 0.0;
				if(this.wordExpansionMap.containsKey(edge.getDep().getLabel())){
					for(Entry<String,Double> substitute : getSimilarWords(edge.getDep().getLabel()).entrySet()){
						if(count >= this.expansionLimit)
							break;
						
						countSub = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), substitute.getKey());
						countCooc = getCooccurrenceCount(edge.getHead().getLabel(), substitute.getKey());
						weight = substitute.getValue();

						if(countCooc != 0.0)
							weightedSum += weight * (countSub / countCooc);
	
						weightSum += weight;
						count++;
					}
				}
	
				if(weightSum != 0.0)
					score += weightedSum / weightSum;

				score /= 2.0;
				edgeScores.put(edge, score);
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
		LinkedHashMap<Edge,Double> b = ces1(parses);
		LinkedHashMap<Edge,Double> c = ces2(parses);
		LinkedHashMap<Edge,Double> d = eces1(parses);
		LinkedHashMap<Edge,Double> e = eces2(parses);
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey())
					* d.get(entry.getKey())
					* e.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)5.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	
	//////////////////////START
	public LinkedHashMap<Edge,Double> ces1s1(LinkedHashMap<Graph,Double> parses){
		double N = semModel.getTotalNodeCount();
		//double K = semModel.getLocationMatchCount(null, null);
		double K = semModel.getTotalEdgeCount();
		//double K = 6.335079062E9;
		double KS = semModel.getTripleTypeCount();
		double NS = semModel.getNodeIndex().size();
		// TODO: Look into this
		
		double score, prob1, prob2, prob3;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				prob1 = (getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) + 1.0) / (K + KS);
				prob2 = (getItemCount(edge.getHead().getLabel()) + 1.0) / (N + NS);
				prob3 = (getItemCount(edge.getDep().getLabel()) + 1.0) / (N + NS);
				if(prob2 == 0.0 || prob3 == 0.0)
					score = 0.0;
				else
					score = prob1 / (prob2 * prob3);
				//System.out.println(edge.toString() + " :\t" + getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) + "\t" + getItemCount(edge.getHead().getLabel()) + "\t" + getItemCount(edge.getDep().getLabel()) + "\t" + N + "\t" + K + "\t: " + score);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> ces2s1(LinkedHashMap<Graph,Double> parses){
	
		double score, score1, score2;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				score1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()) + 1.0;
				score2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel()) + 1.0;
				if(score2 == 0.0)
					score = 0.0;
				else
					score = (score1 / score2);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2b1(LinkedHashMap<Graph,Double> parses){
		
		double score, c1, c2, c3, c4;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				if(c2 == 0.0){
					c3 = getEdgeCount(null, edge.getLabel(), null);
					c4 = getEdgeCount(null, null, null);
					if(c4 == 0.0)
						score = 0.0;
					else score = c3 / c4;
				}
				else
					score = c1 / c2;
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2b2(LinkedHashMap<Graph,Double> parses){
		
		double score, c1, c2;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				if(c2 == 0.0){
					c1 = getEdgeCount(null, edge.getLabel(), edge.getDep().getLabel());
					c2 = getCooccurrenceCount(null, edge.getDep().getLabel());
					if(c2 == 0.0){
						c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), null);
						c2 = getCooccurrenceCount(edge.getHead().getLabel(), null);
						if(c2 == 0.0){
							c1 = getEdgeCount(null, edge.getLabel(), null);
							c2 = getCooccurrenceCount(null, null);
							if(c2 == 0.0)
								score = 0.0;
							else
								score = c1 / c2;
						}
						else
							score = c1 / c2;
					}
					else
						score = c1 / c2;
				}
				else
					score = c1 / c2;
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2ix(LinkedHashMap<Graph,Double> parses, double d){
		
		double score, c1, c2, p1, p2, p3, p4;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				p1 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(null, edge.getDep().getLabel());
				p2 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), null);
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), null);
				p3 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), null);
				c2 = getCooccurrenceCount(null, null);
				p4 = c2==0.0?0.0:(c1/c2);
				
				score = d*p1 + (1.0-d)*(d*p2 + (1.0-d)*(d*p3 + (1.0-d)*(p4)));
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2iz(LinkedHashMap<Graph,Double> parses, double d){
		
		double score, c1, c2, p1, p2, p3, p4;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				p1 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(null, edge.getDep().getLabel());
				p2 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), null);
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), null);
				p3 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), null);
				c2 = getCooccurrenceCount(null, null);
				p4 = c2==0.0?0.0:(c1/c2);
				
				score = d*p1 + (1.0-d)*((p2 + p3 + p4)/3.0);
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2iy(LinkedHashMap<Graph,Double> parses, double d){
		
		double score, c1, c2, p1, p2, p3, p4;
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
				p1 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), edge.getDep().getLabel());
				c2 = getCooccurrenceCount(null, edge.getDep().getLabel());
				p2 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), null);
				c2 = getCooccurrenceCount(edge.getHead().getLabel(), null);
				p3 = c2==0.0?0.0:(c1/c2);
				
				c1 = getEdgeCount(null, edge.getLabel(), null);
				c2 = getCooccurrenceCount(null, null);
				p4 = c2==0.0?0.0:(c1/c2);
				
				score = d*p1 + (1.0-d)*(Math.pow(p2*p3*p4, 1.0/3.0));
				edgeScores.put(edge, score);
			}
		}
		return edgeScores;
	} 
	
	public LinkedHashMap<Edge,Double> ces2i1(LinkedHashMap<Graph,Double> parses){
		return ces2ix(parses, 0.5);
	}
	
	public LinkedHashMap<Edge,Double> ces2i2(LinkedHashMap<Graph,Double> parses){
		return ces2ix(parses, 0.7);
	}
	
	public LinkedHashMap<Edge,Double> ces2i3(LinkedHashMap<Graph,Double> parses){
		return ces2ix(parses, 0.75);
	}
	
	public LinkedHashMap<Edge,Double> ces2i4(LinkedHashMap<Graph,Double> parses){
		return ces2ix(parses, 0.8);
	}
	
	public LinkedHashMap<Edge,Double> ces2i5(LinkedHashMap<Graph,Double> parses){
		return ces2ix(parses, 0.9);
	}
	
	public LinkedHashMap<Edge,Double> ces2i6(LinkedHashMap<Graph,Double> parses){
		return ces2iy(parses, 0.9);
	}
	
	public LinkedHashMap<Edge,Double> ces2i7(LinkedHashMap<Graph,Double> parses){
		return ces2iz(parses, 0.9);
	}
	
	public LinkedHashMap<Edge,Double> ces2b3(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces2(parses);
		LinkedHashMap<Edge,Double> b = res(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		Edge edge;
		for(Entry<Edge,Double> entry : a.entrySet()){
			edge  = entry.getKey();
			double m = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
			double val = 0.0;
			if(m > 0.0)
				val = a.get(entry.getKey());
			else
				val = b.get(entry.getKey());
			res.put(entry.getKey(), val);
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces2b4(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces2(parses);
		LinkedHashMap<Edge,Double> b = eces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		Edge edge;
		for(Entry<Edge,Double> entry : a.entrySet()){
			edge  = entry.getKey();
			double m = getCooccurrenceCount(edge.getHead().getLabel(), edge.getDep().getLabel());
			double val = 0.0;
			if(m > 0.0)
				val = a.get(entry.getKey());
			else
				val = b.get(entry.getKey());
			res.put(entry.getKey(), val);
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces1b4(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces1(parses);
		LinkedHashMap<Edge,Double> b = eces1(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		Edge edge;
		for(Entry<Edge,Double> entry : a.entrySet()){
			edge  = entry.getKey();
			double m = getItemCount(edge.getHead().getLabel()) *  getItemCount(edge.getDep().getLabel());
			double val = 0.0;
			if(m > 0.0)
				val = a.get(entry.getKey());
			else
				val = b.get(entry.getKey());
			res.put(entry.getKey(), val);
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces2b5(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces2(parses);
		LinkedHashMap<Edge,Double> b = eces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		Edge edge;
		for(Entry<Edge,Double> entry : a.entrySet()){
			edge  = entry.getKey();
			double m = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
			double val = 0.0;
			if(m > 0.0)
				val = a.get(entry.getKey());
			else
				val = b.get(entry.getKey());
			res.put(entry.getKey(), val);
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces1b5(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces1(parses);
		LinkedHashMap<Edge,Double> b = eces1(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		Edge edge;
		for(Entry<Edge,Double> entry : a.entrySet()){
			edge  = entry.getKey();
			double m = getEdgeCount(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel());
			double val = 0.0;
			if(m > 0.0)
				val = a.get(entry.getKey());
			else
				val = b.get(entry.getKey());
			res.put(entry.getKey(), val);
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces2c1(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces2(parses);
		LinkedHashMap<Edge,Double> b = eces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			res.put(entry.getKey(), Math.sqrt(a.get(entry.getKey()) * b.get(entry.getKey())));
		}
		return res;
	} 
	
	public LinkedHashMap<Edge,Double> ces1c1(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces1(parses);
		LinkedHashMap<Edge,Double> b = eces1(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			res.put(entry.getKey(), Math.sqrt(a.get(entry.getKey()) * b.get(entry.getKey())));
		}
		return res;
	} 

	
	public LinkedHashMap<Edge,Double> cmb3(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1b4(parses);
		LinkedHashMap<Edge,Double> c = ces2b4(parses);
		LinkedHashMap<Edge,Double> d = eces1(parses);
		LinkedHashMap<Edge,Double> e = eces2(parses);
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey())
					* d.get(entry.getKey())
					* e.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)5.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	
	public LinkedHashMap<Edge,Double> cmb4(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1b4(parses);
		LinkedHashMap<Edge,Double> c = ces2b4(parses);
		
		
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
	
	public LinkedHashMap<Edge,Double> cmb5(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces2b4(parses);
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
	
	public LinkedHashMap<Edge,Double> cmb6(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces2(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)2.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> cmb7(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces2b4(parses);
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)2.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	/////////////////////////////
	
	private double _ces1(String head, String relation, String dep){
		double N = semModel.getTotalNodeCount();
		double K = semModel.getTotalEdgeCount();
		double score1 = getEdgeCount(head, relation, dep) / K;
		double score2 = getItemCount(head) *  getItemCount(dep) / (N*N);
		if(score2 != 0.0)
			return (score1 / score2);
		return 0.0;
	}
	
	public LinkedHashMap<Edge,Double> ces1new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _ces1(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()));
			}
		}
		return edgeScores;
	}
	
	private double _ces2(String head, String relation, String dep){
		double score1 = getEdgeCount(head, relation, dep);
		double score2 = getCooccurrenceCount(head, dep);
		if(score2 != 0.0)
			return (score1 / score2);
		return 0.0;
	}
	
	public LinkedHashMap<Edge,Double> ces2new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _ces2(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel()));
			}
		}
		return edgeScores;
	} 
	
	private double _eces1(String head, String relation, String dep, boolean nomain, double lambda){
		double score = 0.0, sum, weightSum, weight;
		int count;
		
		LinkedHashMap<String,Double> substitutes;
		
		// Replacing head
		substitutes = getSimilarWords(head);
		if(nomain)
			substitutes.remove(head);
		else
			substitutes.put(head, 1.0);
		sum = 0.0;
		weightSum = 0.0;
		count = 0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			sum += weight * _ces1(substitute.getKey(), relation, dep);
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum;
		
		
		// Replacing dep
		substitutes = getSimilarWords(dep);
		if(nomain)
			substitutes.remove(dep);
		else
			substitutes.put(dep, 1.0);
		sum = 0.0;
		weightSum = 0.0;
		count=0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			sum += weight * _ces1(head, relation, substitute.getKey());
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum;
		
		score = score /2.0;
		return score;
	}
	
	public LinkedHashMap<Edge,Double> eces1new(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES1");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _eces1(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), false, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> neces1new(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES1");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _eces1(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), true, 1.0));
			}
		}
		return edgeScores;
	}
	
	private double _eces2(String head, String relation, String dep, boolean nomain, double lambda){
		double score = 0.0, sum, weightSum, weight;
		int count;
		
		LinkedHashMap<String,Double> substitutes;
		
		// Replacing head
		substitutes = getSimilarWords(head);
		if(nomain)
			substitutes.remove(head);
		else
			substitutes.put(head, 1.0);
		sum = 0.0;
		weightSum = 0.0;
		count = 0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			sum += weight * _ces2(substitute.getKey(), relation, dep);
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum;
		
		
		// Replacing dep
		substitutes = getSimilarWords(dep);
		if(nomain)
			substitutes.remove(dep);
		else
			substitutes.put(dep, 1.0);
		sum = 0.0;
		weightSum = 0.0;
		count=0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			sum += weight * _ces2(head, relation, substitute.getKey());
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score += sum / weightSum; 
		
		score = score /2.0;
		return score;
	}
	
	public LinkedHashMap<Edge,Double> eces2new(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES1");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _eces2(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), false, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> neces2new(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded in ECES1");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _eces2(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), true, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> cmb1new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1new(parses);
		LinkedHashMap<Edge,Double> c = ces2new(parses);
		
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
	
	public LinkedHashMap<Edge,Double> cmb2new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = eces1new(parses);
		LinkedHashMap<Edge,Double> c = eces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)3.0));
			res.put(entry.getKey(), val);
			/*
			if(a.get(entry.getKey()) > 1.0)
				System.out.println("TOOBIG RES: " + a.get(entry.getKey()));
			else if(b.get(entry.getKey()) > 1.0)
				System.out.println("TOOBIG ECES1NEW: " + b.get(entry.getKey()));
			else if(c.get(entry.getKey()) > 1.0)
				System.out.println("TOOBIG ECES2NEW: " + c.get(entry.getKey()));
			else if(val > 1.0)
				System.out.println("TOOBIG CMB2NEW: " + val);
				*/
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> cmb3new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1new(parses);
		LinkedHashMap<Edge,Double> c = ces2new(parses);
		LinkedHashMap<Edge,Double> d = eces2new(parses);
		LinkedHashMap<Edge,Double> e = eces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey())
					* c.get(entry.getKey())
					* d.get(entry.getKey())
					* e.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)5.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> cmb4new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces1new(parses);
		LinkedHashMap<Edge,Double> c = ces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					+ b.get(entry.getKey())
					+ c.get(entry.getKey());
			val = val / 3.0;
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> cmb5new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = eces1new(parses);
		LinkedHashMap<Edge,Double> c = eces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					+ b.get(entry.getKey())
					+ c.get(entry.getKey());
			val = val / 3.0;
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> ces1new1(LinkedHashMap<Graph,Double> parses){
		double lambda = 0.5;
		LinkedHashMap<Edge,Double> a = ces1new(parses);
		LinkedHashMap<Edge,Double> b = eces1new(parses);
		
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = Math.pow(a.get(entry.getKey()), lambda)
					* Math.pow(b.get(entry.getKey()), 1.0 - lambda);
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> ces2new1(LinkedHashMap<Graph,Double> parses){
		double lambda = 0.5;
		LinkedHashMap<Edge,Double> a = ces2new(parses);
		LinkedHashMap<Edge,Double> b = eces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = Math.pow(a.get(entry.getKey()), lambda)
					* Math.pow(b.get(entry.getKey()), 1.0 - lambda);
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> ces1new2(LinkedHashMap<Graph,Double> parses){
		double lambda = 0.5;
		LinkedHashMap<Edge,Double> a = ces1new(parses);
		LinkedHashMap<Edge,Double> b = neces1new(parses);
		
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = Math.pow(a.get(entry.getKey()), lambda)
					* Math.pow(b.get(entry.getKey()), 1.0 - lambda);
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	public LinkedHashMap<Edge,Double> ces2new2(LinkedHashMap<Graph,Double> parses){
		double lambda = 0.5;
		LinkedHashMap<Edge,Double> a = ces2new(parses);
		LinkedHashMap<Edge,Double> b = neces2new(parses);
		
		
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = Math.pow(a.get(entry.getKey()), lambda)
					* Math.pow(b.get(entry.getKey()), 1.0 - lambda);
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	private double _geces2(String head, String relation, String dep, boolean nomain, double lambda){
		double score = 1.0, prod, weightSum, weight;
		int count;
		
		LinkedHashMap<String,Double> substitutes;
		
		// Replacing head
		substitutes = getSimilarWords(head);
		if(nomain)
			substitutes.remove(head);
		else
			substitutes.put(head, 1.0);
		
		prod = 1.0;
		weightSum = 0.0;
		count = 0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			prod *= Math.pow(_ces2(substitute.getKey(), relation, dep),weight);
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score *= Math.pow(prod, 1.0/weightSum);
		else
			score *= 0.0;
		
		// Replacing dep
		substitutes = getSimilarWords(dep);
		if(nomain)
			substitutes.remove(dep);
		else
			substitutes.put(dep, 1.0);
		prod = 1.0;
		weightSum = 0.0;
		count=0;
		for(Entry<String,Double> substitute : Tools.sort(substitutes, true).entrySet()){
			if(count >= this.expansionLimit)
				break;
			weight = Math.pow(substitute.getValue(), lambda);
			prod *= Math.pow(_ces2(head, relation, substitute.getKey()), weight);
			weightSum += weight;
			count++;
		}
		if(weightSum > 0.0)
			score *= Math.pow(prod, 1.0/weightSum);
		else
			score *= 0.0;
		
		score = Math.pow(score, 1.0 /2.0);
		return score;
	}
	
	public LinkedHashMap<Edge,Double> geces2new(LinkedHashMap<Graph,Double> parses){
		if(wordExpansionMap == null)
			throw new RuntimeException("Error: the expansion map hasn't been loaded");
		
		LinkedHashMap<Edge,Double> edgeScores = new LinkedHashMap<Edge,Double>();
		
		for(Entry<Graph,Double> e : parses.entrySet()){						
			for(Edge edge : e.getKey().getEdges()){
				edgeScores.put(edge, _geces2(edge.getHead().getLabel(), edge.getLabel(), edge.getDep().getLabel(), false, 1.0));
			}
		}
		return edgeScores;
	}
	
	public LinkedHashMap<Edge,Double> cmb6new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = res(parses);
		LinkedHashMap<Edge,Double> b = ces2new(parses);
		LinkedHashMap<Edge,Double> c = eces2new(parses);
		
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
	
	
	public LinkedHashMap<Edge,Double> cmb7new(LinkedHashMap<Graph,Double> parses){
		LinkedHashMap<Edge,Double> a = ces2new(parses);
		LinkedHashMap<Edge,Double> b = eces2new(parses);
		
		LinkedHashMap<Edge,Double> res = new LinkedHashMap<Edge,Double>();
		for(Entry<Edge,Double> entry : a.entrySet()){
			double val = a.get(entry.getKey())
					* b.get(entry.getKey());
			val = Math.pow(val, (double)((double)1.0/(double)2.0));
			res.put(entry.getKey(), val);
		}
		return res;
	}
	
	/////////////////////////////END
	
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

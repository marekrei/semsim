package sem.apps.parsererank;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

class ParseScorerTask implements Runnable{
	private LinkedHashMap<Graph,Double> parses;
	private EdgeScorer edgeScorer;
	private LinkedHashMap<Graph,Double> scoredParses;
	private int combineMethod;
	
	public ParseScorerTask(LinkedHashMap<Graph,Double> parses, LinkedHashMap<Graph,Double> scoredParses, EdgeScorer edgeScorer, int combineMethod){
		this.parses = parses;
		this.scoredParses = scoredParses;
		this.edgeScorer = edgeScorer;
		this.combineMethod = combineMethod;
	}
	
	@Override
	public void run() {
		// First we populate the edge scores
		LinkedHashMap<Edge,Double> edgeScores = edgeScorer.run(parses);

		// Next we combine the edge scores into a parse score
		double score = 0.0, sum = 0.0, prod = 1.0;
		for(Graph parse : parses.keySet()){
			sum = 0.0; prod = 1.0; score = 0.0;
			for(Edge edge : parse.getEdges()){
				sum += edgeScores.get(edge);
				prod *= edgeScores.get(edge);
			}
			
			if(combineMethod == ParseScorer.COMBINE_SUM)
				score = sum;
			else if(combineMethod == ParseScorer.COMBINE_PROD)
				score = prod;
			else if(combineMethod == ParseScorer.COMBINE_AVG)
				score = sum / (double)(parse.getEdges().size());
			else if(combineMethod == ParseScorer.COMBINE_GEO)
				score = Math.pow(prod, 1.0/(double)(parse.getEdges().size()));
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG1){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					if(node.getLabel().equals(Graph.ellip.getLabel()) || node.getLabel().equals(Graph.nil.getLabel()))
						continue;
					nodeScore = 0.0;
					nodeCount = 0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node){
							nodeScore += edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(nodeScore/nodeCount):0.0;
					
					totalNodeCount++;
				}
				if(totalNodeCount > 0)
					score = nodeSum / (double)totalNodeCount;
				else
					score = 0.0;
			}
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG2){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					if(node.getLabel().equals(Graph.ellip.getLabel()) || node.getLabel().equals(Graph.nil.getLabel()))
						continue;
					nodeScore = 1.0;
					nodeCount = 0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node){
							nodeScore *= edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(Math.pow(nodeScore, 1.0/(double)nodeCount)):0.0;
					totalNodeCount++;
				}
				if(totalNodeCount > 0)
					score = nodeSum / (double)totalNodeCount;
				else
					score = 0.0;
			}
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG3){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					if(node.getLabel().equals(Graph.ellip.getLabel()) || node.getLabel().equals(Graph.nil.getLabel()))
						continue;
					nodeScore = 0.0;
					nodeCount = 0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node || edge.getHead() == node){
							nodeScore += edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(nodeScore/nodeCount):0.0;
					
					totalNodeCount++;
				}
				if(totalNodeCount > 0)
					score = nodeSum / (double)totalNodeCount;
				else
					score = 0.0;
			}
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG4){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					if(node.getLabel().equals(Graph.ellip.getLabel()) || node.getLabel().equals(Graph.nil.getLabel()))
						continue;
					nodeScore = 1.0;
					nodeCount = 0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node || edge.getHead() == node){
							nodeScore *= edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(Math.pow(nodeScore, 1.0/(double)nodeCount)):0.0;
					
					totalNodeCount++;
				}
				if(totalNodeCount > 0)
					score = nodeSum / (double)totalNodeCount;
				else
					score = 0.0;
			}
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG5){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					nodeScore = 0.0;
					nodeCount = 0.0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node){
							nodeScore += edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(nodeScore/nodeCount):0.0;
				}
				if(parse.getNodes().size() > 0)
					score = nodeSum / (double)parse.getNodes().size();
				else
					score = 0.0;
			}
			else if(combineMethod == ParseScorer.COMBINE_NODEAVG6){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0, totalNodeCount = 0.0;
				for(Node node : parse.getNodes()){
					nodeScore = 1.0;
					nodeCount = 0.0;
					for(Edge edge : parse.getEdges()){
						if(edge.getDep() == node){
							nodeScore *= edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(Math.pow(nodeScore,1.0/nodeCount)):0.0;
				}
				if(parse.getNodes().size() > 0)
					score = nodeSum / (double)parse.getNodes().size();
				else
					score = 0.0;
			}
			else {
				throw new RuntimeException("Unknown combineMethod in ParseScorer : " + combineMethod);
			}
			
			if(Double.isNaN(score))
				throw new RuntimeException("Parse score is NaN");
			else if(Double.isInfinite(score))
					throw new RuntimeException("Parse score is Infinite");
			scoredParses.put(parse, score);
		}
	}
}

public class ParseScorer{
	private BlockingQueue<Runnable> queue;
	private ThreadPoolExecutor executor;
	private ArrayList<Future<?>> futures;
	
	
	public static int COMBINE_SUM = 0;
	public static int COMBINE_PROD = 1;
	public static int COMBINE_AVG = 2;
	public static int COMBINE_NODEAVG1 = 3;
	public static int COMBINE_NODEAVG2 = 4;
	public static int COMBINE_NODEAVG3 = 5;
	public static int COMBINE_NODEAVG4 = 6;
	public static int COMBINE_NODEAVG5 = 8;
	public static int COMBINE_NODEAVG6 = 9;
	public static int COMBINE_GEO = 7;

	public ParseScorer(int numThreads){
		this.queue = new LinkedBlockingQueue<Runnable>();
		this.futures = new ArrayList<Future<?>>();
		this.executor = new ThreadPoolExecutor(numThreads, numThreads, 30, TimeUnit.SECONDS, this.queue);
	}


	public synchronized void submitTask(Runnable task){
		if(this.executor == null || this.executor.isShutdown() || this.executor.isTerminated() || this.executor.isTerminating()){
			System.err.println("The threadpool is stopped in ParseScorer.submitTask()");
			System.exit(1);
		}
		
		Future<?> future = this.executor.submit(task);
		futures.add(future);
	}

	public synchronized void submitTask(LinkedHashMap<Graph,Double> parses, LinkedHashMap<Graph,Double> scoredParses, EdgeScorer edgeScorer, int combineMethod){
		ParseScorerTask task = new ParseScorerTask(parses, scoredParses, edgeScorer, combineMethod);
		this.submitTask(task);
	}

	public synchronized void stopThreadPool(){
		this.executor.shutdown();
	}
	
	public boolean isStopped(){
		if(this.executor.isShutdown() || this.executor.isTerminated() || this.executor.isTerminating())
			return true;
		return false;
	}
	
	public void waitToFinish(){
		try{
			for (Future<?> future : futures) {
				future.get();
			}
		} catch(Exception e){
			throw new RuntimeException(e);
		}
		futures.clear();
	}
}

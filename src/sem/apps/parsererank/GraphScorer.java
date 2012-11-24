package sem.apps.parsererank;


import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

/**
 * Assigns confidence scores to dependency graphs
 *
 */
class GraphScorerTask implements Runnable{
	private LinkedHashMap<Graph,Double> graphs;
	private EdgeScorer edgeScorer;
	private LinkedHashMap<Graph,Double> scoredGraphs;
	private int combineMethod;
	
	public GraphScorerTask(LinkedHashMap<Graph,Double> graphs, LinkedHashMap<Graph,Double> scoredGraphs, EdgeScorer edgeScorer, int combineMethod){
		this.graphs = graphs;
		this.scoredGraphs = scoredGraphs;
		this.edgeScorer = edgeScorer;
		this.combineMethod = combineMethod;
	}
	
	@Override
	public void run() {
		// First we calculate the edge scores
		LinkedHashMap<Edge,Double> edgeScores = edgeScorer.run(graphs);

		// Next we combine the edge scores into a parse score
		double score = 0.0, sum = 0.0;
		for(Graph graph : graphs.keySet()){
			sum = 0.0; score = 0.0;
			for(Edge edge : graph.getEdges()){
				sum += edgeScores.get(edge);
			}
			
			if(combineMethod == GraphScorer.COMBINE_SUM)
				score = sum;
			else if(combineMethod == GraphScorer.COMBINE_AVG)
				score = sum / (double)graph.getEdges().size();
			else if(combineMethod == GraphScorer.COMBINE_NODEAVG){
				double nodeSum = 0.0, nodeScore = 0, nodeCount = 0.0;
				for(Node node : graph.getNodes()){
					nodeScore = 0.0;
					nodeCount = 0.0;
					for(Edge edge : graph.getEdges()){
						if(edge.getDep() == node){
							nodeScore += edgeScores.get(edge);
							nodeCount++;
						}
					}
					nodeSum += (nodeCount > 0.0)?(nodeScore/nodeCount):0.0;
				}
				
				if(graph.getNodes().size() > 0)
					score = nodeSum / (double)graph.getNodes().size();
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
			
			scoredGraphs.put(graph, score);
		}
	}
}

/**
 * The main class for performing graph scoring.
 * It calculates scores for every edge using an initialised EdgeScorer, and then combines them together into a graph score.
 * The implementation uses a thread pool to distribute parallel tasks onto separate processors.
 *
 */
public class GraphScorer{
	private BlockingQueue<Runnable> queue;
	private ThreadPoolExecutor executor;
	private ArrayList<Future<?>> futures;
	
	public static final int COMBINE_SUM = 0;
	public static final int COMBINE_AVG = 1;
	public static final int COMBINE_NODEAVG = 2;

	public GraphScorer(int numThreads){
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

	public synchronized void submitTask(LinkedHashMap<Graph,Double> graphs, LinkedHashMap<Graph,Double> scoredGraphs, EdgeScorer edgeScorer, int combineMethod){
		GraphScorerTask task = new GraphScorerTask(graphs, scoredGraphs, edgeScorer, combineMethod);
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

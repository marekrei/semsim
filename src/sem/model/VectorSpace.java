package sem.model;

import java.util.HashMap;
import java.util.LinkedHashMap;

import sem.util.Index;
import sem.util.Tools;

/**
 * A class for managing the feature vectors. It creates vectors using a specified weight measure, and caches them if necessary.
 *
 */
public class VectorSpace {
	private SemModel semModel;
	private int weightScheme;
	private boolean enableCache;
	
	private Index featureIndex;
	private Index nodeIndex;
	private double totalFeatureCount;
	private HashMap<Integer,LinkedHashMap<Integer,Double>> vectorCache;
	
	public static int WEIGHT_BINARY = 0;
	public static int WEIGHT_FREQ = 1;
	public static int WEIGHT_RELFREQ = 2;
	public static int WEIGHT_PMI = 3;
	public static int WEIGHT_PMI_LIM = 4;
	
	public VectorSpace(SemModel semModel, int weightScheme, boolean enableCache){
		this.semModel = semModel;
		this.weightScheme = weightScheme;
		this.enableCache = enableCache;
		this.vectorCache = new HashMap<Integer,LinkedHashMap<Integer,Double>>();
		
		this.featureIndex = null;
		this.nodeIndex = null;
		
		init();
	}
	
	private void init(){
		this.featureIndex = new Index();
		this.nodeIndex = new Index();
		String featureLabel;
		double c;
		for(int key1 : this.semModel.getTensor().getKeys()){
			for(int key2 : this.semModel.getTensor().getKeys(key1)){
				for(int key3 : this.semModel.getTensor().getKeys(key1, key2)){
					featureLabel = key2 + "," + key3;
					c = this.semModel.getTensor().get(key1, key2, key3);
					featureIndex.add(featureLabel, c);
					nodeIndex.add("" + key1, c);
				}
			}
		}
		this.totalFeatureCount = this.featureIndex.getTotalCount();
	}
	
	public Index getFeatureIndex(){
		return this.featureIndex;
	}
	
	public double getTotalFeatureCount(){
		return this.totalFeatureCount;
	}
	
	public synchronized void addToCache(Integer nodeId, LinkedHashMap<Integer,Double> sortedVector){
		this.vectorCache.put(nodeId, sortedVector);
	}
	
	public LinkedHashMap<Integer,Double> getVectorFromCache(Integer nodeId){
		return this.vectorCache.get(nodeId);
	}

	public LinkedHashMap<Integer,Double> getVector(String nodeLabel){
		Integer nodeId = this.semModel.getNodeIndex().getId(nodeLabel);
		if(nodeId == null)
			return new LinkedHashMap<Integer,Double>();
		
		LinkedHashMap<Integer,Double> vector = null;
		if(this.enableCache)
			vector = getVectorFromCache(nodeId);
		if(vector != null)
			return vector;
		
		if(this.weightScheme == WEIGHT_BINARY)
			vector = getVectorBinary(nodeId);
		else if(this.weightScheme == WEIGHT_FREQ)
			vector = getVectorFreq(nodeId);
		else if(this.weightScheme == WEIGHT_RELFREQ)
			vector = getVectorRelFreq(nodeId);
		else if(this.weightScheme == WEIGHT_PMI)
			vector = getVectorPMI(nodeId);
		else if(this.weightScheme == WEIGHT_PMI_LIM)
			vector = getVectorPMILim(nodeId);
		else
			throw new RuntimeException("Unknown weight scheme: " + this.weightScheme);
		
		LinkedHashMap<Integer,Double> sortedVector = Tools.sort(vector, true);
		if(this.enableCache){
			addToCache(nodeId, sortedVector);
		}
		return sortedVector;
	}
	
	private LinkedHashMap<Integer,Double> getVectorPMI(Integer nodeId){
		LinkedHashMap<Integer,Double> vector = new LinkedHashMap<Integer,Double>();
		String featureLabel;
		Integer featureId;
		
		if(!this.semModel.getTensor().containsKey(nodeId))
			return vector;
		
		Double nodeCount = this.semModel.getNodeIndex().getCount(nodeId);
		Double totalFeatureCount = this.getTotalFeatureCount();
		
		for(int key2 : this.semModel.getTensor().getKeys(nodeId)){
			for(int key3 : this.semModel.getTensor().getKeys(nodeId, key2)){
				featureLabel = key2 + "," + key3;
				featureId = this.getFeatureIndex().getId(featureLabel);
				Double weight = Math.log((this.semModel.getTensor().get(nodeId, key2, key3)/totalFeatureCount) 
								/ ((nodeCount / totalFeatureCount) * (this.featureIndex.getCount(featureId)/totalFeatureCount)));
				vector.put(featureId, weight);
			}
		}
		
		
		
		return vector;
	}
	
	private LinkedHashMap<Integer,Double> getVectorPMILim(Integer nodeId){
		LinkedHashMap<Integer,Double> vector = new LinkedHashMap<Integer,Double>();
		String featureLabel;
		Integer featureId;
		
		if(!this.semModel.getTensor().containsKey(nodeId))
			return vector;
		
		Double nodeCount = this.semModel.getNodeIndex().getCount(nodeId);
		Double totalFeatureCount = this.getTotalFeatureCount();
		double featureCount;
		for(int key2 : this.semModel.getTensor().getKeys(nodeId)){
			for(int key3 : this.semModel.getTensor().getKeys(nodeId, key2)){
				featureLabel = key2 + "," + key3;
				featureId = this.getFeatureIndex().getId(featureLabel);
				featureCount = this.featureIndex.getCount(featureId);
				
				if(featureCount < 2)
					continue;
				Double weight = Math.log((this.semModel.getTensor().get(nodeId, key2, key3)/totalFeatureCount) 
								/ ((nodeCount / totalFeatureCount) * (featureCount/totalFeatureCount)));
				vector.put(featureId, weight);
			}
		}
		
		
		
		return vector;
	}
	
	private LinkedHashMap<Integer,Double> getVectorBinary(Integer nodeId){
		LinkedHashMap<Integer,Double> vector = new LinkedHashMap<Integer,Double>();
		String featureLabel;
		Integer featureId;
		
		if(!this.semModel.getTensor().containsKey(nodeId))
			return vector;
		
		for(int key2 : this.semModel.getTensor().getKeys(nodeId)){
			for(int key3 : this.semModel.getTensor().getKeys(nodeId, key2)){
				featureLabel = key2 + "," + key3;
				featureId = this.getFeatureIndex().getId(featureLabel);
				Double weight = 1.0;
				vector.put(featureId, weight);
			}
		}
		
		return vector;
	}
	
	private LinkedHashMap<Integer,Double> getVectorFreq(Integer nodeId){
		LinkedHashMap<Integer,Double> vector = new LinkedHashMap<Integer,Double>();
		String featureLabel;
		Integer featureId;
		
		if(!this.semModel.getTensor().containsKey(nodeId))
			return vector;
		
		for(int key2 : this.semModel.getTensor().getKeys(nodeId)){
			for(int key3 : this.semModel.getTensor().getKeys(nodeId, key2)){
				featureLabel = key2 + "," + key3;
				featureId = this.getFeatureIndex().getId(featureLabel);
				Double weight = this.semModel.getTensor().get(nodeId, key2, key3);
				vector.put(featureId, weight);
			}
		}
		
		return vector;
	}
	
	private LinkedHashMap<Integer,Double> getVectorRelFreq(Integer nodeId){
		LinkedHashMap<Integer,Double> vector = new LinkedHashMap<Integer,Double>();
		String featureLabel;
		Integer featureId;
		
		if(!this.semModel.getTensor().containsKey(nodeId))
			return vector;
		
		Double nodeCount = this.semModel.getTensor().get(nodeId, null, null);
		for(int key2 : this.semModel.getTensor().getKeys(nodeId)){
			for(int key3 : this.semModel.getTensor().getKeys(nodeId, key2)){
				featureLabel = key2 + "," + key3;
				featureId = this.getFeatureIndex().getId(featureLabel);
				Double weight = this.semModel.getTensor().get(nodeId, key2, key3) / nodeCount;
				vector.put(featureId, weight);
			}
		}
		
		return vector;
	}
	
	public void enableCache(){
		this.enableCache = true;
	}
	
	public void disableCache(){
		this.enableCache = false;
	}
	
	public void clearCache(){
		this.vectorCache.clear();
	}
}

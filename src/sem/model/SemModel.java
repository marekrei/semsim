package sem.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import sem.exception.SemModelException;
import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;
import sem.util.Index;
import sem.util.IntegerMultiMap;
import sem.util.Tensor;

/**
 * <p>This class stores the information and statistics about the vector space model. All labels are matched to unique IDs. The number of times a label appears on a node or an edge is counted.
 * <p>It also retains a 3-dimensional tensor of the edge statistics, which has the shape <it>HEADID-RELATIONID-DEPID</it>. Every position in the tensor depends on the 3 keys of integer type, corresponding to a value of type double. However, the VSM has functions for these values directly using only the labels.
 * <p>By default the tensor is built with edges only in one direction, e.g. (head, rel, dep). This saves both disk space and memory. However, when creating feature vectors, we might want to include reverse edges as well, e.g. (dep, rev_rel, head). Call the makeTensorSymmetric() function on a completed SemModel to mirror the tensor and add these missing edges to the model.
 */
public class SemModel {
	private Tensor tensor;
	private Index nodeIndex;
	private Index edgeIndex;
	
	private IntegerMultiMap locations;
	private int count;
	
	private String tensorFileName = "_tensor.vsm";
	private String nodeIndexFileName = "_nodeindex.vsm";
	private String edgeIndexFileName = "_edgeindex.vsm";
	private String locationsFileName = "_locations.vsm";
	
	boolean enableCache;
	ConcurrentHashMap<String,Double> cache;
	
	/**
	 * 
	 * @param keepLoc Setting this to true will keep track of in which sentences every word occurs. It can be useful when we need to find how many times two words occur together in a sentence. However, it requires quite a bit of extra memory.
	 */
	public SemModel(boolean keepLoc){
		this(keepLoc, false);
	}
	
	public SemModel(boolean keepLoc, boolean enableCache){
		this.tensor = new Tensor();
		this.nodeIndex = new Index();
		this.edgeIndex = new Index();
		this.enableCache = enableCache;
		this.cache = new ConcurrentHashMap<String,Double>();
		if(keepLoc)
			this.locations = new IntegerMultiMap();
		else
			this.locations = null;
		this.count = 0;
	}
	
	public SemModel(String path, boolean keepLoc){
		this(path, keepLoc, false);
	}
	
	public SemModel(String path, boolean keepLoc, boolean enableCache){
		this.tensor = new Tensor(path + tensorFileName);
		this.nodeIndex = new Index(path + nodeIndexFileName);
		this.edgeIndex = new Index(path + edgeIndexFileName);
		this.enableCache = enableCache;
		this.cache = new ConcurrentHashMap<String,Double>();
		if(keepLoc)
			this.locations = new IntegerMultiMap(path + this.locationsFileName);
		else
			this.locations = null;
	}
	
	public void save(String path){
		this.tensor.save(path + tensorFileName);
		this.nodeIndex.save(path + nodeIndexFileName);
		this.edgeIndex.save(path + edgeIndexFileName);
		if(this.locations != null)
			this.locations.save(path + this.locationsFileName);
	}

	public synchronized void add(Graph graph) throws SemModelException{
		count++;
		for(Node node : graph.getNodes()){
			int nodeKey = nodeIndex.add(node.getLabel());
			if(locations != null){
				if(!locations.containsKey(nodeKey))
					locations.put(nodeKey, new ArrayList<Integer>());
				locations.get(nodeKey).add(count);
			}
		}
		
		Integer headId, edgeId, depId;
		for(Edge edge : graph.getEdges()){
			edgeId = edgeIndex.add(edge.getLabel());
			headId = nodeIndex.getId(edge.getHead().getLabel());
			depId = nodeIndex.getId(edge.getDep().getLabel());

			if(headId == null || edgeId == null || depId == null)
				throw new SemModelException("Error when adding an edge. Head, dep or edge id is null");
			
			tensor.add(headId, edgeId, depId, 1.0);
		}
	}
	
	public double getNodeCount(String label){
		return this.nodeIndex.getCount(label);
	}
	
	public double getEdgeCount(String label){
		return this.edgeIndex.getCount(label);
	}
	
	public synchronized void addToCache(String key, Double value){
		this.cache.put(key, value);
	}
	
	public double getTripleCount(String headLabel, String edgeLabel, String depLabel){
		String key = "TRIPLE:" + (headLabel == null?"[[!!NULL!!]]":headLabel) + "\t" + (edgeLabel == null?"[[!!NULL!!]]":edgeLabel) + "\t" + (depLabel == null?"[[!!NULL!!]]":depLabel);
		if(this.enableCache && this.cache.containsKey(key))
			return this.cache.get(key);
		
		double result = 0.0;
		Integer headId = null, depId = null, edgeId = null;
		
		if(headLabel != null)
			headId = this.nodeIndex.getId(headLabel);
		if(depLabel != null)
			depId = this.nodeIndex.getId(depLabel);
		if(edgeLabel != null)
			edgeId = this.edgeIndex.getId(edgeLabel);
		
		if((headLabel != null && headId == null) || (depLabel != null && depId == null) || (edgeLabel != null && edgeId == null))
			result = 0.0;
		else
			result = this.tensor.get(headId, edgeId, depId);
		
		if(this.enableCache)
			addToCache(key, result);
		
		return result;
	}

	
	public double getLocationMatchCount(String label1, String label2){
		if(this.locations == null)
			throw new RuntimeException("This VSM does not support locations");
		
		String key = "LOCMATCH:" + (label1 == null?"[[!!NULL!!]]":label1) + "\t" + (label2 == null?"[[!!NULL!!]]":label2);
		if(this.enableCache && this.cache.containsKey(key))
			return this.cache.get(key);
		
		Integer label1Id = null, label2Id = null;
		
		if(label1 != null)
			label1Id = this.nodeIndex.getId(label1);
		if(label2 != null)
			label2Id = this.nodeIndex.getId(label2);
		
		if((label1 != null && label1Id == null) || (label2 != null && label2Id == null))
			return 0.0;
		
		ArrayList<Integer> locations1 = this.locations.get(label1Id);
		ArrayList<Integer> locations2 = this.locations.get(label2Id);
		
		double total = 0.0;
		
		if(label1 == null && label2 == null){
			total = getTotalCoocCount();
		}
		else if((label1 == null && label2 != null) || (label1 != null && label2 == null)){
			Integer labelId = null;
			if(label1 != null)
				labelId = label1Id;
			else if(label2 != null)
				labelId = label2Id;
			
			if(labelId == null)
				total = 0.0;
			else {
				HashSet<Integer> locations = new HashSet<Integer>(this.locations.get(labelId));
				for(Entry<Integer,ArrayList<Integer>> entry : this.locations.entrySet()){
					if(entry.getKey().equals(labelId))
						continue;

					for(Integer sentence : entry.getValue()){
						if(locations.contains(sentence))
							total += 1;
					}
				}
			}
		}
		else if(label1Id.equals(label2Id)){
			double sequenceLength = 0.0;
			for(int i = 1; i < locations1.size(); i++){
				if(locations1.get(i).equals(locations1.get(i-1))){
					sequenceLength++;
					total += sequenceLength;
				}
				else
					sequenceLength = 0.0;
			}
		}
		else{
			int pos1 = 0, pos2 = 0, count1 = 0, count2 = 0;
			while(pos1 < locations1.size() && pos2 < locations2.size()){
				if(locations1.get(pos1).equals(locations2.get(pos2))){
					count1 = 1;
					count2 = 1;
					while(pos1+1 < locations1.size() && locations1.get(pos1).equals(locations1.get(pos1+1))){
						pos1++;
						count1++;
					}
					while(pos2+1 < locations2.size() && locations2.get(pos2).equals(locations2.get(pos2+1))){
						pos2++;
						count2++;
					}
					total += count1 * count2;
					pos1++;
					pos2++;
				}
				else if(locations1.get(pos1) < locations2.get(pos2))
					pos1++;
				else if(locations2.get(pos2) < locations1.get(pos1))
					pos2++;
			}
		}
		
		if(this.enableCache)
			addToCache(key, total);
		
		return total;
		
	}

	
	private String getLocationMatchKey(String label1, String label2){
		String key = "LOCMATCH:" + (label1 == null?"[[!!NULL!!]]":label1) + "\t" + (label2 == null?"[[!!NULL!!]]":label2);
		return key;
	}
	
	private synchronized double calculateTotalCoocCount(){
		double total = 0.0;
		LinkedHashMap<Integer,Double> sentenceLengths = new LinkedHashMap<Integer,Double>();
		Double value;
		for(ArrayList<Integer> list : this.locations.values()){
			for(Integer sentence : list){
				value = sentenceLengths.get(sentence);
				sentenceLengths.put(sentence, (value==null?0.0:value)+1.0);
			}
		}
		for(Double length : sentenceLengths.values())
			if(length > 0.0)
				total += length * (length-1.0);
		
		sentenceLengths = null;
		return total;
	}
	
	private synchronized double _getTotalCoocCount(){
		String key = getLocationMatchKey(null, null);
		if(this.cache.containsKey(key)){
			return this.cache.get(key);
		}
		else{
			double total = calculateTotalCoocCount();
			if(this.enableCache)
				addToCache(key, total);
			return total;
		}
	}
	
	public double getTotalCoocCount(){
		String key = getLocationMatchKey(null, null);
		if(this.cache.containsKey(key))
			return this.cache.get(key);
		else
			return _getTotalCoocCount();
	}
	
	private synchronized double calculateTripleTypeCount(){
		double total = 0.0;
		
		for(int key1 : this.tensor.getKeys())
			for(int key2 : this.tensor.getKeys(key1))
				total += this.tensor.getKeys(key1, key2).length;
		System.out.println("TOTAL_TRIPLE_COUNT:" + total);
		return total;
	}
	
	private synchronized double _getTripleTypeCount(){
		String key = "TRIPLE_TYPE_COUNT";
		if(this.cache.containsKey(key)){
			return this.cache.get(key);
		}
		else{
			double total = calculateTripleTypeCount();
			if(this.enableCache)
				addToCache(key, total);
			return total;
		}
	}
	
	public double getTripleTypeCount(){
		String key = "TRIPLE_TYPE_COUNT";
		if(this.cache.containsKey(key))
			return this.cache.get(key);
		else
			return _getTripleTypeCount();
	}
	
	public double getTotalNodeCount(){
		String key = "TOTAL_NODE_COUNT";
		if(this.enableCache && this.cache.containsKey(key))
			return this.cache.get(key);
		double value = this.nodeIndex.getTotalCount();
		if(this.enableCache)
			addToCache(key, value);
		return value;
	}
	
	public double getTotalEdgeCount(){
		String key = "TOTAL_EDGE_COUNT";
		if(this.enableCache && this.cache.containsKey(key))
			return this.cache.get(key);
		double value = this.edgeIndex.getTotalCount();
		if(this.enableCache)
			addToCache(key, value);
		return value;
	}
	
	public void enableCache(){
		this.enableCache = true;
	}
	
	public void disableCache(){
		this.enableCache = false;
	}
	
	/**
	 * This method needs to be called to make the tensor symmetric (adding dependency edges in the reverse direction).
	 */
	public void makeTensorSymmetric(){
		int key2New;
		String edgeLabel;
		for(int key1 : tensor.getKeys()){
			for(int key2 : tensor.getKeys(key1)){
				edgeLabel = this.edgeIndex.getLabel(key2);
				if(edgeLabel != null && !edgeLabel.startsWith("!")){
					for(int key3 : tensor.getKeys(key1, key2)){
						key2New = edgeIndex.add("!" + edgeLabel, 1.0);
						tensor.add(key3, key2New, key1, tensor.get(key1, key2, key3));
					}
				}
			}
		}
	}
	
	public Index getNodeIndex(){
		return this.nodeIndex;
	}
	
	public Index getEdgeIndex(){
		return this.edgeIndex;
	}
	
	public Tensor getTensor(){
		return this.tensor;
	}
}

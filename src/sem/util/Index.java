package sem.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Maintains an index.
 * Every unique string is mapped to an integer and the number of occurrences is counted.
 */
public class Index{
	
	private HashMap<String,Integer> idMap;
	private HashMap<Integer,Double> countMap;
	private HashMap<Integer,String> labelMap;
	
	/**
	 * Create a new Index.
	 */
	public Index(){
		this.idMap = new HashMap<String,Integer>();
		this.countMap = new HashMap<Integer,Double>();
		this.labelMap = null;
	}
	
	/**
	 * Create a new Dictionary from file. The file should be created by the save() method.
	 * @param file Input file
	 */
	public Index(String file){
		this();
		this.load(file);
	}
	
	/**
	 * Add an item to the dictionary. If the item already exists, the counts are added up.
	 * @param label Label
	 * @param count Added count
	 * @return The ID of the item that was just added.
	 */
	public synchronized int add(String label, Double count){
		Integer tempInt = idMap.get(label);
		if(tempInt == null){
			int newId = this.getNextId();
			idMap.put(label, newId);
			countMap.put(newId, count);
			if(this.labelMap != null)
				this.labelMap.put(newId, label);
			return newId;
		}
		else {
			countMap.put(tempInt, countMap.get(tempInt) + count);
			return tempInt;
		}
	}
	
	/**
	 * Add an item to the dictionary with count 1. If the item already exists, the counts are added up.
	 * @param label	Label
	 * @return The ID of the item that was just added.
	 */
	public synchronized int add(String label){
		return this.add(label, 1.0);
	}
	
	/**
	 * Get the Id corresponding to the label.
	 * @param label Key
	 * @return The ID belonging to the key. Null if the label does not exist in the index.
	 */
	public Integer getId(String label){
		return this.idMap.get(label);
	}
	
	/**
	 * Get the count of an object using their ID.
	 * @param id ID
	 * @return The count of the object. 0 if it does not exist.
	 */
	public Double getCount(Integer id){
		Double tempDouble = this.countMap.get(id);
		if(tempDouble != null)
			return tempDouble;
		return 0.0;
	}
	
	/**
	 * Get the count of an object using their label.
	 * @param label Label
	 * @return The count of the object. 0 if it does not exist.
	 */
	public Double getCount(String str){
		Integer tempInt = this.getId(str);
		if(tempInt != null)
			return this.getCount(tempInt);
		return 0.0;
	}
	
	/**
	 * Get the label using their ID.
	 * @param id	ID	
	 * @return 	The label
	 */
	public String getLabel(Integer id){
		return this.getLabelMap().get(id);
	}
	
	/**
	 * Get the number of elements in the index.
	 * @return The number of elements in the index.
	 */
	public int size(){
		return this.idMap.size();
	}
	
	/**
	 * Remove all elements from the dictionary.
	 */
	public void clear(){
		if(this.idMap != null)
			this.idMap.clear();
		if(this.countMap != null)
			this.countMap.clear();
		if(this.labelMap != null)
			this.labelMap.clear();
	}
	
	private int getNextId(){
		return this.size() + 1;
	}

	/**
	 * Get the Label-to-ID map.
	 * @return Label-to-ID map
	 */
	public HashMap<String, Integer> getIdMap() {
		return this.idMap;
	}

	/**
	 * Get the ID-to-Count map.
	 * @return ID-to-Count map
	 */
	public HashMap<Integer, Double> getCountMap() {
		return this.countMap;
	}
	
	/**
	 * Get the ID-to-Label map. Compared to others, this takes more time, as it is only created when needed.
	 * @return ID-to-Label map
	 */
	public HashMap<Integer,String> getLabelMap(){
		if(this.labelMap == null){
			this.labelMap = new HashMap<Integer,String>();
			for(Entry<String,Integer> e : this.idMap.entrySet())
				labelMap.put(e.getValue(), e.getKey());
		}
		return labelMap;
	}
	
	/**
	 * Load the index from a file.
	 * @param file Input file
	 */
	private void load(String file)
	{
		Integer tempInt;
		String[] temp;
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			while ((strLine = br.readLine()) != null)   {
				temp = strLine.split("\\t+");
				if(temp.length == 3){
					tempInt = Integer.parseInt(temp[0]);
					this.idMap.put(temp[1], tempInt);
					this.countMap.put(tempInt, Double.parseDouble(temp[2]));
				}
				else{
					throw new RuntimeException("Illegal number of columns in the input file.");
				}
			}
			in.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Save the dictionary to a file.
	 * @param file Output file
	 * @param plain Omit counts and print only the strings and Ids
	 */
	public void save(String file)
	{
		try{
			FileWriter fstream = new FileWriter(file);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(Entry<String,Integer> e : this.idMap.entrySet()){
		    	out.write(e.getValue() + "\t" + e.getKey()+"\t"+this.countMap.get(e.getValue()) + "\n");
		    }
		    out.close();
		    fstream.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public boolean contains(String label){
		return this.idMap.containsKey(label);
	}
	
	public boolean contains(Integer id){
		return this.idMap.containsValue(id);
	}
	
	public Double getTotalCount(){
		double total = 0.0;
		for(Double value : this.countMap.values())
			total += value;
		return total;
	}
}
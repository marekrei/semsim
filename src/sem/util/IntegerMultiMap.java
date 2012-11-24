package sem.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * An integer map, where each key can correspond to multiple values
 *
 */
public class IntegerMultiMap extends java.util.HashMap<Integer,ArrayList<Integer>>{
	
	public IntegerMultiMap(){
		super();
	}
	
	public IntegerMultiMap(String inputFile){
		super();
		this.load(inputFile);
	}
	
	public void load(String file){
		if(file == null){
			throw new RuntimeException("Input file required");
		}
		else{
			FileReader fileReader = new FileReader(file);
			String[] line_chunks;
			Integer id;
			while(fileReader.hasNext()){
				line_chunks = fileReader.next().trim().split("\\t+");
				if(line_chunks.length <= 0)
					continue;
				id = Integer.parseInt(line_chunks[0]);
				if(!this.containsKey(id))
					this.put(id, new ArrayList<Integer>());
				for(int i = 1; i < line_chunks.length; i++)
					this.get(id).add(Integer.parseInt(line_chunks[i]));
			}
		}
	}
	
	public void save(String file){
		try{
			FileWriter fstream = new FileWriter(file);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(Entry<Integer,ArrayList<Integer>> e : this.entrySet()){
		    	out.write("" + e.getKey());
		    	for(Integer i : e.getValue())
		    		out.write("\t" + i);
		    	out.write("\n");
		    }
		    out.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void put(Integer key, Integer value){
		if(!this.containsKey(key))
			this.put(key, new ArrayList<Integer>());
		this.get(key).add(value);
	}
}

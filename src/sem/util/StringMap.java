package sem.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map.Entry;

/**
 * A HashMap<String,String> with added functionality for saving and loading in text format.
 *
 */
public class StringMap extends java.util.HashMap<String,String>{
	
	public StringMap(){
		super();
	}
	
	public StringMap(String inputFile){
		super();
		this.load(inputFile);
	}
	
	public void load(String file){
		if(file == null){
			throw new RuntimeException("InputFile required in PosConverter");
		}
		else{
			FileReader fileReader = new FileReader(file);
			String[] line_chunks;
			while(fileReader.hasNext()){
				line_chunks = fileReader.next().split("\\t+");
				if(line_chunks.length != 2){
					throw new RuntimeException("Format error in StringMap.load()");
				}
				this.put(line_chunks[0], line_chunks[1]);
			}
		}
	}
	
	public void save(String file){
		try{
			FileWriter fstream = new FileWriter(file);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(Entry<String,String> e : this.entrySet()){
		    		out.write(e.getKey() +"\t" +e.getValue()+"\n");
		    }
		    out.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}

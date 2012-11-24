package sem.grapheditor;

import sem.util.StringMap;

/**
 * A class that stores the mapping between two types of POS tags.
 *
 */
public class PosMap extends StringMap{
	public PosMap(String posMapPath){
		//super("/auto/homes/mr472/Documents/Projects/SemSim/tagsets/claws2-universal.txt");
		super(posMapPath);
	}
	
	public boolean isConj(String pos){
		if(this.containsKey(pos) && this.get(pos).equalsIgnoreCase("CONJ"))
			return true;
		return false;
	}
	
	public boolean isAdp(String pos){
		if(this.containsKey(pos) && this.get(pos).equalsIgnoreCase("ADP"))
			return true;
		return false;
	}
	
	public boolean isVerb(String pos){
		if(this.containsKey(pos) && this.get(pos).equalsIgnoreCase("VERB"))
			return true;
		return false;
	}
	
	public boolean isNoun(String pos){
		if(this.containsKey(pos) && this.get(pos).equalsIgnoreCase("NOUN"))
			return true;
		return false;
	}
	
	public boolean isAdj(String pos){
		if(this.containsKey(pos) && this.get(pos).equalsIgnoreCase("ADJ"))
			return true;
		return false;
	}
	
	public boolean isOpenClass(String pos){
		if(isNoun(pos) || isVerb(pos) || isAdj(pos))// || isAdv(pos))
			return true;
		return false;
	}
}

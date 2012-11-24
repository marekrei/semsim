package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * A class containing information about the RASP GR hierarchy
 *
 */
public class RaspGrTypeHierarchy {
	/**
	 * The string representing the RASP GR type hierarchy.
	 * See : ﻿Watson, R. (2006). RASP Evaluation Schemes.
	 * It is represented as a comma-separated list of edges. Each node is assumed to have a unique label. Loops are allowed.
	 */
	private static String typeGraphString = "dependent ta, dependent arg_mod, dependent det, dependent aux, dependent conj, " +
			"arg_mod mod, arg_mod arg, mod ncmod, mod xmod, mod cmod, mod pmod, arg subj, arg subj_dobj, arg comp, " +
			"subj ncsubj, subj xsubj, subj csubj, subj_dobj subj, subj_dobj dobj, comp obj, comp pcomp, comp clausal, " +
			"obj dobj, obj obj2, obj iobj, clausal xcomp, clausal ccomp";
	
	/**
	 * List of direct parents for each node. Created as needed.
	 */
	private static HashMap<String,ArrayList<String>> parents;
	
	/**
	 * List of ancestors for each node. Created as needed.
	 */
	private static HashMap<String,ArrayList<String>> ancestors;

	/**
	 * Create the list of direct parents for each node.
	 * @return A HashMap (with keys representing GR types) of ArrayLists that contains the list of direct parents.
	 */
	private static HashMap<String,ArrayList<String>> createParents(){
		HashMap<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
		String[] tempNodes;
		for(String edge : typeGraphString.split(",")){
			edge = edge.trim();
			tempNodes = edge.split("\\s+");
			
			if(tempNodes.length != 2){
				System.out.println("Error when reading edges in RaspGrTypeGraph.init()");
				System.exit(1);
			}
			
			if(!map.containsKey(tempNodes[0]))
				map.put(tempNodes[0], new ArrayList<String>());
			if(!map.containsKey(tempNodes[1]))
				map.put(tempNodes[1], new ArrayList<String>());
			if(!map.get(tempNodes[1]).contains(tempNodes[0]))
				map.get(tempNodes[1]).add(tempNodes[0]);
		}
		
		return map;
	}
	
	/**
	 * Create the list of ancestors for each node.
	 * @return A HashMap (with keys representing GR types) of ArrayLists that contains the list of ancestors.
	 */
	private static HashMap<String,ArrayList<String>> createAncestors(){
		HashMap<String,ArrayList<String>> tempAncestors = new HashMap<String,ArrayList<String>>();
		if(parents == null)
			parents = createParents();
		
		// Initialize ancestors with parents
		for(String key : parents.keySet()){
			tempAncestors.put(key, new ArrayList<String>(parents.get(key)));
		}
		
		// Iteratively propagate ancestors until convergence
		ArrayList<String> types = new ArrayList<String>(tempAncestors.keySet());
		ArrayList<String> newAncestors = new ArrayList<String>();
		boolean update = true;
		while(update == true){
			update = false;
			for(String type : types){
				newAncestors.clear();
				for(String parent : tempAncestors.get(type)){
					for(String ancestor : tempAncestors.get(parent)){
						if(!tempAncestors.get(type).contains(ancestor) && !newAncestors.contains(ancestor)){
							newAncestors.add(ancestor);
							update = true;
						}
					}
				}
				tempAncestors.get(type).addAll(newAncestors);
			}
		}
		
		return tempAncestors;
	}
	
	/**
	 * Get the list of ancestors for a GR type
	 * @param type GR Type
	 * @return ArrayList of ancestors
	 */
	public static List<String> getAncestors(String type){
		if(ancestors == null)
			ancestors = createAncestors();
		if(!ancestors.containsKey(type)){
			System.out.println(type);
			System.exit(1);
		}
		return Collections.unmodifiableList(ancestors.get(type));
	}

	/**
	 * Get the list of parents for a GR type
	 * @param type GR Type
	 * @return ArrayList of parents
	 */
	public static List<String> getParents(String type){
		if(parents == null)
			parents = createParents();
		return Collections.unmodifiableList(parents.get(type));
	}
	
	public static List<String> getSubsumed(String type){
		if(ancestors == null)
			ancestors = createAncestors();
		if(!ancestors.containsKey(type)){
			System.out.println(type);
			System.exit(1);
		}
		ArrayList<String> subsumed = new ArrayList<String>(ancestors.get(type));
		subsumed.add(0, type);
		return Collections.unmodifiableList(subsumed);
	}
	
	public static Set<String> getTypes(){
		if(parents == null)
			parents = createParents();
		return Collections.unmodifiableSet(parents.keySet());
	}
}

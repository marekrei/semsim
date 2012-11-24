package sem.grapheditor;

import java.util.ArrayList;
import java.util.Arrays;

import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for converting numerical lemmas into more generic labels
 *
 */
public class NumTagsGraphEditor implements GraphEditor{

	private static ArrayList<String> numberWords = new ArrayList<String>(Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "tweny-one", "twenty-two", "twenty-three", "twnety-four", "twenty-five", "twenty-six", "twenty-seven", "twenty-eight", "twenty-nine", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", "hundred", "thousand", "million", "billion", "trillion"));
	
	
	public static boolean isNumberWord(String str){
		str = str.toLowerCase();
		if(numberWords.contains(str))
			return true;
		return false; 
	}
	
	public static boolean isNumerical(String str){
		boolean containsNumber = false;
		for(int i = 0; i < str.length(); i++){
			if(Character.isLetter(str.charAt(i)))
				return false;
			else if(Character.isDigit(str.charAt(i)))
				containsNumber = true;
		}
		return containsNumber;
	}
	
	public static boolean isNumber(String str){
		if(str.length() <= 0)
			return false;
		for(int i = 0; i < str.length(); i++){
			if(!Character.isDigit(str.charAt(i)) && str.charAt(i) != '.' && str.charAt(i) != ',')
				return false;
		}
		return true;
	}
	
	public static boolean isMoney(String str){
		str = str.toLowerCase();
		if((str.startsWith("$") || str.startsWith("£"))){
			if((str.endsWith("k") || str.endsWith("m")) && str.length() > 2 && isNumerical(str.substring(1, str.length()-1)))
				return true;
			else if(str.length() > 1 && isNumerical(str.substring(1, str.length())))
				return true;
		}
		return false;
	}
	
	public static boolean isEnumeration(String str){
		str = str.toLowerCase();
		if(str.endsWith("1st") || str.endsWith("2nd") || str.endsWith("3rd") || str.endsWith("th")){
			if(str.length() > 2 && isNumber(str.substring(0, str.length()-2)))
				return true;
		}
		return false;
	}
	
	@Override
	public void edit(Graph graph) {
		String lemma;
		for(Node n : graph.getNodes()){
			lemma = n.getLemma();
			if(isNumberWord(lemma))
				lemma = "[[numword]]";
			else if(isMoney(lemma))
				lemma = "[[money]]";
			else if(isEnumeration(lemma))
				lemma = "[[enumeration]]";
			else if(isNumerical(lemma))
				lemma = "[[numerical]]";
			
			n.setLemma(lemma);
		}
	}
	
}

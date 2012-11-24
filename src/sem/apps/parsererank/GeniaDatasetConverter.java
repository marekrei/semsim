package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Arrays;

import sem.exception.GraphFormatException;
import sem.graph.Graph;
import sem.graph.Node;
import sem.graphreader.ParsevalGraphReader;
import sem.util.FileReader;

/**
 * We create a tokenised version of the Genia-GR dataset.
 * This is required because the dataset is not annotated for all tokens and contains complex terms like 'constitutive_nuclear_expression'.
 * In order to evaluate on this dataset, we want RASP to also treat this as a complex term, hopefully as an unknown word with a correct POS.
 * Therefore, we create a tokenised version where these complex terms are retained.
 *
 */
public class GeniaDatasetConverter {
	
	/**
	 * Check that the input dataset has a valid format
	 * @param inputPath
	 */
	public static void validate(String inputPath){
		ArrayList<String> grsWithSubtype = new ArrayList<String>(Arrays.asList("dependent", "mod", "ncmod", "xmod", "cmod", "arg_mode", "arg", "xcomp", "ccomp", "ta"));
		ArrayList<String> grsWithInitialGr = new ArrayList<String>(Arrays.asList("subj", "ncsubj", "xsubj", "csubj"));
	
		FileReader fr = new FileReader(inputPath);
		
		while(fr.hasNext()){
			String line = fr.next().trim();
			if(!line.startsWith("("))
				continue;
			
			line = line.substring(1, line.length()-1);
			String[] arguments = line.split("\\s+");
			
			String type = arguments[0];
			if(grsWithSubtype.contains(type)){
				if(grsWithInitialGr.contains(type))
					throw new RuntimeException("Not allowed");
				if(arguments.length != 4)
					throw new RuntimeException("Wrong number of arguments: " + line);
				if(arguments[2].equals("_") || arguments[3].equals("_"))
					throw new RuntimeException("Head/dep is '_': " + line);
			}
			else if(type.equals("passive")){
				if(arguments.length != 2)
					throw new RuntimeException("Wrong number of arguments: " + line);
				if (arguments[1].equals("_"))
					throw new RuntimeException("Head/dep is '_': " + line);
			}
			else {
				if(grsWithInitialGr.contains(type)){
					if(grsWithSubtype.contains(type))
						throw new RuntimeException("Not allowed");
					if(arguments.length != 4)
						throw new RuntimeException("Wrong number of arguments: " + line);
				}
				else {
					if(arguments.length != 3)
						throw new RuntimeException("Wrong number of arguments: " + line);
					if(arguments[1].equals("_") || arguments[2].equals("_"))
						throw new RuntimeException("Head/dep is '_': " + line);
				}
				
			}
		}
	}

	public static void run(String inputPath){
		ParsevalGraphReader r = null;
		try {
			r = new ParsevalGraphReader(inputPath, false, true);
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		
		while(r.hasNext()){
			Graph g = null;
			try {
				g = r.next();
			} catch (GraphFormatException e) {
				throw new RuntimeException(e);
			}

			String[] lines = g.getMetadata("text").trim().split("\n");
			if(lines.length != 2){
				System.out.println("Error. Wrong number of lines: " + lines.length);
				System.out.println(g.getMetadata("text"));
				System.exit(1);
			}
			
			ArrayList<String> tokens1 = new ArrayList<String>(Arrays.asList(lines[1].trim().split("\\s+")));
			if(tokens1.get(0).replace("#", "").length() == 0)
				tokens1.remove(0);
			
			ArrayList<String> tokens2 = new ArrayList<String>();
			for(Node n : g.getNodes()){
				if(!tokens2.contains(n.getLemma()))
					tokens2.add(n.getLemma());
			}
			
			// We use the IDs in the dataset to map them correctly to tokens.
			
			String string = "";
			loop: for(int i = 1; i <= tokens1.size(); i++){
				for(String token2 : tokens2){
					if(token2.contains(":"+i+":")){
						int lastColon = token2.lastIndexOf(":");
						int firstColon = token2.lastIndexOf(":", lastColon-1);
						Integer end = Integer.parseInt(token2.substring(lastColon+1));
						Integer start = Integer.parseInt(token2.substring(firstColon+1, lastColon));
						String str = token2.substring(0, firstColon);
						
						if(start != i){
							throw new RuntimeException("Something went wrong here.");
						}
						
						i = end;
						string += str + " ";
						tokens2.remove(token2);
						continue loop;
					}
				}
				
				string += tokens1.get(i-1) + " ";
			}
			
			for(String token2 : tokens2){
				if(token2.contains("_")){
					throw new RuntimeException("A complex term still remained in tokens2: " + token2);
				}
			}
			
			String referenceSentence = "";
			for(String t : tokens1)
				referenceSentence += " " + t;
			referenceSentence = referenceSentence.trim();
			
			if(!referenceSentence.equals(string.replace("_", " ").trim()))
				throw new RuntimeException("The sentences don't match!\n"+string+"\n" + referenceSentence);
			
			string = string.replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<");
			System.out.println("^ " + string.trim());
		}
		
		
		r.close();
		
	}
	public static void main(String[] args) {
		GeniaDatasetConverter c = new GeniaDatasetConverter();
		if(args.length == 1){
			validate(args[0]);
			run(args[0]);
		}
		else {
			System.out.println("Usage: GeniaCorpusConverter <rasp-lisp format input file>");
			System.out.println("Output: Tokenised and sentence-split (^) plain text, suitable for using as RASP input");
		}
	}

}

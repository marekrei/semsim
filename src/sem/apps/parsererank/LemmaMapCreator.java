package sem.apps.parsererank;


import sem.util.FileReader;
import sem.util.FileWriter;
import sem.util.StringMap;
import sem.util.Tools;

/**
 * Creates a mapping between tokens in the input file and lemmas from RASP.
 * It is required because the RASP evaluation is done using tokens, but distributional models are built using lemmas.
 * Therefore, we need to match nodes between these two representations.
 * This approach is not 100% accurate -- a unique token can correspond to multiple different lemmas. But it is a close approximation for our small evaluation datasets.
 * In a practical application everything would be done using lemmas and the mapping to tokens would not be needed.
 *
 */
public class LemmaMapCreator {

	public static void run(String inputPath, String tempPrefix, String raspCommand, String outputPath){
		
		String simPath = tempPrefix + ".1";
		String lemPath = tempPrefix + ".2";
		
		//
		// Take as input a tokenised file. 
		// Produce a simplified version where compound words containing an underscore '_' will be replaced with the last word.
		// For example, 'big_yellow_house' becomes 'house'
		//
		
		FileReader fr = new FileReader(inputPath);
		FileWriter fw = new FileWriter(simPath);
		while(fr.hasNext()){
			String[] tokens = fr.next().trim().split("\\s+");
			String modifiedLine = "";
			for(int i = 0; i < tokens.length; i++){
				if(tokens[i].contains("_")){
					String[] tokenBits = tokens[i].split("_");
					modifiedLine += tokenBits[tokenBits.length-1] + " ";
				}
				else{
					modifiedLine += tokens[i] + " ";
				}
			}
			fw.writeln(modifiedLine.trim());
		}
		fr.close();
		fw.close();
		
		
		//
		// Run the lemmatisation process with RASP
		//
		
		Tools.runCommand(raspCommand + " < " + simPath + " > " + lemPath);
		
		//
		// Read everything back in and match the tokens
		//
		
		StringMap stringMap = new StringMap();
		fr = new FileReader(inputPath);
		FileReader fr2 = new FileReader(lemPath);
		String tempString;
		while(fr.hasNext()){
			String tokenLine = fr.next();
			String lemmaLine = fr2.next();
			
			String[] tokens = tokenLine.trim().split("\\s+");
			String[] lemmas = lemmaLine.trim().split("\\s+");
			
			if(tokens.length != lemmas.length){
				System.err.println("Number of tokens does not match number of lemmas! : " + tokens.length + " " + lemmas.length);
				System.err.println(tokenLine);
				System.err.println(lemmaLine);
				System.exit(1);
			}
			
			for(int i = 0; i < tokens.length; i++){
				if(tokens[i].equals("^"))
					continue;
				tempString = lemmas[i].substring(0, lemmas[i].lastIndexOf('_'));
				if(tempString.contains("+") && !tokens[i].contains("+"))
					tempString = tempString.substring(0, tempString.lastIndexOf('+'));
				stringMap.put(tokens[i], tempString);
			}
		}
		
		fr.close();
		fr2.close();
		
		stringMap.save(outputPath);
	}

	
	public static void main(String[] args) {
		
		String raspCommand = "rasp_parse=cat rasp_sentence=cat rasp_tokenise=cat /local/scratch/mr472/rasp3/scripts/rasp.sh ";
		
		if(args.length == 3){
			run(args[0], args[1], raspCommand, args[2]);
		}
		else {
			System.out.println("Usage: LemmaMapCreator <tokenised input text file> <prefix for temporary files> <output stringmap location>");
		}

	}

}

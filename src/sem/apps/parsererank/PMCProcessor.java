package sem.apps.parsererank;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import sem.util.FileReader;
import sem.util.FileWriter;
import sem.util.Tools;
import sem.util.XmlReader;

class Article{
	public String titleText;
	public String abstractText;
	public String bodyText;
	
	public Article(String titleText, String abstractText, String bodyText){
		this.titleText = titleText;
		this.abstractText = abstractText;
		this.bodyText = bodyText;
	}
}

/**
 * Processes the PubMed Central XML files, extracts files that match a certain criteria, and parses them with RASP.
 *
 */
public class PMCProcessor {
	
	/**
	 * Format a string by adding punctuation.
	 * Some lines (e.g. title) contain a sentence but no punctuation at the end. This confuses the sentence splitter.
	 * We add a period if sentence-ending punctuation is not found.
	 * 
	 * @param input
	 * @return
	 */
	public String format(String input){
		input = input.trim();
		String output = "";
		for(String s : input.split("\n")){
			String str = s.trim();
			if(str.length() > 0){
				if(!str.endsWith(".") && !str.endsWith("?") && !str.endsWith("!"))
					str += " . ";
				output += str + "\n";
			}
		}
		return output.trim();
	}

	/**
	 * Reads the input file and returns an Article object with title, abstract and body text separated.
	 * 
	 * @param file
	 * @return
	 */
	public Article process(String file){
		XmlReader xmlReader = new XmlReader(file);
		String titleText = "";
		String abstractText = "";
		String bodyText = "";

		int position = 0;
		
		ArrayList<String> path = new ArrayList<String>();
		
		while(xmlReader.hasNext()){
			xmlReader.next();
			
			if(xmlReader.isStartElement()){
				String tag = xmlReader.getLocalName();
				path.add(tag);
				if(tag.equals("article-title") && path.contains("article-meta"))
					position = 1;
				else if(tag.equals("abstract"))
					position = 2;
				else if(tag.equals("body"))
					position = 3;
				else if(tag.equals("p")){
					if(position == 1)
						titleText += "\n";
					else if(position == 2)
						abstractText += "\n";
					else if(position == 3)
						bodyText += "\n";
				}
			}
			else if(xmlReader.isEndElement()){
				String tag = xmlReader.getLocalName();
				path.remove(path.size()-1);
				if(tag.equals("article-title") || tag.equals("abstract") || tag.equals("body"))
					position = 0;
				else if(tag.equals("p")){
					if(position == 1)
						titleText += "\n";
					else if(position == 2)
						abstractText += "\n";
					else if(position == 3)
						bodyText += "\n";
				}
			}
			else if(xmlReader.isCharacters()){
				if(position == 1)
					titleText += xmlReader.getText();
				else if(position == 2)
					abstractText += xmlReader.getText();
				else if(position == 3)
					bodyText += xmlReader.getText();
			}
		}
		xmlReader.close();
		
		return new Article(titleText, abstractText, bodyText);
	}
	
	/**
	 * Given a path, list all files in that path recursively.
	 * Returns an ArrayList of absolute paths.
	 * @param path
	 * @return
	 */
	public ArrayList<String> listFilesRec(String path){
		File f = new File(path);
		ArrayList<String> files = new ArrayList<String>();
		for(File s : f.listFiles()){
			if(s.isDirectory())
				files.addAll(listFilesRec(s.getAbsolutePath()));
			else if(s.isFile())
				files.add(s.getAbsolutePath());
		}
		return files;
	}
	

	
	/**
	 * Iterates through all the PMC XML files, keeps the ones that contains word in the filter,
	 * and prints plain text versions of the articles.
	 * The abstract and body go into separate directories, as specified.
	 * Errors (e.g. formatting problems) are reported to standard output but the process is not halted.
	 * 
	 * @param filter		ArrayList of words that get matched (lowercased)
	 * @param dir			Path to the PMC directory
	 * @param absPath		Directory for outputting abstracts
	 * @param bodyPath		Directory for outputting article bodies
	 */
	public void runExtraction(ArrayList<String> filter, String dir, String absPath, String bodyPath){
		PMCProcessor pmcReader = new PMCProcessor();
		int count = 0, countP = 0;
		ArrayList<String> files = pmcReader.listFilesRec(dir);
		System.out.println("Total number of files to process: " + files.size());
		for(String file : files){
			count++;
			Article article = null;
			
			// Get the article
			try{
				article = pmcReader.process(file);
			} catch(Exception e){
				System.out.println("Caught exception with file " + file + "\n" + e.getMessage());
				e.printStackTrace();
				continue;
			}
			
			// Check if it matches one of the filter words
			String allText = article.titleText + "\n" + article.abstractText + "\n" + article.bodyText;
			allText = allText.toLowerCase();
			
			boolean passesFilter = false;
			for(String filterWord : filter){
				if(allText.contains(filterWord.toLowerCase())){
					passesFilter = true;
					break;
				}
			}
			
			// If it matches, print the title+abstract and body to separate files.
			if(passesFilter){
				countP++;
				int i1 = file.lastIndexOf('/');
				int i2 = file.lastIndexOf('.');
				String newName = file.substring(i1 + 1, i2);
				
				FileWriter fw1 = new FileWriter(absPath + "/" + newName + ".txt");
				fw1.write(format(article.titleText) + "\n" + format(article.abstractText));
				fw1.close();
				
				FileWriter fw2 = new FileWriter(bodyPath + "/" + newName + ".txt");
				fw2.write(format(article.bodyText));
				fw2.close();
			}
			if(count % 100 == 0) 
				System.out.println("" + countP + "/" + count);
		}
		System.out.println("Processed " + count + " files, retained " + countP);
	}
	
	/**
	 * Run parsing, taking input files from one dir and printing the output to the other.
	 * 
	 * @param parserCommand
	 * @param fromDir
	 * @param toDir
	 */
	public void runParsing(String parserCommand, String fromDir, String toDir){
		ArrayList<String> files = listFilesRec(fromDir);
		int count = 0;
		for(String file : files){
			int i1 = file.lastIndexOf('/');
			int i2 = file.lastIndexOf('.');
			String newName = file.substring(i1 + 1, i2);
			count++;
			
			System.out.println(count + " / " + files.size() + " : " + newName);
			Tools.runCommand(parserCommand + " < '" + file + "' > '" + toDir + "/" + newName + ".xml'"); 
		}
		
	}

	public static void main(String[] args) {
		String pmcDir = "/local/scratch/mr472/corpora/PMCOA/";
		String absPath = "/local/scratch/mr472/corpora/PMCOA-filtered3/plaintext/abs/";
		String bodyPath = "/local/scratch/mr472/corpora/PMCOA-filtered3/plaintext/body/";
		String absParsedPath = "/local/scratch/mr472/corpora/PMCOA-filtered3/parsed/abs/";
		String bodyParsedPath = "/local/scratch/mr472/corpora/PMCOA-filtered3/parsed/body/";
		String parserCommand = "/local/scratch/mr472/rasp3/scripts/rasp.sh -m -p'-mg -pr'";
		ArrayList<String> filter = new ArrayList<String>(Arrays.asList("nf-kappa b", "nf-kappab", "nf kappa b", "nf-kappa_b", "nf-kb", "nf-κb"));
		
			
		PMCProcessor pmcReader = new PMCProcessor();
		pmcReader.runExtraction(filter, pmcDir, absPath, bodyPath);
		pmcReader.runParsing(parserCommand, absPath, absParsedPath);
		pmcReader.runParsing(parserCommand, bodyPath, bodyParsedPath);
	}
}

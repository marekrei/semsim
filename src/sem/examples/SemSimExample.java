package sem.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import sem.exception.GraphFormatException;
import sem.exception.SemModelException;
import sem.graph.Graph;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspGraphReader;
import sem.model.SemModel;
import sem.model.VectorSpace;
import sem.sim.SimFinder;
import sem.sim.SimMeasure;
import sem.util.Tools;

/**
 * Example class.
 * Shows how to build the vector space model from an input file, and how to access the co-occurrence information and similarity scores.
 *
 */
public class SemSimExample {
	public static void main(String[] args) {
		try {
			// First, let's build the model
			// Using the SemGraph library to read in the dependency graphs
			GraphReader reader = new RaspGraphReader("examples/rasp/pnp_1000.rasp.gz", false);
			
			// Creating a new empty model
			SemModel semModel = new SemModel(false);
			
			// initializing some graph editors. They can be used to clean up the graphs, but are not required.
			ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(new LowerCaseGraphEditor(), new NumTagsGraphEditor()));
			
			// Adding all the graphs to the model
			while(reader.hasNext()){
				Graph graph = reader.next();
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);
				semModel.add(graph);
			}
			reader.close();
			
			// We have now finished and can access different statistics
			System.out.println("The word \"humour_NN1\" occurs " + semModel.getNodeCount("humour_NN1") +" times in the text.");
			System.out.println("The triple (handsome_JJ, ncmod, wonderfully_RR) occurs " + semModel.getTripleCount("handsome_JJ", "ncmod", "wonderfully_RR") + " times."); 
			System.out.println("handsome_JJ occurs as a head in a dependency relation " + semModel.getTripleCount("handsome_JJ", null, null) + " times."); 
			
			// If we wish, we can save and load the model
			//semModel.save("mymodel");
			//semModel = new SemModel(false, "mymodel");
			
			// We make the tensor symmetric. For saving both memory and disk space, the relations are only saved in one direction (head,>rel,dep). However, for our vector space, we might want to use (dep,<rel,head) as well.
			semModel.makeTensorSymmetric();
			
			// We construct a new vector space, using the PMI weighting scheme. The PMI_LIM scheme discards features that occur only once.
			VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true);
			
			// Constructing a new SimFinder object which will help us find similarities.
			SimFinder simFinder = new SimFinder(vectorSpace);
			System.out.println("Similarity score for cosine(handsome_JJ, pretty_JJ): " + simFinder.getScore(SimMeasure.COSINE, "handsome_JJ", "pretty_JJ"));
			System.out.println("Similarity score for clarkeDE(handsome_JJ, pretty_JJ): " + simFinder.getScore(SimMeasure.CLARKE_DE, "handsome_JJ", "pretty_JJ"));
			System.out.println("Similarity score for clarkeDE(pretty_JJ, handsome_JJ): " + simFinder.getScore(SimMeasure.CLARKE_DE, "pretty_JJ", "handsome_JJ"));
			
			// Finding the most similar words to "woman_NN1"
			// First, we add it to the list of "main" words. This would usually contain all the words that we want to run hyponym generation on.
			LinkedHashSet<String> mainWords = new LinkedHashSet<String>();
			mainWords.add("woman_NN1");
			
			// Now we create the set of "candidate" words. These will be considered as possible hyponyms to the main words.
			// Various filtering techniques are possible. A smaller set will speed up the process but risks discarding true hyponyms.
			// Here we select all words that occur at least 5 times.
			LinkedHashSet<String> candidateWords = new LinkedHashSet<String>();
			for(String word : semModel.getNodeIndex().getIdMap().keySet()){
				if(semModel.getNodeCount(word) >= 5){
					candidateWords.add(word);
				}
			}
			
			// We run the scoring, getting back a hashmap with all the similarity scores.
			LinkedHashMap<String,LinkedHashMap<String,Double>> scores = simFinder.getScores(mainWords, candidateWords, SimMeasure.COSINE, false, 1);
			
			// Now we just sort the results and print them out.
			// The results are not especially accurate as we are using a very small corpus for this example.
			System.out.println("The most similar words to woman_NN1:");
			int count = 0;
			for(Entry<String,Double> e : Tools.sort(scores.get("woman_NN1"), true).entrySet()){
				System.out.println(e.getKey() + "\t" + e.getValue());
				if(++count >= 10)
					break;
			}
			
		} catch (GraphFormatException e) {
			e.printLine();
			e.printStackTrace();
		} catch (SemModelException e) {
			e.printStackTrace();
		}
	}
}

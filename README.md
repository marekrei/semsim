SemSim
=========

Author:		Marek Rei (marek@marekrei.com)

Version:	0.1

Updated:	2012-11-24

Homepage:	<http://www.marekrei.com/projects/semsim/>

Download:	<https://github.com/marekrei/semsim>

Documentation:	<http://www.marekrei.com/doc/semsim/0.1/>


About
-----

SemSim is a Java library for creating semantic distributional models from text, and calculating semantic similarity scores between words.
It builds on the [SemGraph](https://github.com/marekrei/semgraph) library which can read the output of different dependency parsers.
A corpus of parsed text can be used to create a distributional model, which in turn can be used to create distributional feature vectors. 
Finally, these vectors are the basis for finding the distributional similarity between two words.
For example, this library can be used to find how semantically similar are words 'music' and 'song', or what are the most similar words to 'music'.


Usage
------

There are a few basic operations that can be performed directly from the jar file.

You can build a model like this:

	BuildModel <corpustype> <corpuspath> <outputpath>
	
For example:

	java -cp semsim-0.1.jar:lib/trove-3.0.2.jar:semgraph-0.3.jar sem.run.BuildModel rasp examples/rasp/pnp_1000.rasp.gz mymodel

This will create a distributional model into your working directory, in the form of several files with the prefix 'mymodel'. 
When working with large datasets, you may need more memory. I was using 15GB of memory (-Xmx15G as Java argument) when creating a model from the British National Corpus.

Next, we can find the similarity between two words:

	Similarity <modelpath> <similaritytype> [word1] [word2]
	
For example:

	java -cp semsim-0.1.jar:lib/trove-3.0.2.jar:semgraph-0.3.jar sem.run.Similarity mymodel cosine dance_NN1 ball_NN1
		
	0.04313027223556267

This will load the SemModel prefixed by 'mymodel', and find the cosine similarity between dance_NN1 and ball_NN1. 
Alternatively, you can run it in a loop by not providing the last two arguments. In this case, the method will wait for your input (two) words, output the similarity, and wait for more.

	java -cp bin:lib/trove.jar:lib/semgraph.jar sem.run.Similarity mymodel cosine

Many different similarity measures are implemented (e.g. dice, jaccard, lin, balAPinc, balPrec, clarkeDE, etc.), see the SimMeasure class for a complete list.

Finally, if we want to find the top most similar words for a specific word, we can run:

	MostSimilar <modelpath> <similaritytype> <frequencylimit> <resultlimit> [word1]
	
For example:

	java -cp semsim-0.1.jar:lib/trove-3.0.2.jar:semgraph-0.3.jar sem.run.MostSimilar mymodel cosine 5 5 man_NN1
	
	man_NN1	1.0
	lady_NN1	0.3020134423952073
	woman_NN1	0.29328699273666575
	people_NN	0.27926840361596206
	subject_NN1	0.2664863271668703
	
This loads the SemModel prefixed by 'mymodel' and find words that are most similar to man_NN1, using cosine similarity. Frequencylimit sets the minimum frequency of a candidate word - in this case, words that occurred less than 5 times were not considered at all. Resultlimit sets the number of results to return.
This can also be run in a loop without terminating, by omitting the last argument:

	java -cp bin:lib/trove.jar:lib/semgraph.jar sem.run.MostSimilar mymodel cosine 5 5
	
	
In addition to these cases, the library can be included into your code and used in much more flexible ways. 
Take a look at sem.examples.SemSimExample to see how the classes can be used:

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


Dependencies
------------

The SemGraph library is needed for reading the dependency graphs and for various generic classes: <https://github.com/marekrei/semgraph>

Trove is needed for building the model, as we use their efficient hashmap implementations: <http://trove.starlight-systems.com/>

The JUnit library needs to be included for the unit tests: <http://junit.sourceforge.net/>


Changes
-------

**0.1**

* Initial release


License
-------

This software is distributed under the GNU Affero General Public License version 3. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. The authors are not responsible for how it performs (or doesn't). See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

If you wish to use this software under a different license, feel free to contact me.

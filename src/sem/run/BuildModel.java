package sem.run;

import sem.graph.Graph;
import sem.graphreader.GraphReader;
import sem.graphreader.GraphReaderType;
import sem.model.SemModel;

/**
 * Builds a SemModel
 *
 */
public class BuildModel {
	public static void main(String[] args) {
		if(args.length == 3){
			try{
				GraphReaderType graphReaderType = GraphReaderType.getType(args[0]);
				String corpusPath = args[1];
				String outputPath = args[2];
				
				if(graphReaderType == null)
					throw new RuntimeException("Invalid corpus type");
				GraphReader reader = graphReaderType.makeGraphReader(corpusPath);
				
				// Creating a new empty model
				SemModel semModel = new SemModel(false);

				// Adding all the graphs to the model
				while(reader.hasNext()){
					Graph graph = reader.next();
					semModel.add(graph);
				}
				reader.close();
				
				semModel.save(outputPath);
				
			} catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		else {
			System.out.println("BuildModel <corpustype> <corpuspath> <outputpath>");
		}
	}

}

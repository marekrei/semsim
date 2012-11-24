package sem.apps.hypgen;

import java.util.ArrayList;
import java.util.Arrays;

import sem.graph.Graph;
import sem.grapheditor.AddNodesGraphEditor;
import sem.grapheditor.BypassAdpGraphEditor;
import sem.grapheditor.BypassConjGraphEditor;
import sem.grapheditor.CombineSubjObjGraphEditor;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.exception.GraphFormatException;
import sem.exception.SemModelException;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspXmlGraphReader;
import sem.model.SemModel;

public class ModelBuilder {

	public static void createModel(GraphReader graphReader, ArrayList<GraphEditor> graphEditors, String outputPath){
		try{
			System.out.println("Creating Model....");

			SemModel semModel = new SemModel(true);
			int count = 0;
			Graph graph = null;
			while(graphReader.hasNext()){
				count++;
				try{
					graph = graphReader.next();
				} catch(GraphFormatException e){
					// Some corpora contain format errors.
					// We catch and report them, but we'll skip these cases rather than halting.
					System.out.println("ERR:" + e.getMessage() + "\nGraph: " + count + "\nLine: " + e.getLine());
					continue;
				}
				
				// We also skip graphs that have an xparse, indicating that the parser failed.
				if(graph.hasMetadata() && graph.getMetadata("xparse") != null && graph.getMetadata("xparse").equals("true")){
					System.out.println("ERR: xparse");
					continue;
				}

				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);
				
				semModel.add(graph);
				
				if(count % 50000 == 0)
					System.out.println(count);
			}
			graphReader.close();
			System.out.println("Total: " + count);
			semModel.save(outputPath);
		} catch (SemModelException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		
		GraphReader graphReader;
		String outputPath;
		ArrayList<GraphEditor> graphEditors;
		
		String posMapPath = "/auto/homes/mr472/Documents/Projects/SemSim/tagsets/claws2-universal.txt";
		
		/*
		 *  Model1
		 *  Source: BNC/tsv (/anfs/bigdisc/mr472/corpora/BNC/tsv/)
		 */
		/*
		try {
			graphReader = new TSVGraphReader("/anfs/bigdisc/mr472/corpora/BNC/tsv/", false);
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		outputPath = "/anfs/bigdisc/mr472/semsim_models/model1";
		graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_POSMAP),
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor()
				));
		*/
		
		/*
		 * Model2
		 * Replicating the old model for parse reranking (S1-P0L1T1C1A3V4N4)
		 * Source: BLLIP
		 */
		/*
		try {
			graphReader = new RaspXmlGraphReader("/anfs/bigdisc/mr472/corpora/BLLIP/bllip.rasp.xml.gz", RaspXmlGraphReader.NODES_ALL, false, true);
			//graphReader = new TSVGraphReader("/anfs/bigdisc/mr472/corpora/BLLIP/bllip-v1.tsv.gz", false); 
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		outputPath = "/anfs/bigdisc/mr472/semsim_models/model2";
		graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				//new ConvertPosGraphEditor(ConvertPosGraphEditor.CONVERSION_NONE),
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor(),
				new BypassConjGraphEditor(),
				new BypassAdpGraphEditor(3),
				new CombineSubjObjGraphEditor(4),
				new AddNodesGraphEditor(4)//,
				//new ReverseEdgesGraphEditor(),
				//new NullEdgesGraphEditor()
				));
		*/
		
		/*
		 * Model 3
		 * A slighly modified version of model2. Using RaspXmlGraphReader.NODES_TOKENS.
		 * Source: BLLIP
		 */
		/*
		try {
			graphReader = new RaspXmlGraphReader("/anfs/bigdisc/mr472/corpora/BLLIP/bllip.rasp.xml.gz", RaspXmlGraphReader.NODES_TOKENS, false, true);
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		outputPath = "/anfs/bigdisc/mr472/semsim_models/model3";
		graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor(),
				new BypassConjGraphEditor(),
				new BypassAdpGraphEditor(3),
				new CombineSubjObjGraphEditor(4),
				new AddNodesGraphEditor(4)
				));
		*/
		
		/*
		 * Model 4
		 * Model for running parse reranking experiments on genia-gr data.
		 * Source: PMCOA-subset. Filtered to contain any of ("nf-kappa b", "nf-kappab", "nf kappa b", "nf-kappa_b", "nf-kb", "nf-κb")
		 * Total: 1623846 graphs
		 */
		
		try {
			graphReader = new RaspXmlGraphReader("/local/scratch/mr472/corpora/PMCOA-subset/parsed/", RaspXmlGraphReader.NODES_TOKENS, false, true);
		} catch (GraphFormatException e) {
			throw new RuntimeException(e);
		}
		outputPath = "/anfs/bigdisc/mr472/semsim_models/model4";
		graphEditors = new ArrayList<GraphEditor>(Arrays.asList(
				new LowerCaseGraphEditor(),
				new NumTagsGraphEditor(),
				new BypassConjGraphEditor(posMapPath),
				new BypassAdpGraphEditor(3, posMapPath),
				new CombineSubjObjGraphEditor(4, posMapPath),
				new AddNodesGraphEditor(4)
				));
		
		
		
		
		createModel(graphReader, graphEditors, outputPath);
	}

}

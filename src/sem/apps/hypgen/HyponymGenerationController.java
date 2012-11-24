package sem.apps.hypgen;

import java.io.File;
import java.util.Arrays;

import sem.sim.SimMeasure;
import sem.util.FileReader;
import sem.util.FileWriter;

/**
 * Some additional helper functions for hyponym generation, for creating Condor jobs and collecting the results.
 *
 */
public class HyponymGenerationController {

	/**
	 * @param args
	 */
	public static void createSubmitFile() {
		FileWriter fw = new FileWriter("/mnt/maun/anfs/bigdisc/mr472/condor/hypgen/hypgen.submit");
		
		fw.writeln("universe       = java");
		fw.writeln("executable     = semsim1.jar");
		fw.writeln("jar_files      = semsim1.jar lib/trove-3.0.2.jar");
		fw.writeln("requirements   = Memory >= 10300");
		//fw.writeln("# && Cpus >= 2");
		fw.writeln("java_vm_args   = -Xmx10G -Dfile.encoding=UTF-8");
		fw.writeln("nice_user      = True");
		int threads = 1;
		
		
		for(String pos : Arrays.asList("noun", "verb")){
			for(String set : Arrays.asList("train", "dev", "test")){
				for(SimMeasure simMeasure : SimMeasure.values()){
					String measure = simMeasure.getLabel();
					String inputFile = "/anfs/bigdisc/mr472/corpora/HyponymGen/hyponym-generation-"+pos+"-"+set+".txt";
					String modelPath = "/anfs/bigdisc/mr472/SemTensor/model1";
					int minFreq = 10;
					String outputPath = "/anfs/bigdisc/mr472/condor/hypgen/extra/" +pos + "-"+set+"-"+measure + ".list";
					fw.writeln("");
					
					fw.writeln("arguments      = sem.apps.HyponymGeneration "+measure+" "+pos+" "+inputFile + " " + modelPath + " " +minFreq + " " + threads + " " + outputPath);
					fw.writeln("output         = output/" + pos + "-"+set+"-"+measure+".output");
					fw.writeln("error          = error/" + pos + "-"+set+"-"+measure+".error");
					
					fw.writeln("queue");
				}
			}
		}
		
		fw.close();
	}
	
	public static void collectResults(){
		for(SimMeasure similarityType : SimMeasure.values()){
			String measure = similarityType.getLabel();
			System.out.print(measure);
			for(String pos : Arrays.asList("noun", "verb")){
				for(String set : Arrays.asList("train", "dev", "test")){
					
					String filename = "/anfs/bigdisc/mr472/condor/hypgen/output/" + pos + "-"+set+"-"+measure+".output";
					String result = "XXXXX";
					if((new File(filename)).exists()){
						FileReader fr = new FileReader(filename);
						while(fr.hasNext()){
							String line = fr.next().trim();
							if(line.length() > 0 && !line.startsWith("#")){
								if(!result.startsWith("X"))
									throw new RuntimeException("error 1");
								result = line.trim();
							}
						}
						fr.close();
						
					}
					System.out.print("\t" + result);
				}
			}
			System.out.println();
		}
	}

	
	public static void main(String[] args){
		//createSubmitFile();
		collectResults();
	}
}





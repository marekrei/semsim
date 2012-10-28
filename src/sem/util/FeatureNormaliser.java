package sem.util;

import java.util.HashMap;

/**
 * Normalises all the features in a standard SVM input file format, given a reference file and an input file
 *
 */
public class FeatureNormaliser {
	public static void run(int normType, String referenceFile, String inputFile, String outputFile){
		HashMap<Integer,Double> sums = new HashMap<Integer,Double>();
		HashMap<Integer,Double> squaresums = new HashMap<Integer,Double>();
		HashMap<Integer,Double> max = new HashMap<Integer,Double>();
		HashMap<Integer,Double> min = new HashMap<Integer,Double>();
		HashMap<Integer,Integer> count = new HashMap<Integer,Integer>();
		
		// Collecting statistics from the reference file
		FileReader refReader = new FileReader(referenceFile);
		while(refReader.hasNext()){
			String[] line = refReader.next().trim().split("\\s+");
			if(line.length <= 0)
				continue;
			for(int i = 1; i < line.length; i++){
				String[] f = line[i].trim().split(":");
				if(f.length != 2)
					throw new RuntimeException("Illegal number of chunks");
				Integer id = Integer.parseInt(f[0]);
				Double value = Double.parseDouble(f[1]);
				
				if(!sums.containsKey(id))
					sums.put(id, 0.0);
				sums.put(id, sums.get(id) + value);
				
				if(!squaresums.containsKey(id))
					squaresums.put(id, 0.0);
				squaresums.put(id, squaresums.get(id) + Math.pow(value, 2.0));
				
				if(!max.containsKey(id) || max.get(id) < value)
					max.put(id, value);
				
				if(!min.containsKey(id) || min.get(id) > value)
					min.put(id, value);
				
				if(!count.containsKey(id))
					count.put(id, 0);
				count.put(id, count.get(id) + 1);
			}
		}
		refReader.close();
		
		// Transforming the input file
		FileReader inputReader = new FileReader(inputFile);
		FileWriter outputWriter = new FileWriter(outputFile);
		while(inputReader.hasNext()){
			String[] line = inputReader.next().trim().split("\\s+");
			if(line.length <= 0)
				continue;
			outputWriter.write(line[0]);
			for(int i = 1; i < line.length; i++){
				String[] f = line[i].trim().split(":");
				if(f.length != 2)
					throw new RuntimeException("Illegal number of chunks");
				Integer id = Integer.parseInt(f[0]);
				Double value = Double.parseDouble(f[1]);
				
				if(!count.containsKey(id))
					continue;
				
				if(normType == 0){
					outputWriter.write(" " + id + ":" + value);
				}
				else if(normType == 1){
					if(count.get(id) < 2)
						continue;
					double mean = sums.get(id) / count.get(id);
					double range = max.get(id) - min.get(id);
					if(range == 0.0)
						continue;
					double newValue = (value - mean) / range;
					outputWriter.write(" " + id + ":" + newValue);
				}
				else if(normType == 2){
					if(count.get(id) < 2)
						continue;
					double mean = sums.get(id) / count.get(id);
					double sdev = squaresums.get(id) - (2.0 * mean * sums.get(id)) + (count.get(id) * mean * mean);
					if(sdev == 0.0)
						continue;
					if(sdev < 0.0)
						throw new RuntimeException("Problems");
					sdev = Math.sqrt(sdev / (count.get(id) - 1));
					double newValue = (value - mean) / sdev;
					outputWriter.write(" " + id + ":" + newValue);
				}
			} 
			outputWriter.writeln("");
		}
		inputReader.close();
		outputWriter.close();
	}
}

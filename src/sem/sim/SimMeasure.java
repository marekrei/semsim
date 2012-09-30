package sem.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import sem.util.Tools;

/**
 * Contains all the similarity functions.
 */
public enum SimMeasure {
	COSINE("cosine", false),
	PEARSON("pearson", false),
	SPEARMAN("spearman", false),
	JACCARD_SET("jaccardSet", false),
	LIN("lin", false),
	DICE_SET("diceSet", false),
	OVERLAP_SET("overlapSet", false),
	COSINE_SET("cosineSet", false),
	JACCARD_GEN("jaccardGen", false),
	DICE_GEN("diceGen", false),
	DICE_GEN_2("diceGen2", false),
	KENDALLS_TAU("kendallsTau", false),
	CLARKE_DE("clarkeDE", false),
	WEEDS_PREC("weedsPrec", false),
	WEEDS_REC("weedsRec", false),
	WEEDS_F("weedsF", false),
	AP("ap", false),
	AP_INC("apInc", false),
	BAL_AP_INC("balAPInc", false),
	LIN_D("linD", false),
	BAL_PREC("balPrec", false),
	KL_DIVERGENCE("klDivergence", true),
	KL_DIVERGENCE_R("klDivergenceR", true),
	JS_DIVERGENCE("jsDivergence", true),
	ALPHA_SKEW("alphaSkew", true),
	ALPHA_SKEW_R("alphaSkewR", true),
	MANHATTAN("manhattan", true),
	EUCLIDEAN("euclidean", true),
	CHEBYSHEV("chebyshev", true),
	WEIGHTED_COSINE("weightedCosine", false),
	WEIGHTED_COSINE_2("weightedCosine2", false)
	;
	
	private final String label;
	private final boolean isDistance;

	private SimMeasure(String label, boolean isDistance){
		this.label = label;
		this.isDistance = isDistance;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public static SimMeasure getType(String label){
		for(SimMeasure simMeasure : SimMeasure.values())
			if(simMeasure.getLabel().equalsIgnoreCase(label))
				return simMeasure;
		return null;
	}
	
	public boolean isDistance(){
		return this.isDistance;
	}
	
	
	
	public static void validateVectors(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		if(a == null || b == null)
			throw new IllegalArgumentException("Vectors cannot be null");
	}
	
	public static void validateResult(double result){
		if(Double.isInfinite(result))
			throw new RuntimeException("Similarity is infinite");
		if(Double.isNaN(result))
			throw new RuntimeException("Similarity is NaN");
	}
	
	public static HashMap<Integer,Double> fillVector(HashMap<Integer,Double> mainVector, HashMap<Integer,Double> referenceVector){
		HashMap<Integer,Double> newVector = new HashMap<Integer,Double>(mainVector);
		for(Integer key : referenceVector.keySet())
			if(!newVector.containsKey(key))
				newVector.put(key, 0.0);
		return newVector;
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Cosine_similarity
	 * @param a
	 * @param b
	 * @return
	 */
	public static double cosine(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return cosine(b, a);
		
		double aLength = 0.0;
		double bLength = 0.0;
		double dotProduct = 0.0;

		Double tempDouble;
		
		for(Entry<Integer,Double> entry : a.entrySet()){
			tempDouble = b.get(entry.getKey());
			if(tempDouble != null)
				dotProduct += entry.getValue() * tempDouble;
			aLength += entry.getValue() * entry.getValue();
		}
		
		for(Entry<Integer,Double> entry : b.entrySet())
			bLength += entry.getValue() * entry.getValue();
		
		double result;
		if(aLength == 0.0 || bLength == 0.0)
			result = 0.0;
		else
			result = dotProduct / Math.sqrt(aLength * bLength);
		validateResult(result);
		return result;
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient
	 * @param a
	 * @param b
	 * @return
	 */
	public static double pearson(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		HashMap<Integer,Double> a2 = fillVector(a, b);
		HashMap<Integer,Double> b2 = fillVector(b, a);
		
		double result = Tools.pearson(a2, b2);
		validateResult(result);
		return result;
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient
	 * @param a
	 * @param b
	 * @return
	 */
	public static double spearman(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		HashMap<Integer,Double> a2 = fillVector(a, b);
		HashMap<Integer,Double> b2 = fillVector(b, a);
		
		double result = Tools.spearman(a2, b2);
		validateResult(result);
		return result;
	}

	
	/**
	 * http://en.wikipedia.org/wiki/Jaccard_index
	 * @param a
	 * @param b
	 * @return
	 */
	public static double jaccardSet(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return jaccardSet(b, a);
		
		double intersectionSize = 0;
		HashSet<Integer> union = new HashSet<Integer>();
		Double bValue;
		
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && bValue != 0.0)
				intersectionSize++;
			if(e.getValue() != null && e.getValue() != 0.0)
				union.add(e.getKey());
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			if(e.getValue() != null && e.getValue() != 0.0)
				union.add(e.getKey());
		}
		
		double result;
		if(union.size() == 0)
			result = 0.0;
		else
			result = (double)intersectionSize / (double)union.size();
		validateResult(result);
		return result;
	}
	
	/**
	 * Looks at positive elements in the vector
	 * ﻿Lin, D. (1998). Automatic retrieval and clustering of similar words. Proceedings of the 17th international conference on Computational linguistics-Volume 2 (pp. 768–774). Association for Computational Linguistics. doi:10.3115/980432.980696
	 * @param a
	 * @param b
	 * @return
	 */
	public static double lin(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return lin(b, a);
		
		double aSum = 0.0;
		double bSum = 0.0;
		double combinedSum = 0.0;
		
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			if(e.getValue() <= 0.0)
				continue;
			bValue = b.get(e.getKey());
			if(bValue != null && bValue > 0.0)
				combinedSum += e.getValue() + bValue;
			aSum += e.getValue();
		}
		
		for(Entry<Integer,Double> e : b.entrySet())
			if(e.getValue() > 0.0)
				bSum += e.getValue();
		
		double result;
		if(aSum + bSum == 0.0)
			result = 0.0;
		else
			result = combinedSum / (aSum + bSum);
		validateResult(result);
		return result;
	}
	
	
	public static double diceSet(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return diceSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && bValue != 0.0)
				sharedCount++;
			if(e.getValue() != 0.0)
				aCount++;
		}
		
		for(Double value : b.values()){
			if(value != 0.0)
				bCount++;
		}
		
		double result;
		if(aCount + bCount == 0.0)
			result = 0.0;
		else
			result = 2 * sharedCount / (aCount + bCount);
		validateResult(result);
		return result;
	}
	
	
	public static double overlapSet(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return overlapSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && bValue != 0.0)
				sharedCount++;
			if(e.getValue() != 0.0)
				aCount++;
		}
		
		for(Double value : b.values()){
			if(value != 0.0)
				bCount++;
		}
		
		double result;
		if(Math.min(aCount,  bCount) == 0.0)
			result = 0.0;
		else
			result = sharedCount / Math.min(aCount,  bCount);
		validateResult(result);
		return result;
	}
	
	public static double cosineSet(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return cosineSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && bValue != 0.0)
				sharedCount++;
			if(e.getValue() != 0.0)
				aCount++;
		}
		
		for(Double value : b.values()){
			if(value != 0.0)
				bCount++;
		}
		
		double result;
		if(aCount == 0.0 || bCount == 0.0)
			result = 0.0;
		else
			result = sharedCount / Math.sqrt(aCount * bCount);
		validateResult(result);
		return result;
	}
	
	
	public static double jaccardGen(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);

		double minSum = 0.0, maxSum = 0.0;
		
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null){
				minSum += Math.min(e.getValue(), bValue);
				maxSum += Math.max(e.getValue(), bValue);
			}
			else
				maxSum += e.getValue();
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()))
				maxSum += e.getValue();
		}
		
		double result;
		if(maxSum == 0.0)
			result = 0.0;
		else
			result = minSum / maxSum;
		validateResult(result);
		return result;
	}
	
	
	public static double diceGen(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);

		double minSum = 0.0, sum = 0.0;
		
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				minSum += Math.min(e.getValue(), bValue);
			sum += e.getValue();
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			sum += e.getValue();
		}

		double result;
		if(sum == 0.0)
			result = 0.0;
		else
			result = 2 * minSum / sum;
		validateResult(result);
		return result;
	}
	
	/**
	 * This is the second implementation of DiceGen from Curran's thesis.
	 * It also describes a jaccardGen implementation which is identical.
	 * @param a
	 * @param b
	 * @return
	 */
	public static double diceGen2(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);

		double uSum = 0.0, sum = 0.0;
		
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				uSum += e.getValue() * bValue;
			sum += e.getValue();
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			sum += e.getValue();
		}

		double result;
		if(sum == 0.0)
			result = 0.0;
		else
			result = uSum / sum;
		validateResult(result);
		return result;
	}
	
	private static ArrayList<Integer> getUnion(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		HashSet<Integer> keys = new HashSet<Integer>();
		
		for(Integer key : a.keySet()){
			keys.add(key);
		}
		
		for(Integer key : b.keySet()){
			keys.add(key);
		}
		
		return new ArrayList<Integer>(keys);
	}
	
	public static double kendallsTau(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		return kendallsTauFast(a, b);
	}
	
	public static double getValue(HashMap<Integer,Double> a, Integer key){
		Double value = a.get(key);
		if(value == null)
			return 0.0;
		return value.doubleValue();
	}
	
	private static double kendallsTauMergeSort(ArrayList<Integer> x, HashMap<Integer,Double> ref, int offset, int length){
		double exchcnt = 0;
		if(length == 0 || length == 1){
			return 0;
		}
		if(length == 2){
			if(getValue(ref, x.get(offset)) <= getValue(ref, x.get(offset+1))){
				return 0;
			}
			else{
				Integer t = x.get(offset);
				x.remove(offset);
				x.add(offset+1, t);
				return 1;
			}
		}
		else{
			int length0 = length / 2;
			int length1 = length - length0;
			int middle = offset + length0;
			exchcnt += kendallsTauMergeSort(x, ref, offset, length0);
			exchcnt += kendallsTauMergeSort(x, ref, middle, length1);
			// Merging
			int j = 0, k = 0;
			ArrayList<Integer> tempList = new ArrayList<Integer>();
			while(j < length0 || k < length1){
				if(k >= length1 || (j < length0 && getValue(ref, x.get(offset+j)) <= getValue(ref, x.get(middle+k)))){
					// pop from left sublist
					tempList.add(x.get(offset+j));
					j++;
				}
				else {
					// pop from right sublist
					tempList.add(x.get(middle + k));
					exchcnt += length0-j;
					k++;
				}
			}
			for(int m = 0; m < length; m++){
				x.remove(offset + m);
				x.add(offset+m, tempList.get(m));
			}
		}
		return exchcnt;
	}
	
	public static double kendallsTauFast(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		class KendallsComparator implements Comparator<Integer>{
			private HashMap<Integer,Double> a;
			private HashMap<Integer,Double> b;
			
			public KendallsComparator(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
				this.a = a;
				this.b = b;
			}
			
			@Override
			public int compare(Integer keyi, Integer keyj) {
				double ai = a.containsKey(keyi)?a.get(keyi):0.0;
	            double aj = a.containsKey(keyj)?a.get(keyj):0.0;
	            if(ai < aj)
	            	return -1;
	            else if(ai > aj)
	            	return 1;
	            else{
		            double bi = b.containsKey(keyi)?b.get(keyi):0.0;
		            double bj = b.containsKey(keyj)?b.get(keyj):0.0;
		            if(bi < bj)
		            	return -1;
		            else if(bi > bj)
		            	return 1;
		            else
		            	return 0;
	            }
			}
		}
		
		validateVectors(a, b);
		
		int i;
		//ArrayList<Integer> keys = getUnion(a, b);
		LinkedHashSet<Integer> keySet = new LinkedHashSet<Integer>(a.keySet());
		keySet.addAll(b.keySet());
		ArrayList<Integer> keys = new ArrayList<Integer>(keySet);
		
		// Sort by values of a and, if tied, by values of b.
		Collections.sort(keys, new KendallsComparator(a, b));
		
		// Compute joint ties.
		// We iterate over the sorted sequence, find the subsequence [i,j] that has the same values, 
		// and use (i-j)*(i-j-1)/2 to calculate the number of (distinct) pairs in that subsequence.
		i = 0;
		double t = 0;
		double aiValue, ajValue, biValue, bjValue;
		for(int j = 1; j < keys.size(); j++){
			aiValue = getValue(a, keys.get(i));
			ajValue = getValue(a, keys.get(j));
			biValue = getValue(b, keys.get(i));
			bjValue = getValue(b, keys.get(j));
			
			if(aiValue == ajValue && biValue == bjValue)
				t += j-i;
			else
				i = j;
		}
		
		// Compute ties in a
		i = 0;
		double u = 0;
		for(int j = 1; j < keys.size(); j++){
			aiValue = getValue(a, keys.get(i));
			ajValue = getValue(a, keys.get(j));
			if(aiValue == ajValue)
				u += j-i;
			else
				i = j;
		}
		
		// Count exchanges
		double exchanges = kendallsTauMergeSort(keys, b, 0, keys.size());
		
		// Compute ties in b (after mergesort has been performed, sorting the elements according to b).
		i = 0;
		double v = 0;
		for(int j = 1; j < keys.size(); j++){
			biValue = getValue(b, keys.get(i));
			bjValue = getValue(b, keys.get(j));
			if(biValue == bjValue)
				v += j-i;
			else
				i = j;
		}

		double n = keys.size();
		double tot = (n * (n-1)) / 2;
		if(tot == u || tot == v){
			return 1.0;
		}
		/*
		System.out.println("n:" + n);
		System.out.println("tot:" + tot);
		System.out.println("v:" + v);
		System.out.println("u:" + u);
		System.out.println("t:" + t);
		System.out.println("exchanges:" + exchanges);
		*/
		
		double tau = ((tot-(v+u-t)) - 2.0 * exchanges) / Math.sqrt((tot-u) * (tot-v)); 

		validateResult(tau);
		return tau;
	}
	
	public static double kendallsTauSlow(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);

		ArrayList<Integer> keys = getUnion(a, b);
		
		Collections.sort(keys);
		
		HashMap<Integer,Double> tiesA = new HashMap<Integer,Double>();
		HashMap<Integer,Double> tiesB = new HashMap<Integer,Double>();
		double concordance = 0.0;
		for(int i = 0; i < keys.size(); i++){
			Integer keyi = keys.get(i);
			for(int j = i+1; j < keys.size(); j++){
				Integer keyj = keys.get(j);
				
				double ai = a.containsKey(keyi)?a.get(keyi):0.0;
                double aj = a.containsKey(keyj)?a.get(keyj):0.0;
                double bi = b.containsKey(keyi)?b.get(keyi):0.0;
                double bj = b.containsKey(keyj)?b.get(keyj):0.0;
                
                double value = (ai - aj)*(bi - bj);
                if(value > 0.0)
                	concordance++;
                else if(value < 0.0)
                	concordance--;
                
                if(ai == aj){
                	if(!tiesA.containsKey(keyi))
                		tiesA.put(keyi, 0.0);
                	tiesA.put(keyi, 1+ tiesA.get(keyi));
                }
                
                if(bi == bj){
                	if(!tiesB.containsKey(keyi))
                		tiesB.put(keyi, 0.0);
                	tiesB.put(keyi, 1+ tiesB.get(keyi));
                }
			}
		}
		/*
		double nA = 0.0;
        for (double t : tiesA.values()) {
        	t = t + 1;
        	nA += t * (t - 1) / 2;
        }

        double nB = 0.0;
        for (double t : tiesB.values()) {
        	t = t + 1;
        	nB += t * (t - 1) / 2;
        }
		*/
		
		double nA = 0.0;
        for (double t : tiesA.values()) {
        	nA += t;
        }

        double nB = 0.0;
        for (double t : tiesB.values()) {
        	nB += t;
        }
		
		double n = (double)keys.size();
		double n0 = n * (n-1) / 2.0;
		
		double result;
		if(concordance == 0.0)
			result = 0.0;
		else
			result = concordance / Math.sqrt((n0 - nA)*(n0 - nB));
		validateResult(result);
		return result;
	}
	

	public static double clarkeDE(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double minSum = 0.0, aSum = 0.0;
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			aSum += e.getValue();
			bValue = b.get(e.getKey());
			if(bValue != null){
				minSum += Math.min(e.getValue(), bValue);
			}
		}
		
		double result;
		if(aSum == 0.0)
			result = 0.0;
		else
			result = minSum / aSum;
		validateResult(result);
		return result;
	}
	
	public static double weedsPrec(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double sumA = 0.0, sumBoth = 0.0;
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			if(e.getValue() > 0.0){
				sumA += e.getValue();
				bValue = b.get(e.getKey());
				if(bValue != null && bValue > 0.0){
					sumBoth += e.getValue();
				}
			}
		}
		
		double result;
		if(sumA == 0.0)
			result = 0.0;
		else
			result = sumBoth / sumA;
		validateResult(result);
		return result;
	}
	
	public static double weedsRec(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double result = weedsPrec(b, a);
		validateResult(result);
		return result;
	}
	
	public static double weedsF(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double prec = weedsPrec(a, b);
		double rec = weedsRec(a, b);
		
		double result;
		if(prec + rec == 0.0)
			result = 0.0;
		else
			result = 2 * prec * rec / (prec + rec);
		validateResult(result);
		return result;
	}
	
	public static double ap(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		LinkedHashMap<Integer,Double> aSorted = Tools.sort(a, true);
		
		int r = 0;
		double sum = 0.0, correctReturned = 0.0;
		for(Integer key : aSorted.keySet()){
			r++;
			
			if(b.containsKey(key)){
				correctReturned++;
				sum += correctReturned / r;
			}
		}
		
		double result;
		if(b.size() == 0)
			result = 0.0;
		else
			result = sum / (double)b.size();
		validateResult(result);
		return result;
	}
	
	public static double apInc(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		LinkedHashMap<Integer,Double> aSorted = Tools.sort(a, true);
		LinkedHashMap<Integer,Double> bSorted = Tools.sort(b, true);
		HashMap<Integer,Integer> bRanks = new HashMap<Integer,Integer>();
		
		int r = 0;
		for(Integer key : bSorted.keySet()){
			r++;
			bRanks.put(key, r);
		}
		
		r = 0;
		double sum = 0.0, correctReturned = 0.0, p, rel;
		for(Integer key : aSorted.keySet()){
			r++;
			
			if(b.containsKey(key)){
				correctReturned++;
				p = correctReturned / r;
				rel = 1.0 - ((double)bRanks.get(key) / ((double)b.size() + 1.0));
				sum += p * rel;
			}
		}
		
		double result;
		if(a.size() == 0)
			result = 0.0;
		else
			result = sum / (double)a.size();
		validateResult(result);
		return result;
	}
	
	public static double balAPInc(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		double lin = lin(a, b);
		double apInc = apInc(a, b);
		double result = Math.sqrt(lin * apInc);
		validateResult(result);
		return result;
	}
	
	public static double linD(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double aSum = 0.0;
		double bSum = 0.0;
		double combinedSum = 0.0;
		
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			if(e.getValue() <= 0.0)
				continue;
			bValue = b.get(e.getKey());
			if(bValue != null && bValue > 0.0){
				combinedSum += e.getValue() + bValue;
				bSum += bValue;
			}
			aSum += e.getValue();
		}
		
		double result;
		if(aSum + bSum == 0.0)
			result = 0.0;
		else
			result = combinedSum / (aSum + bSum);
		validateResult(result);
		return result;
	}
	
	public static double balPrec(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double lin = lin(a, b);
		double weedsPrec = weedsPrec(a, b);
		double result = Math.sqrt(lin * weedsPrec);
		validateResult(result);
		return result;
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
	 * We calculate it over the intersection of nonzero features, as it is undefined otherwise
	 * Non-summetric
	 * @param a
	 * @param b
	 * @return
	 */
	public static double klDivergence(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double divergence = 0;
		Double aValue;
		for (Entry<Integer,Double> e : b.entrySet()) {
			aValue = a.get(e.getKey());
			if (aValue != null && aValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ aValue);
		}

		validateResult(divergence);
		return divergence;
	}

	/*
	public static double klDivergenceR(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double divergence = 0;
		Double bValue;
		for (Entry<Integer,Double> e : a.entrySet()) {
			bValue = b.get(e.getKey());
			if (bValue != null && bValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ bValue);
		}

		validateResult(divergence);
		return divergence;
	}
	*/
	
	public static double klDivergenceR(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		return klDivergence(b, a);
	}
	
	public static double jsDivergence(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double divergence = 0;
		Double aValue, bValue;
		for (Entry<Integer,Double> e : a.entrySet()) {
			bValue = b.get(e.getKey());
			if (bValue != null && bValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((e.getValue() + bValue)/2));
			else if((bValue == null || bValue == 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ (e.getValue()/2));
		}
		
		for (Entry<Integer,Double> e : b.entrySet()) {
			if(e.getValue() <= 0.0)
				continue;
			aValue = a.get(e.getKey());
			if (aValue != null && aValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((e.getValue() + aValue)/2));
			else if((aValue == null || aValue == 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ (e.getValue()/2));
		}

		validateResult(divergence);
		return divergence;
	}
	
	public static double alphaSkew(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		double alpha = 0.99;
		
		double divergence = 0;
		Double aValue;
		for (Entry<Integer,Double> e : b.entrySet()) {
			aValue = a.get(e.getKey());
			if (aValue != null && aValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue() + alpha * aValue));
			else if((aValue == null || aValue > 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue()));
		}

		validateResult(divergence);
		return divergence;
	}
	
	/*
	public static double alphaSkewR(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		double alpha = 0.99;
		
		double divergence = 0;
		Double bValue;
		for (Entry<Integer,Double> e : a.entrySet()) {
			bValue = b.get(e.getKey());
			if (bValue != null && bValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue() + alpha * bValue));
			else if((bValue == null || bValue > 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue()));
		}

		validateResult(divergence);
		return divergence;
	}
	*/
	
	public static double alphaSkewR(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		return alphaSkew(b, a);
	}
	
	public static double manhattan(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double sum = 0.0;
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				sum += Math.abs(e.getValue() - bValue);
			else
				sum += Math.abs(e.getValue());
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()))
				sum += Math.abs(e.getValue());
		}
		
		validateResult(sum);
		return sum;
	}
	
	public static double euclidean(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double sum = 0.0;
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				sum += Math.pow(e.getValue() - bValue, 2);
			else
				sum += Math.pow(e.getValue(), 2);
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()))
				sum += Math.pow(e.getValue(), 2);
		}
		
		double result = Math.sqrt(sum);
		validateResult(result);
		return result;
	}
	
	public static double chebyshev(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double max = 0.0;
		Double bValue;
		for(Entry<Integer,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && Math.abs(e.getValue() - bValue) > max)
				max = Math.abs(e.getValue() - bValue);
			else if(bValue == null && Math.abs(e.getValue()) > max)
				max = Math.abs(e.getValue());
		}
		
		for(Entry<Integer,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()) && Math.abs(e.getValue()) > max)
				max = Math.abs(e.getValue());
		}
		
		validateResult(max);
		return max;
	}
	
	public static double weightedCosine(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double C = 0.5;
		if(Double.isNaN(C))
			throw new RuntimeException("C is NAN");
		if(Double.isInfinite(C))
			throw new RuntimeException("C is inf");

		double aLength = 0.0; 
		double bLength = 0.0;
		double dotProduct = 0.0;

		Double aValue, bValue;

		HashMap<Integer,Double> ranks = Tools._convert_to_ranks(b);
		for(Entry<Integer,Double> entry : Tools.sort(b, true).entrySet()){
			bValue = entry.getValue();
			aValue = a.get(entry.getKey());
			if(aValue != null){
				double rank = ranks.get(entry.getKey());
				double w = (1.0 - (rank / ((double)b.size() + 1.0)))*(1.0 - C) + C;
				dotProduct += (bValue * w) * (aValue * w);
				aLength += Math.pow(aValue * w, 2.0); 
				bLength += Math.pow(bValue * w, 2.0);
			}
			else {
				bLength += Math.pow(bValue * C, 2.0);
			}
		}
		
		for(Entry<Integer,Double> entry : a.entrySet()){
			if(!b.containsKey(entry.getKey()))
				aLength += Math.pow(C * entry.getValue(), 2.0);
		}
		
		double result;
		if(aLength == 0.0 || bLength == 0.0)
			result = 0.0;
		else
			result = dotProduct / Math.sqrt(aLength * bLength);
		
		validateResult(result);
		return result; 
	}
	
	
	/**
	 * This is a slightly more efficient version. When the weights are tied, the rank is taken as the highest possible rank for that value.
	 * @param a
	 * @param b
	 * @return
	 */
	public static double weightedCosine2(HashMap<Integer,Double> a, HashMap<Integer,Double> b){
		validateVectors(a, b);
		
		double C = 0.5;
		if(Double.isNaN(C))
			throw new RuntimeException("C is NAN");
		if(Double.isInfinite(C))
			throw new RuntimeException("C is inf");

		double aLength = 0.0; 
		double bLength = 0.0;
		double dotProduct = 0.0;

		Double aValue, bValue;

		int count = 0;
		double rank = 0.0;
		double previousValue = 0.0;
		for(Entry<Integer,Double> entry : Tools.sort(b, true).entrySet()){
			count++;
			if(count == 1 || !entry.getValue().equals(previousValue)){
				rank = count;
				previousValue = entry.getValue();
			}

			double w = (1.0 - (rank / ((double)b.size() + 1.0)))*(1.0 - C) + C;
			bValue = entry.getValue();
			aValue = a.get(entry.getKey());
			if(aValue != null){
				dotProduct += (bValue * w) * (aValue * w);
				aLength += Math.pow(aValue * w, 2.0); 
				bLength += Math.pow(bValue * w, 2.0);
			}
			else {
				bLength += Math.pow(bValue * C, 2.0);
			}
		}
		
		for(Entry<Integer,Double> entry : a.entrySet()){
			if(!b.containsKey(entry.getKey()))
				aLength += Math.pow(C * entry.getValue(), 2.0);
		}
		
		double result;
		if(aLength == 0.0 || bLength == 0.0)
			result = 0.0;
		else
			result = dotProduct / Math.sqrt(aLength * bLength);
		
		validateResult(result);
		return result; 
	}
	
	
	public double sim(LinkedHashMap<Integer,Double> v1, LinkedHashMap<Integer,Double> v2){
		switch(this){
		case COSINE:
			return SimMeasure.cosine(v1, v2);
		case PEARSON:
			return SimMeasure.pearson(v1, v2);
		case SPEARMAN:
			return SimMeasure.spearman(v1, v2);
		case JACCARD_SET:
			return SimMeasure.jaccardSet(v1, v2);
		case LIN:
			return SimMeasure.lin(v1, v2);
		case DICE_SET:
			return SimMeasure.diceSet(v1, v2);
		case OVERLAP_SET:
			return SimMeasure.overlapSet(v1, v2);
		case COSINE_SET:
			return SimMeasure.cosineSet(v1, v2);
		case JACCARD_GEN:
			return SimMeasure.jaccardGen(v1, v2);
		case DICE_GEN:
			return SimMeasure.diceGen(v1, v2);
		case DICE_GEN_2:
			return SimMeasure.diceGen2(v1, v2);
		case KENDALLS_TAU:
			return SimMeasure.kendallsTau(v1, v2);
		case CLARKE_DE:
			return SimMeasure.clarkeDE(v1, v2);
		case WEEDS_PREC:
			return SimMeasure.weedsPrec(v1, v2);
		case WEEDS_REC:
			return SimMeasure.weedsRec(v1, v2);
		case WEEDS_F:
			return SimMeasure.weedsF(v1, v2);
		case AP:
			return SimMeasure.ap(v1, v2);
		case AP_INC:
			return SimMeasure.apInc(v1, v2);
		case BAL_AP_INC:
			return SimMeasure.balAPInc(v1, v2);
		case LIN_D:
			return SimMeasure.linD(v1, v2);
		case BAL_PREC:
			return SimMeasure.balPrec(v1, v2);
		case KL_DIVERGENCE:
			return SimMeasure.klDivergence(v1, v2);
		case KL_DIVERGENCE_R:
			return SimMeasure.klDivergenceR(v1, v2);
		case JS_DIVERGENCE:
			return SimMeasure.jsDivergence(v1, v2);
		case ALPHA_SKEW:
			return SimMeasure.alphaSkew(v1, v2);
		case ALPHA_SKEW_R:
			return SimMeasure.alphaSkewR(v1, v2);
		case MANHATTAN:
			return SimMeasure.manhattan(v1, v2);
		case EUCLIDEAN:
			return SimMeasure.euclidean(v1, v2);
		case CHEBYSHEV:
			return SimMeasure.chebyshev(v1, v2);
		case WEIGHTED_COSINE:
			return SimMeasure.weightedCosine(v1, v2);
		case WEIGHTED_COSINE_2:
			return SimMeasure.weightedCosine2(v1, v2);
		default:
			throw new RuntimeException("Unknown similarity measure: " + this);
		}
	}
}

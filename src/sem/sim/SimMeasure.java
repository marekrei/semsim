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
	
	
	
	public static <T> void validateVectors(HashMap<T,Double> a, HashMap<T,Double> b){
		if(a == null || b == null)
			throw new IllegalArgumentException("Vectors cannot be null");
	}
	
	public static void validateResult(double result){
		if(Double.isInfinite(result))
			throw new RuntimeException("Similarity is infinite");
		if(Double.isNaN(result))
			throw new RuntimeException("Similarity is NaN");
	}
	
	public static <T> HashMap<T,Double> fillVector(HashMap<T,Double> mainVector, HashMap<T,Double> referenceVector){
		HashMap<T,Double> newVector = new HashMap<T,Double>(mainVector);
		for(T key : referenceVector.keySet())
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
	public static <T> double cosine(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return cosine(b, a);
		
		double aLength = 0.0;
		double bLength = 0.0;
		double dotProduct = 0.0;

		Double tempDouble;
		
		for(Entry<T,Double> entry : a.entrySet()){
			tempDouble = b.get(entry.getKey());
			if(tempDouble != null)
				dotProduct += entry.getValue() * tempDouble;
			aLength += entry.getValue() * entry.getValue();
		}
		
		for(Entry<T,Double> entry : b.entrySet())
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
	public static <T> double pearson(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		HashMap<T,Double> a2 = fillVector(a, b);
		HashMap<T,Double> b2 = fillVector(b, a);
		
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
	public static <T> double spearman(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		HashMap<T,Double> a2 = fillVector(a, b);
		HashMap<T,Double> b2 = fillVector(b, a);
		
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
	public static <T> double jaccardSet(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return jaccardSet(b, a);
		
		double intersectionSize = 0;
		HashSet<T> union = new HashSet<T>();
		Double bValue;
		
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && bValue != 0.0)
				intersectionSize++;
			if(e.getValue() != null && e.getValue() != 0.0)
				union.add(e.getKey());
		}
		
		for(Entry<T,Double> e : b.entrySet()){
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
	public static <T> double lin(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return lin(b, a);
		
		double aSum = 0.0;
		double bSum = 0.0;
		double combinedSum = 0.0;
		
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			if(e.getValue() <= 0.0)
				continue;
			bValue = b.get(e.getKey());
			if(bValue != null && bValue > 0.0)
				combinedSum += e.getValue() + bValue;
			aSum += e.getValue();
		}
		
		for(Entry<T,Double> e : b.entrySet())
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
	
	
	public static <T> double diceSet(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return diceSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<T,Double> e : a.entrySet()){
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
	
	
	public static <T> double overlapSet(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return overlapSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<T,Double> e : a.entrySet()){
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
	
	public static <T> double cosineSet(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		if(a.size() > b.size())
			return cosineSet(b, a);
		
		double sharedCount = 0.0, aCount = 0.0, bCount = 0.0;
		Double bValue;
		
		for(Entry<T,Double> e : a.entrySet()){
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
	
	
	public static <T> double jaccardGen(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);

		double minSum = 0.0, maxSum = 0.0;
		
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null){
				minSum += Math.min(e.getValue(), bValue);
				maxSum += Math.max(e.getValue(), bValue);
			}
			else
				maxSum += e.getValue();
		}
		
		for(Entry<T,Double> e : b.entrySet()){
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
	
	
	public static <T> double diceGen(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);

		double minSum = 0.0, sum = 0.0;
		
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				minSum += Math.min(e.getValue(), bValue);
			sum += e.getValue();
		}
		
		for(Entry<T,Double> e : b.entrySet()){
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
	public static <T> double diceGen2(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);

		double uSum = 0.0, sum = 0.0;
		
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				uSum += e.getValue() * bValue;
			sum += e.getValue();
		}
		
		for(Entry<T,Double> e : b.entrySet()){
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
	
	private static <T> ArrayList<T> getUnion(HashMap<T,Double> a, HashMap<T,Double> b){
		HashSet<T> keys = new HashSet<T>();
		
		for(T key : a.keySet()){
			keys.add(key);
		}
		
		for(T key : b.keySet()){
			keys.add(key);
		}
		
		return new ArrayList<T>(keys);
	}
	
	public static <T> double kendallsTau(HashMap<T,Double> a, HashMap<T,Double> b){
		return kendallsTauFast(a, b);
	}
	
	public static <T> double getValue(HashMap<T,Double> a, T key){
		Double value = a.get(key);
		if(value == null)
			return 0.0;
		return value.doubleValue();
	}
	
	private static <T> double kendallsTauMergeSort(ArrayList<T> x, HashMap<T,Double> ref, int offset, int length){
		double exchcnt = 0;
		if(length == 0 || length == 1){
			return 0;
		}
		if(length == 2){
			if(getValue(ref, x.get(offset)) <= getValue(ref, x.get(offset+1))){
				return 0;
			}
			else{
				T t = x.get(offset);
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
			ArrayList<T> tempList = new ArrayList<T>();
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
	
	public static <T> double kendallsTauFast(HashMap<T,Double> a, HashMap<T,Double> b){
		class KendallsComparator implements Comparator<T>{
			private HashMap<T,Double> a;
			private HashMap<T,Double> b;
			
			public KendallsComparator(HashMap<T,Double> a, HashMap<T,Double> b){
				this.a = a;
				this.b = b;
			}
			
			@Override
			public int compare(T keyi, T keyj) {
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
		LinkedHashSet<T> keySet = new LinkedHashSet<T>(a.keySet());
		keySet.addAll(b.keySet());
		ArrayList<T> keys = new ArrayList<T>(keySet);
		
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
	
	public static <T extends Comparable<T>> double kendallsTauSlow(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);

		ArrayList<T> keys = getUnion(a, b);
		
		Collections.sort(keys);
		
		HashMap<T,Double> tiesA = new HashMap<T,Double>();
		HashMap<T,Double> tiesB = new HashMap<T,Double>();
		double concordance = 0.0;
		T keyi, keyj;
		for(int i = 0; i < keys.size(); i++){
			keyi = keys.get(i);
			for(int j = i+1; j < keys.size(); j++){
				keyj = keys.get(j);
				
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
	

	public static <T> double clarkeDE(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double minSum = 0.0, aSum = 0.0;
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
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
	
	public static <T> double weedsPrec(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double sumA = 0.0, sumBoth = 0.0;
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
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
	
	public static <T> double weedsRec(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double result = weedsPrec(b, a);
		validateResult(result);
		return result;
	}
	
	public static <T> double weedsF(HashMap<T,Double> a, HashMap<T,Double> b){
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
	
	public static <T> double ap(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		LinkedHashMap<T,Double> aSorted = Tools.sort(a, true);
		
		int r = 0;
		double sum = 0.0, correctReturned = 0.0;
		for(T key : aSorted.keySet()){
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
	
	public static <T> double apInc(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		LinkedHashMap<T,Double> aSorted = Tools.sort(a, true);
		LinkedHashMap<T,Double> bSorted = Tools.sort(b, true);
		HashMap<T,Integer> bRanks = new HashMap<T,Integer>();
		
		int r = 0;
		for(T key : bSorted.keySet()){
			r++;
			bRanks.put(key, r);
		}
		
		r = 0;
		double sum = 0.0, correctReturned = 0.0, p, rel;
		for(T key : aSorted.keySet()){
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
	
	public static <T> double balAPInc(HashMap<T,Double> a, HashMap<T,Double> b){
		double lin = lin(a, b);
		double apInc = apInc(a, b);
		double result = Math.sqrt(lin * apInc);
		validateResult(result);
		return result;
	}
	
	public static <T> double linD(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double aSum = 0.0;
		double bSum = 0.0;
		double combinedSum = 0.0;
		
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
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
	
	public static <T> double balPrec(HashMap<T,Double> a, HashMap<T,Double> b){
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
	public static <T> double klDivergence(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double divergence = 0;
		Double aValue;
		for (Entry<T,Double> e : b.entrySet()) {
			aValue = a.get(e.getKey());
			if (aValue != null && aValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ aValue);
		}

		validateResult(divergence);
		return divergence;
	}
	
	public static <T> double klDivergenceR(HashMap<T,Double> a, HashMap<T,Double> b){
		return klDivergence(b, a);
	}
	
	public static <T> double jsDivergence(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double divergence = 0;
		Double aValue, bValue;
		for (Entry<T,Double> e : a.entrySet()) {
			bValue = b.get(e.getKey());
			if (bValue != null && bValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((e.getValue() + bValue)/2));
			else if((bValue == null || bValue == 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ (e.getValue()/2));
		}
		
		for (Entry<T,Double> e : b.entrySet()) {
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
	
	public static <T> double alphaSkew(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		double alpha = 0.99;
		
		double divergence = 0;
		Double aValue;
		for (Entry<T,Double> e : b.entrySet()) {
			aValue = a.get(e.getKey());
			if (aValue != null && aValue > 0.0 && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue() + alpha * aValue));
			else if((aValue == null || aValue > 0.0) && e.getValue() > 0.0)
				divergence += e.getValue() * Math.log(e.getValue()/ ((1-alpha) * e.getValue()));
		}

		validateResult(divergence);
		return divergence;
	}
	
	public static <T> double alphaSkewR(HashMap<T,Double> a, HashMap<T,Double> b){
		return alphaSkew(b, a);
	}
	
	public static <T> double manhattan(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double sum = 0.0;
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				sum += Math.abs(e.getValue() - bValue);
			else
				sum += Math.abs(e.getValue());
		}
		
		for(Entry<T,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()))
				sum += Math.abs(e.getValue());
		}
		
		validateResult(sum);
		return sum;
	}
	
	public static <T> double euclidean(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double sum = 0.0;
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null)
				sum += Math.pow(e.getValue() - bValue, 2);
			else
				sum += Math.pow(e.getValue(), 2);
		}
		
		for(Entry<T,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()))
				sum += Math.pow(e.getValue(), 2);
		}
		
		double result = Math.sqrt(sum);
		validateResult(result);
		return result;
	}
	
	public static <T> double chebyshev(HashMap<T,Double> a, HashMap<T,Double> b){
		validateVectors(a, b);
		
		double max = 0.0;
		Double bValue;
		for(Entry<T,Double> e : a.entrySet()){
			bValue = b.get(e.getKey());
			if(bValue != null && Math.abs(e.getValue() - bValue) > max)
				max = Math.abs(e.getValue() - bValue);
			else if(bValue == null && Math.abs(e.getValue()) > max)
				max = Math.abs(e.getValue());
		}
		
		for(Entry<T,Double> e : b.entrySet()){
			if(!a.containsKey(e.getKey()) && Math.abs(e.getValue()) > max)
				max = Math.abs(e.getValue());
		}
		
		validateResult(max);
		return max;
	}
	
	public static <T> double weightedCosine(HashMap<T,Double> a, HashMap<T,Double> b){
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

		HashMap<T,Double> ranks = Tools._convert_to_ranks(b);
		for(Entry<T,Double> entry : Tools.sort(b, true).entrySet()){
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
		
		for(Entry<T,Double> entry : a.entrySet()){
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
	public static <T> double weightedCosine2(HashMap<T,Double> a, HashMap<T,Double> b){
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
		for(Entry<T,Double> entry : Tools.sort(b, true).entrySet()){
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
		
		for(Entry<T,Double> entry : a.entrySet()){
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
	
	
	public <T> double sim(LinkedHashMap<T,Double> v1, LinkedHashMap<T,Double> v2){
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

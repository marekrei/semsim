package sem.test.sim;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sem.sim.SimMeasure;

public class SimilarityTest {
	
	private LinkedHashMap<Integer,Double> vector1;
	private LinkedHashMap<Integer,Double> vector2;
	private LinkedHashMap<Integer,Double> vector3;
	private static double smallValue = 0.00000001;
	
	@Before
	public void setUp() throws Exception {
		vector1 = new LinkedHashMap<Integer,Double>();
		vector2 = new LinkedHashMap<Integer,Double>();
		vector3 = new LinkedHashMap<Integer,Double>();
		
		vector1.put(2, 2.0);
		vector1.put(4, 5.0);
		vector1.put(7, 1.0);
		vector1.put(10, 4.0);
		
		vector2.put(3, 2.0);
		vector2.put(4, 3.0);
		vector2.put(7, 2.0);
		
		vector3.put(2, 2.0);
		vector3.put(4, 5.0);
		vector3.put(7, 2.0);
		vector3.put(10, 5.0);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCosine() {
		double cosineValue = 0.607918759;
		assertTrue(Math.abs(SimMeasure.cosine(vector1, vector2) - cosineValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.COSINE.sim(vector1, vector2) - cosineValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.cosine(vector2, vector1) - cosineValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.cosine(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.cosine(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void testPearson(){
		double pearsonValue = 0.017972129;
		assertTrue(Math.abs(SimMeasure.pearson(vector1, vector2) - pearsonValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.PEARSON.sim(vector1, vector2) - pearsonValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.pearson(vector2, vector1) - pearsonValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.pearson(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.pearson(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void testSpearman(){
		double spearmanValue = 0.10540925533894598;
		assertTrue(Math.abs(SimMeasure.spearman(vector1, vector2) - spearmanValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.SPEARMAN.sim(vector1, vector2) - spearmanValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.spearman(vector2, vector1) - spearmanValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.spearman(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.spearman(vector2, vector2) - 1.0) < smallValue);
	}

	
	@Test
	public void testJaccardSet(){
		double jaccardValue = 2.0 / 5.0;
		assertTrue(Math.abs(SimMeasure.jaccardSet(vector1, vector2) - jaccardValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.JACCARD_SET.sim(vector1, vector2) - jaccardValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jaccardSet(vector2, vector1) - jaccardValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jaccardSet(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.jaccardSet(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void testLin(){
		double linValue = 11.0/19.0;
		assertTrue(Math.abs(SimMeasure.lin(vector1, vector2) - linValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.LIN.sim(vector1, vector2) - linValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.lin(vector2, vector1) - linValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.lin(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.lin(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void diceSetTest(){
		double diceSetValue = 4.0/7.0;
		assertTrue(Math.abs(SimMeasure.diceSet(vector1, vector2) - diceSetValue) < smallValue);
		assertTrue(Math.abs(SimMeasure.DICE_SET.sim(vector1, vector2) - diceSetValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.diceSet(vector2, vector1) - diceSetValue) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.diceSet(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.diceSet(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void overlapSetTest(){
		double value = 2.0/3.0;
		assertTrue(Math.abs(SimMeasure.overlapSet(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.OVERLAP_SET.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.overlapSet(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.overlapSet(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.overlapSet(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void cosineSetTest(){
		double value = 2.0/Math.sqrt(4*3);
		assertTrue(Math.abs(SimMeasure.cosineSet(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.COSINE_SET.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.cosineSet(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.cosineSet(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.cosineSet(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void jaccardGenTest(){
		double value = 4.0/15.0;
		assertTrue(Math.abs(SimMeasure.jaccardGen(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.JACCARD_GEN.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jaccardGen(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jaccardGen(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.jaccardGen(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void diceGenTest(){
		double value = 8.0/19.0;
		assertTrue(Math.abs(SimMeasure.diceGen(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.DICE_GEN.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.diceGen(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.diceGen(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.diceGen(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void diceGen2Test(){
		double value = 17.0/19.0;
		assertTrue(Math.abs(SimMeasure.diceGen2(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.DICE_GEN_2.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.diceGen2(vector2, vector1) - value) < smallValue);
	}
	
	private HashMap<Integer,Double> createRandVector(int length){
		HashMap<Integer,Double> v = new HashMap<Integer,Double>();
		Random random = new Random();
		for(int i = 0; i < length; i++){
			double p = random.nextDouble();
			if(p < 0.3){
				v.put(i, random.nextDouble());
			}
			else if(p < 0.6)
				v.put(i, (double)random.nextInt(100));
			else if(p < 0.8 && v.size() > 0){
				ArrayList<Integer> keys = new ArrayList<Integer>(v.keySet());
				v.put(i, v.get(keys.get(random.nextInt(keys.size()))));
			}
		}
		return v;
	}

	@Test
	public void kendallsTauTest(){
		double value1 = 0.0;
		double value2 = 0.816496551036835;
		double smallValue = 0.0000001;


		assertTrue(Math.abs(SimMeasure.kendallsTau(vector1, vector3) - value2) < smallValue);
		assertTrue(Math.abs(SimMeasure.KENDALLS_TAU.sim(vector1, vector3) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.kendallsTau(vector3, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.kendallsTau(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.kendallsTau(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.kendallsTau(vector2, vector2) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.kendallsTau(vector3, vector3) - 1.0) < smallValue);
		
		
		for(int experiment = 0; experiment < 100; experiment++){
			HashMap<Integer,Double> v1 = createRandVector(100);
			HashMap<Integer,Double> v2 = createRandVector(100);
			double tau1 = SimMeasure.kendallsTauSlow(v1, v2);
			double tau2 = SimMeasure.kendallsTauFast(v1, v2);
			assertTrue(Math.abs(tau1-tau2) < smallValue);
		}
	}
	
	@Test
	public void clarkeDeTest(){
		double value1 = 4.0 / 12.0;
		double value2 = 4.0 / 7.0;
		assertTrue(Math.abs(SimMeasure.clarkeDE(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.CLARKE_DE.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.clarkeDE(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.clarkeDE(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.clarkeDE(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void weedsPrecTest(){
		double value1 = 6.0/12.0;
		double value2 = 5.0/7.0;
		assertTrue(Math.abs(SimMeasure.weedsPrec(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.WEEDS_PREC.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsPrec(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsPrec(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.weedsPrec(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void weedsRecTest(){
		double value1 = 5.0/7.0;
		double value2 = 6.0/12.0;
		assertTrue(Math.abs(SimMeasure.weedsRec(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.WEEDS_REC.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsRec(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsRec(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.weedsRec(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void weedsFTest(){
		double value = 0.588235294;
		assertTrue(Math.abs(SimMeasure.weedsF(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.WEEDS_F.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsF(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weedsF(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.weedsF(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void apTest(){
		double value1 = 0.5;
		double value2 = 0.416666667;
		assertTrue(Math.abs(SimMeasure.ap(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.AP.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.ap(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.ap(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.ap(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void apIncTest(){
		double value1 = 0.21875;
		double value2 = 0.311111111;
		assertTrue(Math.abs(SimMeasure.apInc(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.AP_INC.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.apInc(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.apInc(vector1, vector1) - 0.5) < smallValue);
		assertTrue(Math.abs(SimMeasure.apInc(vector2, vector2) - 0.5) < smallValue);
	}
	
	@Test
	public void balAPIncTest(){
		double value1 = Math.sqrt(0.21875 * (11.0/19.0));
		double value2 = Math.sqrt(0.311111111 * (11.0/19.0));
		assertTrue(Math.abs(SimMeasure.balAPInc(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.BAL_AP_INC.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.balAPInc(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.balAPInc(vector1, vector1) - Math.sqrt(0.5)) < smallValue);
		assertTrue(Math.abs(SimMeasure.balAPInc(vector2, vector2) - Math.sqrt(0.5)) < smallValue);
	}
	
	@Test
	public void testKlDivergence() {
		double value1 = -0.14618251;
		double value2 = 1.860980938;
		assertTrue(Math.abs(SimMeasure.klDivergence(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.KL_DIVERGENCE.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.klDivergence(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.klDivergence(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.klDivergence(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testKlDivergenceR() {
		double value1 = 1.860980938;
		double value2 = -0.14618251;
		assertTrue(Math.abs(SimMeasure.klDivergenceR(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.KL_DIVERGENCE_R.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.klDivergenceR(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.klDivergenceR(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.klDivergenceR(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testJsDivergence() {
		double value = 5.96774802;
		assertTrue(Math.abs(SimMeasure.jsDivergence(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.JS_DIVERGENCE.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jsDivergence(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.jsDivergence(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.jsDivergence(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testAlphaSkew() {
		double value1 = 9.056281264;
		double value2 = 29.463791882;
		assertTrue(Math.abs(SimMeasure.alphaSkew(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.ALPHA_SKEW.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.alphaSkew(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.alphaSkew(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.alphaSkew(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testAlphaSkewR() {
		double value1 = 29.463791882;
		double value2 = 9.056281264;
		assertTrue(Math.abs(SimMeasure.alphaSkewR(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.ALPHA_SKEW_R.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.alphaSkewR(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.alphaSkewR(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.alphaSkewR(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testManhattan(){
		double value = 2 + 2 + 2 + 1 + 4;
		
		assertTrue(Math.abs(SimMeasure.manhattan(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.MANHATTAN.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.manhattan(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.manhattan(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.manhattan(vector2, vector2)) < smallValue);
	}
	
	
	@Test
	public void testEuclidean(){
		double value = Math.sqrt(4 + 4 + 4 + 1 + 16);
		
		assertTrue(Math.abs(SimMeasure.euclidean(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.EUCLIDEAN.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.euclidean(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.euclidean(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.euclidean(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testChebyshev(){
		double value = 4;
		assertTrue(Math.abs(SimMeasure.chebyshev(vector1, vector2) - value) < smallValue);
		assertTrue(Math.abs(SimMeasure.CHEBYSHEV.sim(vector1, vector2) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.chebyshev(vector2, vector1) - value) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.chebyshev(vector1, vector1)) < smallValue);
		assertTrue(Math.abs(SimMeasure.chebyshev(vector2, vector2)) < smallValue);
	}
	
	@Test
	public void testWeightedCosine() {
		double value1 = 0.801084397;
		double value2 = 0.815299803;
		assertTrue(Math.abs(SimMeasure.weightedCosine(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.WEIGHTED_COSINE.sim(vector1, vector2) - value1) < smallValue);

		assertTrue(Math.abs(SimMeasure.weightedCosine(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weightedCosine(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.weightedCosine(vector2, vector2) - 1.0) < smallValue);
	}
	
	@Test
	public void testWeightedCosine2() {
		double value1 = 0.796682452;
		double value2 = 0.815299803;
		assertTrue(Math.abs(SimMeasure.weightedCosine2(vector1, vector2) - value1) < smallValue);
		assertTrue(Math.abs(SimMeasure.WEIGHTED_COSINE_2.sim(vector1, vector2) - value1) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weightedCosine2(vector2, vector1) - value2) < smallValue);
		
		assertTrue(Math.abs(SimMeasure.weightedCosine2(vector1, vector1) - 1.0) < smallValue);
		assertTrue(Math.abs(SimMeasure.weightedCosine2(vector2, vector2) - 1.0) < smallValue);
	}
}

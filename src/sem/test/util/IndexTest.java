package sem.test.util;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sem.util.Index;

public class IndexTest {
	
	private String dir = "semtests/";
	private String file = dir + "test-dictionary.txt";

	@Before
	public void setUp() throws Exception {
		File d = new File(dir);
		if(!d.exists())
			d.mkdir();
	}
	
	@After
	public void tearDown() throws Exception {
		(new File(file)).delete();
		(new File(dir)).delete();
	}

	@Test
	public void testDictionaryString() {
		Index index = new Index();

		index.add("str1", 3.0);
		index.add("str1", 8.0);
		index.add("str2", 5.0);
		index.save(file);
		index = new Index(file);
		assertTrue(index.getCount("str1") == 11.0);
		assertTrue(index.getCount("str2") == 5.0);
	}

	@Test
	public void testAdd() {
		Index index = new Index();
		assertTrue(index.getCount("str1") == 0.0);
		
		index.add("str1", 3.0);
		assertTrue(index.getCount("str1") == 3.0);
		
		index.add("str2", 5.0);
		assertTrue(index.getCount("str2") == 5.0);
		
		index.add("str1", 8.0);
		assertTrue(index.getCount("str1") == 11.0);
		
		index.add("str1");
		assertTrue(index.getCount("str1") == 12.0);
	}

	@Test
	public void testGetId() {
		Index index = new Index();
		assertTrue(index.getId("str1") == null);
		
		index.add("str1", 3.0);
		assertTrue(index.getId("str1") == 1);
		
		index.add("str2", 5.0);
		assertTrue(index.getId("str2") == 2);
		
		index.add("str3", 1.0);
		assertTrue(index.getId("str3") == 3);
		
		index.add("str1", 8.0);
		assertTrue(index.getId("str1") == 1);
		
		assertTrue(index.getId("str4") == null);
	}

	@Test
	public void testGetCountInteger() {
		Index index = new Index();
		assertTrue(index.getCount(5) == 0.0);
		
		index.add("str1", 3.0);
		assertTrue(index.getCount(1) == 3.0);
		
		index.add("str2", 5.0);
		assertTrue(index.getCount(2) == 5.0);
		
		index.add("str1", 8.0);
		assertTrue(index.getCount(1) == 11.0);
		
		assertTrue(index.getCount(3) == 0.0);
	}

	@Test
	public void testSize() {
		Index index = new Index();
		assertTrue(index.size() == 0);
		
		index.add("str1", 3.0);
		assertTrue(index.size() == 1);
		
		index.add("str2", 5.0);
		assertTrue(index.size() == 2);
		
		index.add("str1", 8.0);
		assertTrue(index.size() == 2);
	}

	@Test
	public void testClear() {
		Index index = new Index();
		index.add("str1", 3.0);
		index.add("str2", 5.0);
		index.add("str1", 8.0);
		
		assertTrue(index.size()  == 2);
		index.clear();
		assertTrue(index.size() == 0);
	}

	@Test
	public void testGetIdMap() {
		Index index = new Index();
		index.add("str1", 3.0);
		index.add("str2", 5.0);
		index.add("str1", 8.0);
		index.add("str3", 10.0);
		
		HashMap<String,Integer> idMap = index.getIdMap();
		assertTrue(idMap.size() == 3);
		assertTrue(idMap.get("str1") == 1);
		assertTrue(idMap.get("str2") == 2);
		assertTrue(idMap.get("str3") == 3);
	}

	@Test
	public void testGetCountMap() {
		Index index = new Index();
		index.add("str1", 3.0);
		index.add("str2", 5.0);
		index.add("str1", 8.0);
		index.add("str3", 10.0);
		
		HashMap<Integer,Double> countMap = index.getCountMap();
		assertTrue(countMap.size() == 3);
		assertTrue(countMap.get(1) == 11.0);
		assertTrue(countMap.get(2) == 5.0);
		assertTrue(countMap.get(3) == 10.0);
	}

	@Test
	public void testGetStringMap() {
		Index index = new Index();
		index.add("str1", 3.0);
		index.add("str2", 5.0);
		index.add("str1", 8.0);
		index.add("str3", 10.0);
		
		HashMap<Integer,String> labelMap = index.getLabelMap();
		assertTrue(labelMap.size() == 3);
		assertTrue(labelMap.get(1).equals("str1"));
		assertTrue(labelMap.get(2).equals("str2"));
		assertTrue(labelMap.get(3).equals("str3"));
	}

	@Test
	public void testSave() {
		Index index = new Index();
		index.add("str1", 3.0);
		index.add("str2", 5.0);
		index.add("str1", 8.0);
		index.add("str3", 10.0);
		
		index.save(file);
		
		Index index2 = new Index(file);
		assertTrue(index2.getCount("str1") == 11.0);
		assertTrue(index2.getCount("str2") == 5.0);
		assertTrue(index2.getCount("str3") == 10.0);
	}

}

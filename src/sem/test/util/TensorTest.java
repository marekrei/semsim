package sem.test.util;

import java.io.File;

import org.junit.*;

import static org.junit.Assert.*;

import sem.util.Tensor;

public class TensorTest {
	
	private String dir = "semtests/";
	private String file = dir + "test-tensor.txt";
	
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
	public void testAdd() {
		Tensor tensor = new Tensor();
		assertTrue(tensor.get(6, 21, 2) == 0.0);
		
		tensor.add(6, 21, 2, 4.0);
		assertTrue(tensor.get(6, 21, 2) == 4.0);
		
		tensor.add(1, 1, 1, 2.0);
		assertTrue(tensor.get(1, 1, 1) == 2.0);
		
		tensor.add(6, 21, 2, 15.0);
		assertTrue(tensor.get(6, 21, 2) == 19.0);
		
		assertTrue(tensor.get(4, 21, 2) == 0.0);
		assertTrue(tensor.get(6, 20, 2) == 0.0);
		assertTrue(tensor.get(6, 21, 0) == 0.0);
	}
	
	@Test
	public void testSet() {
		Tensor tensor = new Tensor();
		assertTrue(tensor.get(6, 21, 2) == 0.0);
		
		tensor.set(6, 21, 2, 4.0);
		assertTrue(tensor.get(6, 21, 2) == 4.0);
		
		tensor.set(1, 1, 1, 2.0);
		assertTrue(tensor.get(1, 1, 1) == 2.0);
		
		tensor.set(6, 21, 2, 15.0);
		assertTrue(tensor.get(6, 21, 2) == 15.0);
		
		assertTrue(tensor.get(4, 21, 2) == 0.0);
		assertTrue(tensor.get(6, 20, 2) == 0.0);
		assertTrue(tensor.get(6, 21, 0) == 0.0);
	}
	
	@Test
	public void testSave(){
		Tensor tensor = new Tensor();
		tensor.add(1, 3, 6, 15.0);
		tensor.add(3, 1, 6, 12.0);
		tensor.add(6, 3, 1, 17.0);
		tensor.add(6, 3, 1, 2.0);
		tensor.save(file);
		tensor = new Tensor(file);
		assertTrue(tensor.get(1, 3, 6) == 15.0);
		assertTrue(tensor.get(3, 1, 6) == 12.0);
		assertTrue(tensor.get(6, 3, 1) == 19.0);
	}

	@Test
	public void testWildcard(){
		Tensor tensor = new Tensor();
		tensor.add(3, 2, 1, 5.0);
		
		tensor.add(1, 2, 1, 2.0);
		tensor.add(19, 2, 1, 10.0);
		
		
		tensor.add(3, 15, 1, 3.0);
		tensor.add(3, 0, 1, 25.0);
		
		
		tensor.add(3, 2, 3, 7.0);
		tensor.add(3, 2, 6, 2.0);
		
		assertTrue(tensor.get(null, 2, 1) == 17.0);
		assertTrue(tensor.get(3, null, 1) == 33.0);
		assertTrue(tensor.get(3, 2, null) == 14.0);
		assertTrue(tensor.get(3, null, null) == 42.0);
		assertTrue(tensor.get(null, 2, null) == 26.0);
		assertTrue(tensor.get(null, null, 1) == 45.0);
		assertTrue(tensor.get(null, null, null) == 54.0);
	}
	
	
}

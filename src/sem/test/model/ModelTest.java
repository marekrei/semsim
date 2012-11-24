package sem.test.model;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sem.exception.SemModelException;
import sem.graph.Graph;
import sem.graph.Node;
import sem.model.SemModel;
import sem.test.util.ToolsTest;

public class ModelTest {
	
	private String dir = "semtests/";
	private String file = dir + "model";

	@Before
	public void setUp() throws Exception {
		ToolsTest.initTestDir(dir);
	}

	@After
	public void tearDown() throws Exception {
		ToolsTest.removeTestDir(dir);
	}
	
	public Graph createGraph(){
		Graph graph = new Graph();
		Node n1 = graph.addNode("A", "A");
		Node n2 = graph.addNode("B", "B");
		Node n3 = graph.addNode("C", "C");
		Node n4 = graph.addNode("D", "D");
		Node n5 = graph.addNode("C", "C");
		Node n6 = graph.addNode("A", "A");
		graph.addEdge("1", n2, n1);
		graph.addEdge("2", n2, n4);
		graph.addEdge("3", n4, n3);
		graph.addEdge("3", n4, n5);
		graph.addEdge("2", n5, n6);
		graph.addEdge("4", n6, n2);
		graph.addEdge("3", n3, n1);
		graph.addEdge("1", n3, n1);
		return graph;
	}
	
	public void testBasic(SemModel vsm){
		assertTrue(vsm.getTotalNodeCount() == 6);
		
		assertTrue(vsm.getEdgeCount("1") == 2);
		assertTrue(vsm.getEdgeCount("2") == 2);
		assertTrue(vsm.getEdgeCount("4") == 1);
		assertTrue(vsm.getEdgeCount("9") == 0);
		
		assertTrue(vsm.getNodeCount("A_A") == 2);
		assertTrue(vsm.getNodeCount("B_B") == 1);
		assertTrue(vsm.getNodeCount("H_H") == 0);
		
		assertTrue(vsm.getTripleCount("B_B", "2", "D_D") == 1);
		assertTrue(vsm.getTripleCount("D_D", "3", "C_C") == 2);
		assertTrue(vsm.getTripleCount("B_B", "2", "C_C") == 0);
		assertTrue(vsm.getTripleCount("A_A", "1", "X_X") == 0);
		assertTrue(vsm.getTripleCount("C_C", "3", "A_A") == 1);
	}
	
	@Test
	public void testSimple() {
		SemModel vsm = new SemModel(true);
		Graph graph = createGraph();
		try {
			vsm.add(graph);
		} catch (SemModelException e) {
			e.printStackTrace();
		}
		
		testBasic(vsm);
		
		assertTrue(vsm.getTotalEdgeCount() == 8);
		
		assertTrue(vsm.getTripleCount(null, "1", "A_A") == 2);		
		assertTrue(vsm.getTripleCount("C_C", null, "A_A") == 3);
		assertTrue(vsm.getTripleCount("D_D", "3", null) == 2);
		
		assertTrue(vsm.getTripleCount("C_C", null, null) == 3);
		assertTrue(vsm.getTripleCount(null, "2", null) == 2);
		assertTrue(vsm.getTripleCount(null, null, "A_A") == 4);
		
		assertTrue(vsm.getTripleCount(null, null, null) == 8);
		
	}
	/*
	@Test
	public void testNull() {
		VSM vsm = new VSM();
		Graph graph = createGraph();
		try {
			vsm.add(graph, false, true);
		} catch (VSMException e) {
			e.printStackTrace();
		}
		
		testBasic(vsm);
		
		assertTrue(vsm.getTotalEdgeCount() == 31);
		
		assertTrue(vsm.getEdgeCount(VSM.nullEdgeLabel) == 23);
		
		assertTrue(vsm.getTripleCount("C_C", VSM.nullEdgeLabel, "A_A") == 2);
		
		assertTrue(vsm.getTripleCount(null, "1", "A_A") == 2);		
		assertTrue(vsm.getTripleCount("C_C", null, "A_A") == 5);
		
		assertTrue(vsm.getTripleCount("C_C", null, null) == 11);
		assertTrue(vsm.getTripleCount("D_D", null, null) == 5);
		assertTrue(vsm.getTripleCount(null, VSM.nullEdgeLabel, null) == 23);
		assertTrue(vsm.getTripleCount(null, null, "A_A") == 11);
		
		assertTrue(vsm.getTripleCount(null, null, null) == 31);
		
	}
	*/
	@Test
	public void testSave(){
		SemModel vsm = new SemModel(true);
		Graph graph = createGraph();
		try {
			vsm.add(graph);
		} catch (SemModelException e) {
			e.printStackTrace();
		}
		
		vsm.save(file);
		
		SemModel vsm2 = new SemModel(file, true);
		testBasic(vsm2);
		
		assertTrue(vsm2.getTotalEdgeCount() == 8);
		
		assertTrue(vsm2.getTripleCount(null, "1", "A_A") == 2);		
		assertTrue(vsm2.getTripleCount("C_C", null, "A_A") == 3);
		assertTrue(vsm2.getTripleCount("D_D", "3", null) == 2);
		
		assertTrue(vsm2.getTripleCount("C_C", null, null) == 3);
		assertTrue(vsm2.getTripleCount(null, "2", null) == 2);
		assertTrue(vsm2.getTripleCount(null, null, "A_A") == 4);
		
		assertTrue(vsm2.getTripleCount(null, null, null) == 8);
	}

}

package sem.grapheditor;

import java.util.HashSet;

import sem.graph.Graph;

/**
 * GraphEditor for explicitly adding null-edges (non-existant edges)
 *
 */
public class NullEdgesGraphEditor implements GraphEditor{

	public static String nullEdgeLabel = "nulledge";
	
	@Override
	public void edit(Graph graph) {
		Integer headId, depId;
		HashSet<String> hashes = new HashSet<String>();
		/*for(Edge edge : graph.getEdges()){
			headId = graph.getNodes().indexOf(edge.getHead());
			depId = graph.getNodes().indexOf(edge.getDep());
			if(headId == null || headId < 0 || depId == null || depId < 0)
				throw new RuntimeException("Node in an edge was not found in the list of nodes in the graph.");
			hashes.add("" + headId + "," + depId);
		}
		*/
		for(int i = 0; i < graph.getNodes().size(); i++){
			for(int j = 0; j < graph.getNodes().size(); j++){
				if(hashes.contains("" + i + "," + j))
					continue;
				if(i == j)
					continue;
				
				graph.addEdge(nullEdgeLabel, graph.getNodes().get(i), graph.getNodes().get(j));
			}
		}
	}

}

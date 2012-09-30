package sem.grapheditor;

import java.util.ArrayList;

import sem.graph.Edge;
import sem.graph.Graph;

public class ReverseEdgesGraphEditor implements GraphEditor{

	public static String reverseEdgePrefix = "rev_";
	
	@Override
	public void edit(Graph graph) {
		ArrayList<Edge> newEdges = new ArrayList<Edge>();
		for(Edge edge : graph.getEdges())
			newEdges.add(new Edge(reverseEdgePrefix + edge.getLabel(), edge.getDep(), edge.getHead()));
		graph.getEdges().addAll(newEdges);
	}

}

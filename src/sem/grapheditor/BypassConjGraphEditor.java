package sem.grapheditor;

import java.util.LinkedHashSet;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for creating edges that bypass conjunctions
 *
 */
public class BypassConjGraphEditor implements GraphEditor{
	
	private PosMap posMap;
	
	public BypassConjGraphEditor(String posMapPath){
		this.posMap = new PosMap(posMapPath);
	}
	
	@Override
	public void edit(Graph graph) {
		LinkedHashSet<Node> activeNodes = new LinkedHashSet<Node>();
		for(Node n : graph.getNodes()){
			if(posMap.isConj(n.getPos()))
				activeNodes.add(n);
		}
		
		LinkedHashSet<Edge> headEdges = new LinkedHashSet<Edge>();
		LinkedHashSet<Edge> depEdges = new LinkedHashSet<Edge>();
		LinkedHashSet<Edge> newEdges = new LinkedHashSet<Edge>();
		String newLabel;
		
		for(Node node : activeNodes){
			headEdges.clear();
			depEdges.clear();
			
			for(Edge edge : graph.getEdges()){
				if(edge.getHead() == edge.getDep())
					continue;
				if(edge.getHead() == node)
					headEdges.add(edge);
				else if(edge.getDep() == node)
					depEdges.add(edge);
			}
			
			for(Edge headEdge : headEdges){
				for(Edge depEdge : depEdges){
					if(headEdge.getLabel().equals("conj"))
						newLabel = depEdge.getLabel();
					else
						newLabel = depEdge.getLabel() + "-" + headEdge.getLabel();
					Edge newEdge = new Edge(newLabel, depEdge.getHead(), headEdge.getDep());
					newEdges.add(newEdge);
				}
			}
		}
		graph.getEdges().addAll(newEdges);
	}

}

package sem.grapheditor;

import java.util.LinkedHashSet;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for creating edges that bypass adpositions
 *
 */
public class BypassAdpGraphEditor implements GraphEditor{

	private int bypassType;
	private PosMap posMap;

	public BypassAdpGraphEditor(int bypassType, String posMapPath){
		this.bypassType = bypassType;
		this.posMap = new PosMap(posMapPath);
	}

	
	@Override
	public void edit(Graph graph) {
		LinkedHashSet<Node> activeNodes = new LinkedHashSet<Node>();
		for(Node n : graph.getNodes()){
			if(posMap.isAdp(n.getPos()))
				activeNodes.add(n);
		}
		/*
		// Temp
		System.out.println("----");
		for(Node n : activeNodes){
			n.print();
		}
		*/
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
					newLabel = "";
					switch(bypassType){
						case 1:
							newLabel = "adp";
							break;
						case 2:
							newLabel = "adp_" + headEdge.getLabel();
							break;
						case 3:
							newLabel = depEdge.getLabel() + "_adp"; 
							break;
						case 4:
							newLabel = depEdge.getLabel() + "_adp_" + headEdge.getLabel();
							break;
						case 5:
							newLabel = headEdge.getLabel();
							break;
						case 6:
							newLabel = depEdge.getLabel();
							break;
						case 7:
							newLabel = depEdge.getLabel() + "_" + headEdge.getLabel();
							break;
						default:
							throw new RuntimeException("Unknown bypassType: " + bypassType);
					}
					Edge newEdge = new Edge(newLabel, depEdge.getHead(), headEdge.getDep());
					/*
					// Temp
					System.out.println("N: " + newEdge.toString());
					*/
					newEdges.add(newEdge);
				}
			}
		}
		graph.getEdges().addAll(newEdges);
	}
	
}

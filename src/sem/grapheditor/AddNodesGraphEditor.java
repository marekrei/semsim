package sem.grapheditor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import sem.graph.Graph;
import sem.graph.Edge;
import sem.graph.Node;

/**
 * GraphEditor for adding new nodes with reduced information to the graph
 *
 */
public class AddNodesGraphEditor implements GraphEditor{

	private int parameter;
	
	public AddNodesGraphEditor(int type){
		this.parameter = type;
	}
	
	@Override
	public void edit(Graph graph) {
		LinkedHashMap<Node,Node> nodeMapPos = new LinkedHashMap<Node,Node>();
		LinkedHashMap<Node,Node> nodeMapLemma = new LinkedHashMap<Node,Node>();
		LinkedHashSet<Edge> newEdges = new LinkedHashSet<Edge>();
		Node newNode;
		
		for(Node node : graph.getNodes()){
			if(parameter == 1 || parameter == 2 || parameter == 5 || parameter == 6 || parameter == 7 || parameter == 8){
				newNode = new Node("NOLEMMA", node.getPos());
				nodeMapPos.put(node, newNode);
			}
			
			if(parameter >= 3 && parameter <= 8){
				newNode = new Node(node.getLemma(), "NOPOS");
				nodeMapLemma.put(node, newNode);
			}
			
			if(parameter == 7 || parameter == 8){
				newEdges.add(new Edge("haspos", nodeMapLemma.get(node), nodeMapPos.get(node)));
			}
			
			if(parameter == 8){
				newEdges.add(new Edge("posof", nodeMapLemma.get(node), node));
				newEdges.add(new Edge("lemmaof", nodeMapPos.get(node), node));
			}
		}
		
		for(Edge edge : graph.getEdges()){
			if(parameter == 1 || parameter == 2 || parameter == 5 || parameter == 6 || parameter == 7 || parameter == 8){
				newEdges.add(new Edge(edge.getLabel(), edge.getHead(), nodeMapPos.get(edge.getDep())));
				newEdges.add(new Edge(edge.getLabel(), nodeMapPos.get(edge.getHead()), edge.getDep()));
			}
			if(parameter == 2 || parameter == 6 || parameter == 7 || parameter == 8){
				newEdges.add(new Edge(edge.getLabel(), nodeMapPos.get(edge.getHead()), nodeMapPos.get(edge.getDep())));
			}
			if(parameter == 3 || parameter == 4 || parameter == 5 || parameter == 6 || parameter == 7 || parameter == 8){
				newEdges.add(new Edge(edge.getLabel(), edge.getHead(), nodeMapLemma.get(edge.getDep())));
				newEdges.add(new Edge(edge.getLabel(), nodeMapLemma.get(edge.getHead()), edge.getDep()));
			}
			if(parameter == 4 || parameter == 6 || parameter == 7 || parameter == 8){
				newEdges.add(new Edge(edge.getLabel(), nodeMapLemma.get(edge.getHead()), nodeMapLemma.get(edge.getDep())));
			}
			if(parameter == 6 || parameter == 7 || parameter == 8){
				newEdges.add(new Edge(edge.getLabel(), nodeMapLemma.get(edge.getHead()), nodeMapPos.get(edge.getDep())));
				newEdges.add(new Edge(edge.getLabel(), nodeMapPos.get(edge.getHead()), nodeMapLemma.get(edge.getDep())));
			}
		}
		
		for(Node node : nodeMapPos.values())
			graph.addNode(node);
		for(Node node : nodeMapLemma.values())
			graph.addNode(node);
		for(Edge edge : newEdges)
			graph.addEdge(edge);
	}

}

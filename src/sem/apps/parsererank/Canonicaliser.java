package sem.apps.parsererank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

class NodeComparator implements Comparator<Node>{
	private ArrayList<Node> nodes;
	public NodeComparator(ArrayList<Node> nodes){
		this.nodes = nodes;
	}
	
	@Override
	public int compare(Node o1, Node o2) {
		if(o1.getLabel().compareTo(o2.getLabel()) < 0)
			return -1;
		else if(o1.getLabel().compareTo(o2.getLabel()) > 0)
			return 1;
		else
			return (this.nodes.indexOf(o1) - this.nodes.indexOf(o2));
	}
}

class EdgeComparator implements Comparator<Edge>{
	private ArrayList<Node> nodes;

	public EdgeComparator(ArrayList<Node> nodes){
		this.nodes = nodes;
	}
	
	@Override
	public int compare(Edge o1, Edge o2) {
		if(this.nodes.indexOf(o1.getHead()) < this.nodes.indexOf(o2.getHead()))
			return -1;
		else if(this.nodes.indexOf(o1.getHead()) > this.nodes.indexOf(o2.getHead()))
			return 1;
		else if(this.nodes.indexOf(o1.getDep()) < this.nodes.indexOf(o2.getDep()))
			return -1;
		else if(this.nodes.indexOf(o1.getDep()) > this.nodes.indexOf(o2.getDep()))
			return 1;
		else 
			return o1.getLabel().compareTo(o2.getLabel());
	}
}

/**
 * Creates a canonical string form of a graph 
 *
 */
public class Canonicaliser {

	public static String getString(Graph graph){
		ArrayList<Node> nodes = new ArrayList<Node>(graph.getNodes());
		ArrayList<Edge> edges = new ArrayList<Edge>(graph.getEdges());
		Collections.sort(nodes, new NodeComparator(nodes));
		Collections.sort(edges, new EdgeComparator(nodes));
		
		ArrayList<String> strings = new ArrayList<String>();
		
		for(Node n : nodes){
			strings.add(n.getLabel());
		}
		
		if(edges.size() > 0)
			strings.add("[GRS]");
		
		for(Edge e : edges){
			strings.add(nodes.indexOf(e.getHead()) + "," + e.getLabel() + "," + nodes.indexOf(e.getDep()));
		}
		if(strings.size() == 0)
			return "";
		
		String str = "";
		for(String s : strings)
			str += s + " | ";
		
		return str;
	}
}

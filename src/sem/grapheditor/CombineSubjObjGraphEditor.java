package sem.grapheditor;

import java.util.Arrays;
import java.util.LinkedHashSet;

import sem.graph.Edge;
import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for creating additional edges between the subjects and objects of a verb
 *
 */
public class CombineSubjObjGraphEditor implements GraphEditor{
	
	private int combineMode;
	private PosMap posMap;
	
	public CombineSubjObjGraphEditor(int combineMode, String posMapPath){
		this.combineMode = combineMode;
		this.posMap = new PosMap(posMapPath);
	}
	
	public boolean startsSubj(String label){
		for(String s : Arrays.asList("subj", "ncsubj", "xsubj", "csubj")){
			if(label.startsWith(s))
				return true;
		}
		return false;
	}
	
	public boolean startsObj(String label){
		for(String s : Arrays.asList("obj", "dobj", "iobj", "obj2")){
			if(label.startsWith(s))
				return true;
		}
		return false;
	}

	@Override
	public void edit(Graph graph) {
		LinkedHashSet<Node> activeNodes = new LinkedHashSet<Node>();
		for(Node n : graph.getNodes()){
			if(posMap.isVerb(n.getPos()))
				activeNodes.add(n);
		}
		
		LinkedHashSet<Edge> headEdges = new LinkedHashSet<Edge>();
		LinkedHashSet<Edge> newEdges = new LinkedHashSet<Edge>();
		String newLabel;
		boolean openClass;

		for(Node node : activeNodes){
			headEdges.clear();
			
			for(Edge edge : graph.getEdges()){
				if(edge.getHead() == edge.getDep())
					continue;
				if(edge.getHead() == node)
					headEdges.add(edge);
			}
			
			for(Edge headEdge1 : headEdges){
				loop: for(Edge headEdge2 : headEdges){
					if(headEdge1 == headEdge2)
						continue;
					if(!startsSubj(headEdge1.getLabel()) || !startsObj(headEdge2.getLabel()))
						continue;
					
					if(posMap.isOpenClass(headEdge1.getDep().getPos()) && posMap.isOpenClass(headEdge2.getDep().getPos()))
						openClass = true;
					else
						openClass = false;
					
					newLabel = "";
					switch(this.combineMode){
						case 1:
							newLabel = "subjobj";
							break;
						case 2:
							if(!openClass) 
								continue loop;
							newLabel = "subjobj";
							break;
						case 3:
							newLabel = headEdge1.getLabel() + "-" + headEdge2.getLabel();
							break;
						case 4:
							if(!openClass)
								continue loop;
							newLabel = headEdge1.getLabel() + "-" + headEdge2.getLabel();
							break;
						default:
							throw new RuntimeException("Unknown parameter in GraphEditor.editCombineSubjObj(): " + this.combineMode);
					}
					Edge newEdge = new Edge(newLabel, headEdge1.getDep(), headEdge2.getDep());
					newEdges.add(newEdge);
				}
			}
		}
		graph.getEdges().addAll(newEdges);
	}

}

package sem.grapheditor;

import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for lowercasing lemmas
 *
 */
public class LowerCaseGraphEditor implements GraphEditor{

	@Override
	public void edit(Graph graph) {
		for(Node n : graph.getNodes()){
			n.setLemma(n.getLemma().toLowerCase());
		}
	}

}

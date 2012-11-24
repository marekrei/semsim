package sem.grapheditor;

import sem.graph.Graph;
import sem.graph.Node;
import sem.util.StringMap;

/**
 * GraphEditor for applying lemmatisation, based on a lemmamap
 *
 */
public class LemmatiserGraphEditor implements GraphEditor{
	private StringMap lemmaMap;
	
	public LemmatiserGraphEditor(String lemmaMapPath){
		this.lemmaMap = new StringMap(lemmaMapPath);
	}
	
	public String lemmatise(String word){
		if(Graph.ellip.getLemma().equals(word) || Graph.nil.getLemma().equals(word))
			return word;
		else if(this.lemmaMap.containsKey(word))
			return this.lemmaMap.get(word);
		else
			return word;
	}

	@Override
	public void edit(Graph graph) {
		for(Node node : graph.getNodes()){
			node.setLemma(lemmatise(node.getLemma()));
		}
	}
}

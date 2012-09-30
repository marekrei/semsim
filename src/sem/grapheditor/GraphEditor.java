package sem.grapheditor;

import sem.graph.Graph;

/** 
 * The general interface for a grapheditor.
 * Grapheditors can be used to modify the dependency graph according to some rules, e.g. lowercasing, adding new edges, rewriting POS tags, etc.
 * Multiple grapheditors can be applied in sequence.
 *
 */
public interface GraphEditor {
	public void edit(Graph graph);
}

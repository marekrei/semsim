package sem.grapheditor;

import sem.graph.Graph;
import sem.graph.Node;

/**
 * GraphEditor for converting the POS labels
 *
 */
public class ConvertPosGraphEditor implements GraphEditor{

	private int conversionType;
	private PosMap posMap;
	
	public static int CONVERSION_NONE = 0;
	public static int CONVERSION_ALL = 1;
	public static int CONVERSION_POSMAP = 2;
	public static int CONVERSION_1CHAR = 3;
	public static int CONVERSION_2CHAR = 3;
	
	public ConvertPosGraphEditor(int conversionType, String posMapPath){
		this.conversionType = conversionType;
		this.posMap = new PosMap(posMapPath);
	}
	
	@Override
	public void edit(Graph graph) {
		for(Node n : graph.getNodes()){
			if(conversionType == CONVERSION_POSMAP && this.posMap.containsKey(n.getPos()))
				n.setPos(this.posMap.get(n.getPos()));
			else if(conversionType == CONVERSION_1CHAR && n.getPos().length() > 1)
				n.setPos(n.getPos().substring(0, 1));
			else if(conversionType == CONVERSION_2CHAR && n.getPos().length() > 2)
				n.setPos(n.getPos().substring(0, 2));
			else if(conversionType == CONVERSION_ALL)
				n.setPos("POS");
		}
	}

}

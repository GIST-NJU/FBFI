package MainAlgorithm;

import java.util.HashSet;
import java.util.Set;

public class Node {
	public String nodeID;
	public int layerID;
	public String layerName;
	public Set<String> subNodes = new HashSet<>();
	public Set<String> preNodes = new HashSet<>();
	public boolean available;

	public Node(String nodeID, int layerID, String layerName) {
		this.nodeID = nodeID;
		this.layerID = layerID;
		this.layerName = layerName;
		this.available = true;
	}

	protected Node(String nodeID, int layerID, Set<String> subnodes) {
		this.nodeID = nodeID;
		this.layerID = layerID;
		this.layerName = String.valueOf(layerID);
		this.subNodes = subnodes;
		this.available = true;
	}

	protected void PrintEdge() {
		String subNodes_string = "SubNodes: ";
		if (subNodes.isEmpty())
			subNodes_string += "\\";
		else
			for (String subNode : subNodes)
				subNodes_string += subNode + " ";
		IO.Output(subNodes_string);

		String preNodes_string = "PreNodes: ";
		if (preNodes.isEmpty())
			preNodes_string += "\\";
		else
			for (String preNode : preNodes)
				preNodes_string += preNode + " ";
		IO.Output(preNodes_string+"\n");
	}

	public void PrintNode() {
		IO.Output("NodeID: " + nodeID);
		IO.Output("Layer: " + layerID);
		PrintEdge();
	}
}

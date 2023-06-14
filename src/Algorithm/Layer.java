package Algorithm;

import java.util.Set;

public class Layer {
	public int layerID;
	public String layerName;
	public Set<String> layerNodes;

	public Layer(int layerID, String layerName, Set<String> layerNodes) {
		this.layerID = layerID;
		this.layerName = layerName;
		this.layerNodes = layerNodes;
	}
}

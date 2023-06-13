package Algorithm;

import java.util.List;
import java.util.Map;

public class Graph {
	public Map<String, Node> nodes;
	public Map<String, Layer> layers;
	public List<String> layersName;

	public Graph(Map<String, Node> nodes, Map<String, Layer> layers, List<String> layersName) {
		this.nodes = nodes;
		this.layers = layers;
		this.layersName = layersName;
	}

	public void PrintGraph(boolean detail) {
		IO.Output("\n~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Graph ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
		for (String serviceName : layersName) {
			Layer layer = layers.get(serviceName);
			IO.Output("[ Layer " + layer.layerID + ": " + layer.layerName + " ]");
			if (detail) {
				IO.Output("LayerSize: " + layer.layerNodes.size());
				IO.Output("LayerNodes: " + layer.layerNodes + "\n");
				for (String nodeName : layer.layerNodes)
					nodes.get(nodeName).PrintNode();
			}
		}
	}

}

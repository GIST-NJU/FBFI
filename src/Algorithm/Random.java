package Algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Random {

	public Map<String, Node> nodes = new HashMap<>();
	Set<Set<String>> tested = new LinkedHashSet<>();
	public Random(Map<String, Node> nodes,Set<Set<String>> tested) {
		this.nodes = nodes;
		this.tested=tested;
	}

	public Set<String> RandomInject() {

		Set<String> injectConfig;
		do {
			injectConfig = new LinkedHashSet<>();
			java.util.Random random = new java.util.Random();
			int random_config_size = random.nextInt(nodes.size() - 2) + 1;
			List<String> nodelist = new ArrayList<>(nodes.keySet());
			nodelist.remove("start");
			nodelist.remove("end");
			for (int i = 0; i < random_config_size; i++) {
				int random_index = random.nextInt(nodelist.size());
				injectConfig.add(nodelist.get(random_index));
				nodelist.remove(random_index);
			}
		} while (tested.contains(injectConfig));
		tested.add(injectConfig);
		return injectConfig;
	}
}

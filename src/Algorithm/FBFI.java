package Algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;

@SuppressWarnings("serial")
public class FBFI {
	int layer_number;
	public Map<String, Node> nodes = new HashMap<>();
	public Map<String, Set<Node>> layers = new HashMap<>();
	public Map<String, Set<Set<String>>> counted = new HashMap<>();
	public Map<String, Boolean> valid = new HashMap<>();

	public FBFI(Map<String, Node> nodes) {
		this.nodes = nodes;
	}

	public FBFI(Map<String, Node> nodes, Map<String, Set<Node>> layers) {
		this.nodes = nodes;
		this.layers = layers;
	}

	public FBFI() {
	}


	boolean redundant(Set<Set<String>> config_set_A, Set<Set<String>> config_set_B, Set<String> a, Set<String> b) {
		Set<String> aUb = new LinkedHashSet<>(a);
		aUb.addAll(b);
		Set<String> a_b = new LinkedHashSet<>(a);
		a_b.removeAll(b);
		Set<String> b_a = new LinkedHashSet<>(b);
		b_a.removeAll(a);
		for (Set<String> config_a : config_set_A)
			if (aUb.containsAll(config_a) && !(config_a.containsAll(a_b)))
				return true;
		for (Set<String> config_b : config_set_B)
			if (aUb.containsAll(config_b) && !(config_b.containsAll(b_a)))
				return true;
		return false;
	}

	public Set<Set<String>> conduct(Set<Set<String>> config_set_A, Set<Set<String>> config_set_B) {
		if (config_set_A.size() == 0)
			return config_set_B;
		Set<Set<String>> result = new LinkedHashSet<>();
		for (Set<String> config_a : config_set_A) {
			for (Set<String> config_b : config_set_B) {
				Set<String> aUb = new LinkedHashSet<>(config_a);
				aUb.addAll(config_b);
				if (!redundant(config_set_A, config_set_B, config_a, config_b)) {
					result.add(aUb);
				}
			}
		}
		// System.out.println("Conduct Result: \n"+result);
		return result;
	}

	void sift(Set<Set<String>> configs) {
		Iterator<Set<String>> it = configs.iterator();
		while (it.hasNext()) {
			Set<String> config = it.next();
			for (Set<String> c : configs) {
				if (config.equals(c))
					continue;
				if (config.containsAll(c)) {
					it.remove();
					break;
				}
			}
		}
	}

	public Set<Set<String>> bottleneck(int start_layer, String node) {
		Set<Set<String>> config_set = new LinkedHashSet<>();
		if (nodes.get(node).layerID == start_layer) {
			// System.out.println("bottleneck(" + node + ")=" + config_set);
			return config_set;
		}
		// counted
		if (valid.containsKey(node) && counted.containsKey(node)) {
			// System.out.println("bottleneck(" + node + ")=" + counted.get(node));
			return new LinkedHashSet<>(counted.get(node));
		}
		for (String preNode : nodes.get(node).preNodes) {
			Set<Set<String>> c = bottleneck(start_layer, preNode);
			c.add(new LinkedHashSet<String>() {
				{
					add(preNode);
				}
			});
			// System.out.println("conduct(" + config_set + " and "+c+")");
			config_set = conduct(config_set, c);
		}
		sift(config_set);
		counted.put(node, config_set);
		valid.put(node, true);
		Set<String> set = new LinkedHashSet<>();

		for (String n : set) {
			counted.put(n, config_set);
			valid.put(n, true);
		}
		// System.out.println("bottleneck(" + node + ")=" + counted.get(node));
		return new LinkedHashSet<>(counted.get(node));
	}
	
	public static Set<Set<String>> bottleneck_split(FBFI handler, Graph graph, int start_layer, int span) {
		Set<String> nodes = graph.layers.get(graph.layersName.get(start_layer + span - 2)).layerNodes;
		Set<Set<String>> C = new LinkedHashSet<>();
		for (String node : nodes) {
			Set<Set<String>> c = handler.bottleneck(start_layer, node);
			c.add(new LinkedHashSet<String>() {
				{
					add(node);
				}
			});
			C = handler.conduct(C, c);
		}
		return C;
	}
	public static void main(String[] args) {
	}
}

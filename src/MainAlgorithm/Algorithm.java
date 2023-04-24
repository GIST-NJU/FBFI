package MainAlgorithm;

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
public class Algorithm {
	int layer_number;
	public Map<String, Node> nodes = new HashMap<>();
	public static Set<String> nodes_explored = new LinkedHashSet<>();
	public Map<String, Set<Node>> layers = new HashMap<>();
	int changed_layer;
	Map<String, Set<Set<String>>> counted = new HashMap<>();
	Map<String, Boolean> valid = new HashMap<>();
	Set<Set<String>> equivalent_set = new LinkedHashSet<>();
	public List<Set<String>> bottlenecks = new ArrayList<>();
	public List<Set<String>> defects = new ArrayList<>();
	public List<Set<String>> redundancies = new ArrayList<>();
	static List<Set<String>> failedCases = new ArrayList<>();
	int iterator_number = 0;
	int node_number = 0;
	int path_number = 0;
	int counted_times = 0;
	int judge_times = 0;

	public Set<String> LDFI_Result = new LinkedHashSet<>();

	public Algorithm(Map<String, Node> nodes) {
		this.nodes = nodes;
	}

	public Algorithm(Map<String, Node> nodes, Map<String, Set<Node>> layers) {
		this.nodes = nodes;
		this.layers = layers;
	}

	public Algorithm() {
	}

	void GenES(String node) {
		for (Set<String> es : equivalent_set)
			for (String n : es)
				if (nodes.get(node).preNodes.equals(nodes.get(n).preNodes)) {
					es.add(node);
					return;
				}
		equivalent_set.add(new LinkedHashSet<String>() {
			{
				add(node);
			}
		});
	}

	public void PathProcessing(List<String> path) {
		changed_layer = -1;
		valid = new HashMap<>();
		equivalent_set = new LinkedHashSet<>();
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes_explored.add(nodeID);
			if (changed_layer < 0 && !nodes.get(nodeID).preNodes.contains(preID))
				changed_layer = nodes.get(preID).layerID;
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		for (String nodeID : nodes.keySet()) {
			if (nodes.get(nodeID).layerID <= changed_layer)
				valid.put(nodeID, true);
			if (nodes.get(nodeID).layerID > 1)
				GenES(nodeID);
		}

	}

	public List<String> Traverse() {
		List<String> path = new ArrayList<String>();
		Set<String> visited = new HashSet<>();
		Map<String, String> next = new HashMap<>();
		Stack<String> stack = new Stack<String>();
		stack.push("start");
		while (!stack.empty()) {
			boolean traceback = true;
			String current = stack.lastElement();
			ArrayList<String> subNodes = new ArrayList<String>(nodes.get(current).subNodes);
			while (!subNodes.isEmpty()) {
				int randomIndex = new Random().nextInt(subNodes.size());
				String subnode = subNodes.get(randomIndex);
				subNodes.remove(randomIndex);

				if (subnode.equals("end")) {
					next.put(current, subnode);
					for (current = "start"; !current.equals("end"); current = subnode) {
						subnode = next.get(current);
						if (!current.equals("start"))
							path.add(current);
					}
					return path;
				}
				if (nodes.get(subnode).available && !visited.contains(subnode)) {
					visited.add(subnode);
					next.put(current, subnode);
					stack.push(subnode);
					traceback = false;
					break;
				}
			}
			if (traceback == true) {
				stack.pop();
			}
		}
		return new ArrayList<String>();
	}

	boolean redundant(Set<Set<String>> config_set_A, Set<Set<String>> config_set_B, Set<String> a, Set<String> b) {
		judge_times++;
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
			counted_times++;
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
		for (Set<String> es : equivalent_set)
			if (es.contains(node)) {
				set = es;
				break;
			}
		for (String n : set) {
			counted.put(n, config_set);
			valid.put(n, true);
		}
		// System.out.println("bottleneck(" + node + ")=" + counted.get(node));
		return new LinkedHashSet<>(counted.get(node));
	}

	public Set<Set<String>> CNF(String node) {
		Set<Set<String>> cnf = new HashSet<>();
		if (node.equals("start"))
			cnf.add(new HashSet<>());
		else
			for (Iterator<String> i = nodes.get(node).preNodes.iterator(); i.hasNext();) {
				Set<Set<String>> c = CNF(i.next());
				if (!node.equals("end")) {
					for (Set<String> s : c)
						s.add(node);
				}
				cnf.addAll(c);
			}
		return cnf;
	}

	@SuppressWarnings("resource")
	public boolean SolveLDFI(Set<Set<String>> cnf, List<Set<String>> FaultyCases, int most) {
		IO.Output("< Most: " + most + " >");

		LDFI_Result = new LinkedHashSet<>();
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		Context context = new Context(cfg);
		Solver solver = context.mkSolver();
		Map<String, BoolExpr> map = new HashMap<>();

		BoolExpr[] x = new BoolExpr[nodes.size() - 2];
		int i = 0;
		for (String node : nodes.keySet()) {
			if (!node.equals("start") && !node.equals("end")) {
				x[i] = context.mkBoolConst("x" + i);
				map.put(node, x[i++]);
			}
		}
		solver.add(context.mkAtMost(x, most));

		for (Set<String> cn : cnf) {
			BoolExpr OR = context.mkBool(false);
			for (String c : cn) {
				if (c.length() > 0)
					OR = context.mkOr(OR, map.get(c));
			}
			solver.add(OR);
		}

		for (Set<String> fc : FaultyCases) {
			BoolExpr hasTest = context.mkBool(true);
			for (String c : fc)
				hasTest = context.mkAnd(hasTest, map.get(c));
			hasTest = context.mkNot(hasTest);
			solver.add(hasTest);
		}
		// System.out.println(solver.toString());
		switch (solver.check()) {
		case SATISFIABLE:
			final Model model = solver.getModel();
			map.forEach((node, xi) -> {
				if (model.getConstInterp(xi).toString().equals("true")) {
					LDFI_Result.add(node);
				}
			});
			return true;
		case UNSATISFIABLE:
			return false;
		default:
			System.out.println("unknow");
			return false;
		}
	}

	void Inject_Restore(Set<String> config, boolean sign) {
		for (String node : config)
			nodes.get(node).available = sign;
	}

	public int minLayer(Set<String> config) {
		int minLayer = -1;
		for (String nodeID : config) {
			int currentLayer = nodes.get(nodeID).layerID;
			if (minLayer < 0 || currentLayer < minLayer)
				minLayer = currentLayer;
		}
		return minLayer;
	}

	public void Find_Defect(List<Set<String>> failedCases) {
		System.out.println(failedCases);
		for (Set<String> failed_case : failedCases) {
			System.out.println(failed_case);
			Inject_Restore(failed_case, false);
			if (Traverse().size() > 0) {
				System.out.println("defects");
				defects.add(failed_case);
			} else {
				for (String config : failed_case) {
					nodes.get(config).available = true;
					if (Traverse().size() == 0) {
						System.out.println("redundancies");
						redundancies.add(failed_case);
						break;
					}
					nodes.get(config).available = false;
				}
				System.out.println("bottlenecks");
				bottlenecks.add(failed_case);
			}
			Inject_Restore(failed_case, true);
		}
		bottlenecks.removeAll(redundancies);
	}

	public static void main(String[] args) {
	}
}

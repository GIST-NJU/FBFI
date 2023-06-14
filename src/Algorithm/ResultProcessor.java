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
public class ResultProcessor {
	public Map<String, Node> nodes = new HashMap<>();
	public List<Set<String>> bottlenecks = new ArrayList<>();
	public List<Set<String>> defects = new ArrayList<>();
	public List<Set<String>> redundancies = new ArrayList<>();

	public ResultProcessor(Map<String, Node> nodes) {
		this.nodes = nodes;
	}

	public void PathProcessing(List<String> path) {
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
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

	void Inject_Restore(Set<String> config, boolean sign) {
		for (String node : config)
			nodes.get(node).available = sign;
	}

	public void findDefect(List<Set<String>> failedCases) {
		for (Set<String> failed_case : failedCases) {
			Inject_Restore(failed_case, false);
			if (Traverse().size() > 0) {
				defects.add(failed_case);
			} else {
				for (String config : failed_case) {
					nodes.get(config).available = true;
					if (Traverse().size() == 0) {
						redundancies.add(failed_case);
						break;
					}
					nodes.get(config).available = false;
				}
				bottlenecks.add(failed_case);
			}
			Inject_Restore(failed_case, true);
		}
		bottlenecks.removeAll(redundancies);
	}
}

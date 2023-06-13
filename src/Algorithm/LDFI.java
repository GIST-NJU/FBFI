package Algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;

public class LDFI {
	public Set<String> LDFI_Result = new LinkedHashSet<>();
	public Map<String, Node> nodes = new HashMap<>();
	
	public LDFI(Map<String, Node> nodes) {
		this.nodes = nodes;
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

}

package Algorithm;

import java.util.HashSet;
import java.util.Set;

import GenQ.Fault;
import GenQ.Q;

public class RandomInjection {

	public String MainFunc(String model, String excludes_edges, int M){
		int fail_cases = 0;
		int bottlenecks = 0;
		int random_nodes = 0;
		int random_edges = 0;
		long startTime = System.currentTimeMillis();
		for(int index = 0; index < 1; index++){
			Graph g = new Graph(model);
			g.cutoff_edges(excludes_edges);
			//System.out.println(model);
			String path = g.TrackTrace();
			RandomFaultSet rfs = new RandomFaultSet();
			rfs.AddTrace(path);
			
			for(int j = 0;j<M;j++){
				//int t = Math.min(T, rfs.nodes.size());
				Fault fault = rfs.getRandomFault(rfs.nodes.size());
				g.InjectFaults(fault.toString());
				if ((path = g.TrackTrace()).equals("")) {
					rfs.valid_faults.add(fault);
				} else
					rfs.AddTrace(path);
			}

			fail_cases += rfs.valid_faults.size();
			random_nodes += rfs.nodes.size();
			random_edges += rfs.edges.size();
			Set<Fault> vf = rfs.valid_faults;
			Set<Fault> fs = new HashSet();
			for(Fault f : vf) {
				Fault temp;
				boolean tf = true;
				for(Fault s : fs){
					temp = new Fault(f.set);
					temp.set.retainAll(s.set);
					g.InjectFaults(temp.toString());
					if(g.TrackTrace() == "")
					{
						tf = false;
						break;
					}
				}
				if(tf) {
					fs.add(f);
				}
			}
			bottlenecks += fs.size();
		}
		Q q = new Q();
		long endTime = System.currentTimeMillis();

		return "rounds = " + M + " , fail_cases = " +  q.df.format((double)fail_cases/1) +
				" , bottlenecks = " +  q.df.format((double)bottlenecks/1) +
				" , explore_nodes = " + q.df.format((double)random_nodes/1) +
				" , explore_edges = " + q.df.format((double)random_edges/1) +
				" , time = " + (endTime - startTime) + " ms";
	}
}

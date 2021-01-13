import java.util.List;

import Algorithm.FBFI;
import Algorithm.RandomInjection;

public class ExpExplore {

	public static void main(String[] args) {
		int omit_sum = 0;		// default version, k = 0
		int index = 1;
		String model_file = "subject";

		List<String> models = new GenQ.GenFile().ReadFile(model_file + ".txt");
		//executeModel(models, index, omit_sum);

		StringBuilder sb = new StringBuilder();
		for(int i = 1 ; i <= 1 ; i++) {
			String out = executeModel(models, i, omit_sum);
			sb.append(out);
		}

		String output_file = model_file + ".out";
		System.out.println("output file path: " + output_file);
		new GenQ.GenFile().WriteFile(output_file, sb.toString());
	}

	private static String executeModel(List<String> models, int index, int omit_sum) {
		String content = models.get(index - 1);
		String model = content.split(" \\[excludes\\] ")[0];
		String excludes_edges = "";
		try{
			excludes_edges = content.split(" \\[excludes\\] ")[1];
		}catch(Exception e) {
		}

		// output data = rounds, fail_cases, bottlenecks, explore_nodes, explore_edges, time
		String output = "";
		output += "[Model " + index + "]\n";
		FBFI fb = new FBFI();
		output += fb.MainFunc(index, model, excludes_edges, omit_sum) + "\n";
		output += "Random-1x: ";
		output += new RandomInjection().MainFunc(model, excludes_edges, fb.M) + "\n";
		output += "Random-2x: ";
		output += new RandomInjection().MainFunc(model, excludes_edges, 2*fb.M) + "\n";
		output += "Random-4x: ";
		output += new RandomInjection().MainFunc(model, excludes_edges, 4*fb.M) + "\n";
		System.out.println(output);
		return output;
	}



}

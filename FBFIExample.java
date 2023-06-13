import java.util.*;
import Main.FBFIUsage;

public class FBFIExample {

	public static void main(String[] args) throws Exception {
		FBFIUsage FBFI = new FBFIUsage() {
			@Override
			/**
			 * You can use the method getInjectConfig() to get the configuration that needs
			 * to be injected into the SUT. After injecting the fault, you need to execute
			 * the SUT workload and get the new execution path as the output of this method.
			 * 
			 * @param config The configuration that needs to be injected into the SUT
			 * @return The execution path with the following form: {"A_1", "B_1", "C_1"}, or
			 *         an empty list if the SUT workload execution failed.
			 */
			public List<String> genPath(Set<String> config) throws Exception {
				List<String> path = new ArrayList<String>();
				// TODO Your code may like this:
				// Inject(config);
				// path = Tracer();
				// Restore(config);
				return path;
			}
		};
		FBFI.RunFBFIExperiment();
	}
}

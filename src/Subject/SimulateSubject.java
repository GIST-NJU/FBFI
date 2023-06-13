package Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Main.Http;

public class SimulateSubject extends ExperimentSubject {
	Workload[] workloads = {};
	public SimulateSubject() {
		this.subject_name = "Simulate";
		this.services = new ArrayList<String>();
	}
	@Override
	public boolean GenerateWorkload() throws Exception {
		return true;
	}
	public void ResetJaeger() throws Exception {
		return;
	}
	public static void main(String[] args) throws Exception {
	}
}

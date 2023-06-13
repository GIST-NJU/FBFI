package Main;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;

import Algorithm.*;
import Subject.*;

@SuppressWarnings("serial")

public abstract class FBFIUsage {
	protected static ExperimentSubject subject;
	protected static boolean Simulate = true;

	protected static enum TraceType {
		Jaeger, Scope, Abstract
	};

	protected static TraceType trace_type;
	protected static Path_Trace path_trace;
	protected static int path_number = 0;
	protected static List<Set<String>> failedCases = new ArrayList<>();
	protected static List<Set<String>> flakyCases = new ArrayList<>();
	protected static List<Set<String>> flakyCases_Reinject = new ArrayList<>();
	protected static int round_number = 0;
	protected static int inject_number = 0;
	protected static int inject_size = 0;
	protected static String inject_type = "stop";
	protected static long handle_time = 0;
	protected static long algorithm_time = 0;
	protected static long reinject_time = 0;
	protected static String algorithm_name = "";
	protected static ResultProcessor processor;
	protected static long start_time = 0;
	protected static boolean stop_early = false;
	protected static Set<String> injectConfig = new LinkedHashSet<>();
	protected static String fileaddr = "../Data/Simulate";
	protected static double time_limit = 3600;

	/**
	 * @return The set of bottlenecks calculated by FBFI
	 */
	public List<Set<String>> getBottlenecks() {
		processor = new ResultProcessor(path_trace.graph.nodes);
		processor.findDefect(failedCases);
		return processor.bottlenecks;
	}

	/**
	 * @return The set of defect revealing configurations found by FBFI
	 */
	public List<Set<String>> getDefects() {
		processor = new ResultProcessor(path_trace.graph.nodes);
		processor.findDefect(failedCases);
		return processor.defects;
	}

	/**
	 * @return The set of failure-causing configurations
	 */
	public List<Set<String>> getFailedCases() {
		return failedCases;
	}

	/**
	 * @return The fault injection configuration that needs to be injected into the
	 *         system
	 */
	public Set<String> getInjectConfig() {
		return injectConfig;
	}

	/**
	 * @return The complete business structure
	 */
	public Graph getGraph() {
		return path_trace.graph;
	}

	/**
	 * You can use the method getInjectConfig() to get the configuration that needs
	 * to be injected into the SUT. After injecting the fault, you need to execute
	 * the SUT workload and get the new execution path as the output of this method.
	 * 
	 * @param config The configuration that needs to be injected into the SUT
	 * @return The execution path with the following form: {"A_1", "B_1", "C_1"}, or
	 *         an empty list if the SUT workload execution failed.
	 */
	public abstract List<String> genPath(Set<String> config) throws Exception;

	/**
	 * When you have overridden the method: getPath(Set<String> config), you can use
	 * this method to perform the FBFI experiment.
	 * 
	 * @param outputfileaddr The file address where the output results are stored
	 * @throws Exception
	 */
	public void RunFBFIExperiment() throws Exception {
		Simulate = false;
		trace_type = TraceType.Abstract;
		subject = new SimulateSubject();
		try {
			RunExperiment("FBFI");
		} catch (Exception e) {
			e.printStackTrace();
			IO.Output("Encountered exception, Experiment aborted.");
		}
	}

	protected static void Summary() throws Exception {
		if (path_trace == null) {
			return;
			// throw new Experiment_Exception("< Summary ERROR! >");
		}
		Graph graph = path_trace.graph;
		if (graph == null) {
			return;
		}
		IO.Output("\nTotal Graph");
		graph.PrintGraph(true);
		processor = new ResultProcessor(graph.nodes);
		processor.findDefect(failedCases);
		IO.Output("\nBottlenecks: \n" + processor.bottlenecks);
		IO.Output("bottleneckNumber = " + processor.bottlenecks.size());
		IO.Output("\nRedundancies: \n" + processor.redundancies);
		IO.Output("redundanciesNumber = " + processor.redundancies.size());
		IO.Output("\nDefects: \n" + processor.defects);
		IO.Output("defectNumber = " + processor.defects.size() + "\n");

		Map<String, Integer> scale_map = new HashMap<>();
		int node_explored = graph.nodes.size() - 2;
		int node_total = 0;
		if (!Simulate && !(trace_type == TraceType.Abstract)) {
			IO.Output("\nFlaky Defects: \n" + flakyCases + "\n");
			IO.Output("\nFlaky By Reinject: \n" + flakyCases_Reinject + "\n");
			List<String> scale_list = IO.Read(fileaddr + "/scale.txt");
			for (String scale : scale_list) {
				String layerName = scale.split(" = ")[0];
				int layerSize_total = Integer.valueOf(scale.split(" = ")[1]);
				int layerSize_explored = graph.layers.get(layerName).layerNodes.size();
				scale_map.put(layerName, layerSize_total);
				IO.Output(layerName + ": " + layerSize_explored + "/" + layerSize_total);
			}
			for (String serviceName : graph.layers.keySet())
				node_total += scale_map.getOrDefault(serviceName, 1);
			IO.Output("\nNode Coverage = " + node_explored + "/" + node_total);
		} else
			IO.Output("\nNode Cover = " + node_explored);

		int edge_explored = 0;
		int edge_total = 0;
		int pre_layer_size = 0;
		Set<String> preNodes = new LinkedHashSet<String>();
		Set<String> missedEdges = new LinkedHashSet<String>();
		for (String layerName : graph.layersName) {
			for (String nodeName : graph.layers.get(layerName).layerNodes) {
				Node node = graph.nodes.get(nodeName);
				edge_explored += node.preNodes.size();
				Set<String> tempNodes = new LinkedHashSet<String>(preNodes);
				tempNodes.removeAll(node.preNodes);
				for (String tempNode : tempNodes) {
					missedEdges.add(tempNode + " -> " + nodeName);
				}
			}
			preNodes = graph.layers.get(layerName).layerNodes;
			int layer_size = scale_map.getOrDefault(layerName, 1);
			edge_total += layer_size * pre_layer_size;
			pre_layer_size = layer_size;
		}
		edge_explored -= graph.nodes.get("start").subNodes.size();
		if (!Simulate && !(trace_type == TraceType.Abstract))
			IO.Output("\nEdge Coverage = " + edge_explored + "/" + edge_total);
		else
			IO.Output("\nEdge Cover = " + edge_explored);
		IO.Output("Missed Edge = " + missedEdges);
		IO.Output("\nPath Number = " + path_number);
		IO.Output("Inject Number = " + inject_number);
		DecimalFormat df = new DecimalFormat(".00");
		IO.Output("Average Inject Size = " + df.format((double) inject_size / inject_number));
		double total_time = (double) (System.currentTimeMillis() - start_time) / 1000;
		IO.Output("\nTotal Time = " + total_time + "s");
		if (algorithm_name.equals("FBFI") && !Simulate && !(trace_type == TraceType.Abstract)) {
			List<String> limit = new ArrayList<String>() {
				{
					add("FBFI_inject_number = " + inject_number);
					add("FBFI_total_time = " + total_time);
				}
			};
			IO.Write(fileaddr + "/outputs_LDFI.txt", true, limit);
			IO.Write(fileaddr + "/outputs_Random.txt", true, limit);
		}
		IO.Output("HandleConfig Time = " + (double) handle_time / 1000 + "s");
		IO.Output("Algorithm Time = " + (double) algorithm_time / 1000 + "s");
		if (!Simulate && !(trace_type == TraceType.Abstract))
			IO.Output("ReInject Time = " + (double) reinject_time / 1000 + "s");

		if (trace_type == TraceType.Abstract)
			IO.Write("./outputs_FBFI.txt", true, IO.outputs);
		else
			IO.Write(fileaddr + "/outputs_" + algorithm_name + ".txt", true, IO.outputs);
		IO.outputs = new ArrayList<>();
	}

	static void CheckSystem(Set<String> config) throws Exception {
		IO.Output("Checking workload on unbroken system");
		for (int restart_time = 1; restart_time <= 5; restart_time++) {
			for (int retry_time = 1; retry_time <= 2; retry_time++) {
				Thread.sleep(retry_time * 5000);
				if (subject.GenerateWorkload()) {
					System.out.println("[ OK ]");
					return;
				}
				IO.Output("[ TracePath unexpectedly failed! Retry for " + retry_time + " times ]");
			}
			if (restart_time < 3) {
				IO.Output("[ Restart config and dashboard for " + restart_time + " times ]");
				subject.Restore(config, "restart", trace_type == TraceType.Jaeger);
				subject.ResetDashboard(trace_type == TraceType.Jaeger);
			} else {
				IO.Output("[ Restart Subject for " + (restart_time - 2) + " times ]");
				subject.ResetSubject();
			}
		}
		throw new Experiment_Exception("< Error: System has been broken unexpectedly! >");
	}

	boolean HandleConfig(Set<String> config, String algorithm) throws Exception {
		Graph graph = path_trace.graph;
		if (Simulate)
			IO.Output("\nPlease inject config: " + config + "\n...\n");
		else {
			IO.Output("\nInject Config: " + config);
			if (trace_type != TraceType.Abstract)
				subject.Inject(config, inject_type, trace_type == TraceType.Jaeger);
		}
		inject_number++;
		inject_size += config.size();
		List<String> path = new ArrayList<String>();
		boolean success = false;
		if (Simulate) {
			System.out.println(
					"Please input the new execution path (if the system works correctly), \nor \"-\" (if the system fails in all cases), \nor \"stop\" (if you want to stop the test process early):");
			String input = new Scanner(System.in).nextLine();
			if (input.contains("stop")) {
				IO.Output("Stopped early");
				stop_early = true;
				return false;
			}
			path = new ArrayList<String>(Arrays.asList(input.split("-")));
			success = path.size() > 0;
			if (!success)
				IO.Output("No Path");
			for (String nodeID : path) {
				String serviceName = nodeID.split("_")[0];
				int layerID = graph.layers.get(serviceName).layerID;
				graph.layers.get(serviceName).layerNodes.add(nodeID);
				graph.nodes.putIfAbsent(nodeID, new Node(nodeID, layerID, serviceName));
			}
		} else if (trace_type != TraceType.Abstract) {
			long reinject_start_time = 0;
			int reinject;
			for (reinject = 0; reinject < 3; reinject++) {
				for (int j = 0; j < 3; j++) {
					if (subject.GenerateWorkload()) {
						if (trace_type == TraceType.Jaeger)
							path = path_trace.TracePath_Jaeger();
						else
							path = path_trace.TracePath_Scope();
						if (path.size() > 0) {
							success = true;
							if (reinject > 0 || j > 0) {
								IO.Output("Flaky Case: " + config);
								flakyCases.add(config);
								if (reinject > 0) {
									IO.Output("Flaky by Reinject");
									flakyCases_Reinject.add(config);
								}
							}
							break;
						}
					}
				}
				if (success)
					break;
				if (reinject == 0)
					reinject_start_time = System.currentTimeMillis();
				if (reinject < 2) {
					subject.Restore(config, inject_type, trace_type == TraceType.Jaeger);
					subject.Inject(config, inject_type, trace_type == TraceType.Jaeger);
				}
			}
			if (reinject > 0)
				reinject_time += System.currentTimeMillis() - reinject_start_time;
		} else {
			path = genPath(config);

			if (path.size() > 0)
				success = true;
		}
		if (success) {
			IO.Output("Path: " + path);
			processor = new ResultProcessor(graph.nodes);
			processor.PathProcessing(path);
			path_number++;

			if (Simulate || trace_type == TraceType.Abstract)
				System.out.println("Please restore config: " + config);
			else
				subject.Restore(config, inject_type, trace_type == TraceType.Jaeger);
			return true;
		} else {
			switch (algorithm) {
			case "LDFI":
				List<Set<String>> tempFailedCases = new ArrayList<>();
				for (Set<String> failedCase : failedCases) {
					if (!failedCase.containsAll(config))
						tempFailedCases.add(failedCase);
				}
				tempFailedCases.add(config);
				failedCases = tempFailedCases;
				break;
			case "Random":
			case "FBFI":
				failedCases.add(config);
				break;
			}

			if (Simulate || trace_type == TraceType.Abstract)
				System.out.println("Please restore config: " + config);
			else {
				subject.Restore(config, inject_type, trace_type == TraceType.Jaeger);
				CheckSystem(config);
			}
			return false;
		}
	}

	Void RunLDFI() throws Exception {
		Graph graph = path_trace.graph;
		LDFI LDFIHandler = new LDFI(graph.nodes);
		injectConfig = new LinkedHashSet<>();
		failedCases = new ArrayList<>();
		boolean satis = true;
		int most = 1;
		boolean updated = true;
		Set<Set<String>> cnf = new HashSet<>();
		while (stop_early == false && (most < LDFIHandler.nodes.size() - graph.layers.size())) {
			IO.Output("\nLDFI Round " + ++round_number);
			long algorithm_start_time = System.currentTimeMillis();
			if (updated)
				cnf = LDFIHandler.CNF("end");
			boolean solveResult = LDFIHandler.SolveLDFI(cnf, failedCases, most);
			algorithm_time += System.currentTimeMillis() - algorithm_start_time;
			if (solveResult == false) {
				most++;
				continue;
			}
			injectConfig = LDFIHandler.LDFI_Result;
			long handleconfig_start_time = System.currentTimeMillis();
			updated = HandleConfig(injectConfig, "LDFI");
			handle_time += System.currentTimeMillis() - handleconfig_start_time;

		}
		return null;
	}

	void RunFBFI() throws Exception {
		Graph graph = path_trace.graph;
		FBFI FBFIHandler = new FBFI(graph.nodes);
		Set<Set<String>> tested = new LinkedHashSet<>();
		injectConfig = new LinkedHashSet<>();
		int step_number_FBFI = 0;
		for (int span = 1; span <= graph.layers.size(); span++) {
			for (int start_layer = 1; start_layer + span - 1 <= graph.layers.size(); start_layer++) {
				IO.Output("\n{ FBFI Step " + ++step_number_FBFI + " }");
				IO.Output("[ Span: " + span + " ]");
				IO.Output("[ Layer: " + graph.layersName.subList(start_layer - 1, start_layer + span - 1) + " ]\n");
				boolean updated = true;
				Set<Set<String>> C = new LinkedHashSet<>();
				// Algorithm split_handler = new Algorithm(graph.nodes);
				// FBFIhandler = new FBFI(graph.nodes);
				while (stop_early == false) {
					IO.Output("\nFBFI Round " + ++round_number);
					// If the path is updated, the bottleneck is recalculated.
					if (updated) {
						IO.Output("Node Number: " + (graph.nodes.size() - 2));
						long algorithm_start_time = System.currentTimeMillis();
						C = FBFI.bottleneck_split(FBFIHandler, graph, start_layer, span);
						algorithm_time += System.currentTimeMillis() - algorithm_start_time;
						IO.Output("Bottleneck result: " + C);
					} else
						IO.Output("Graph Unchanged");
					IO.Output("Bottleneck Size: " + C.size());
					boolean hasTestedAll = true;
					for (Set<String> c : C) {
						if (!tested.contains(c)) {
							injectConfig = c;
							hasTestedAll = false;
						}
					}
					if (hasTestedAll)
						break;
					tested.add(injectConfig);
					long handle_start_time = System.currentTimeMillis();
					updated = HandleConfig(injectConfig, "FBFI");
					handle_time += System.currentTimeMillis() - handle_start_time;

				}
			}
		}

	}

	void RunRandom() throws Exception {
		if (!Simulate) {
			List<String> content = IO.Read(fileaddr + "/outputs_Random.txt");
			if (!content.isEmpty())
				time_limit = Double.valueOf(content.get(1).split(" = ")[1]);
			IO.Output("\nRandom max time = " + time_limit + "s");
		}
		Graph graph = path_trace.graph;
		failedCases = new ArrayList<>();

		Set<Set<String>> tested = new LinkedHashSet<>();
		injectConfig = new LinkedHashSet<>();

		while (Simulate && stop_early == false || !Simulate && (algorithm_time + handle_time) < time_limit * 1000) {
			IO.Output("\nRandom Round " + (inject_number + 1));
			IO.Output("Node Number: " + (graph.nodes.size() - 2));
			if (tested.size() >= Math.pow(2, graph.nodes.size() - 2) - 1) {
				IO.Output("All possibilities have been exhausted.");
				break;
			}
			long algorithm_start_time = System.currentTimeMillis();
			Random RandomHandler = new Random(graph.nodes, tested);
			injectConfig = RandomHandler.RandomInject();
			tested.add(injectConfig);
			long algorithm_end_time = System.currentTimeMillis();
			algorithm_time += algorithm_end_time - algorithm_start_time;
			HandleConfig(injectConfig, "Random");
			handle_time += System.currentTimeMillis() - algorithm_end_time;
		}
	}

	protected void RunExperiment(String algorithm) throws Exception {

		failedCases = new ArrayList<>();
		flakyCases = new ArrayList<>();
		flakyCases_Reinject = new ArrayList<>();
		round_number = 0;
		inject_number = 0;
		inject_size = 0;
		handle_time = 0;
		algorithm_time = 0;
		reinject_time = 0;
		algorithm_name = algorithm;
		stop_early = false;
		injectConfig = new LinkedHashSet<>();

		IO.Output("\n[ < Experiment Start > ]\n");
		IO.Output("subject = " + subject.subject_name);
		IO.Output("algorithm = " + algorithm_name);

		if (trace_type == TraceType.Jaeger)
			subject.ResetJaeger();
		path_trace = new Path_Trace(subject);
		IO.Output("\nRunning algorithm: " + algorithm);
		// Trace for the first time, recording all headService and their corresponding
		// graph.
		if (Simulate)
			path_trace.InitGraph_Simulate();
		else {
			switch (trace_type) {
			case Jaeger:
				path_trace.InitGraph_Jaeger();
				break;
			case Scope:
				path_trace.InitGraph_Scope();
				break;
			case Abstract:
				path_trace.InitGraph_Abstract(genPath(new HashSet<String>()));
				break;
			}
		}

		path_number = 1;
		start_time = System.currentTimeMillis();
		switch (algorithm) {
		case "LDFI":
			if (!Simulate) {
				List<String> content = IO.Read(fileaddr + "/outputs_LDFI.txt");
				if (!content.isEmpty()) {
					Double FBFI_total_time = Double.valueOf(content.get(1).split(" = ")[1]);
					time_limit = FBFI_total_time * 3;
					IO.Output("\nLDFI max time = 3 * " + FBFI_total_time + " = " + time_limit + "s");
				} else {
					IO.Output("\nLDFI max time = " + time_limit + "s");
				}
			}
			ExecutorService poll = Executors.newFixedThreadPool(1);
			Future<Void> future = poll.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					return RunLDFI();
				}
			});
			try {
				Void result = future.get((long) (time_limit * 1000), TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				IO.Output(sw.toString());
			} finally {
				poll.shutdownNow();
			}
			break;
		case "FBFI":
			RunFBFI();
			break;
		case "Random":
			RunRandom();
			break;
		default:
			throw new Experiment_Exception("< Algorithm Error! > ");
		}
		Summary();
	}

	protected void Main(String[] args) throws Exception {
		subject = new SimulateSubject();
		try {
			// Determine experimental parameters
			for (String arg : args) {
				String arg_key = arg.split("=")[0];
				String arg_value = arg.split("=")[1];
				switch (arg_key) {

				case "subject":
					fileaddr = arg_value;
					if (fileaddr.contains("SockShop")) {
						subject = new SockShop();
						Simulate = false;
						trace_type = TraceType.Scope;
					} else if (fileaddr.contains("TrainTicket")) {
						subject = new TrainTicket();
						Simulate = false;
						trace_type = TraceType.Jaeger;
					} else {
						throw new Experiment_Exception("< Subject Undefined! >");
					}
					break;
				case "algorithm":
					algorithm_name = arg_value;
					break;
				case "time":
					time_limit = Double.valueOf(arg_value);
					break;
				default:
					throw new Experiment_Exception("< Arguments ERROR! >");
				}
			}

			if (Simulate == false) {
				if (subject.Deploy(fileaddr + "/scale.txt") == false)
					throw new Experiment_Exception("< Deploy FAILED! >");
			}

			if (!algorithm_name.isEmpty()) {
				RunExperiment(algorithm_name);
			} else {
				RunExperiment("FBFI");
				RunExperiment("LDFI");
				RunExperiment("Random");
			}
		} catch (Exception e) {
			e.printStackTrace();
			IO.Output("Encountered exception, Experiment aborted.");
		}
	}

}

package Experiment;

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
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;

import MainAlgorithm.*;
import WorkLoad.*;

@SuppressWarnings("serial")

public class Experiment {
	static ExperimentSubject subject;
	static boolean Simulate = true;

	static enum TraceType {
		Jaeger, Scope
	};

	static TraceType trace_type;
	static Path_Trace path_trace;
	static int path_number = 0;
	static Map<String, Integer> scale_map = new HashMap<>();
	static List<Set<String>> failedCases = new ArrayList<>();
	static List<Set<String>> flakyCases = new ArrayList<>();
	static List<Set<String>> flakyCases_Reinject = new ArrayList<>();
	static int round_number = 0;
	static int inject_number = 0;
	static int inject_size = 0;
	static String subject_name;
	static String inject_type = "stop";
	static String port = "80";
	static long handle_time = 0;
	static long algorithm_time = 0;
	static long reinject_time = 0;
	static String algorithm_name = "";
	static int random_times = 1;
	static Algorithm FBFIHandler;
	static long start_time = 0;
	static boolean stop_early = false;
	static int deploy_scale = -1;

	public static Set<Set<String>> bottleneck_split(Algorithm handler, Graph graph, int start_layer, int span) {
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

	public static void Summary() throws Exception {
		Graph graph = path_trace.graph;
		if (graph != null) {
			IO.Output("\nTotal Graph");
			graph.PrintGraph(true);
		}
		FBFIHandler = new Algorithm(graph.nodes);
		System.out.println("\n+ + + + + Finding Defects + + + + +");
		FBFIHandler.Find_Defect(failedCases);
		IO.Output("\nBottlenecks: \n" + FBFIHandler.bottlenecks);
		IO.Output("bottleneckNumber = " + FBFIHandler.bottlenecks.size());
		IO.Output("\nRedundancies: \n" + FBFIHandler.redundancies);
		IO.Output("redundanciesNumber = " + FBFIHandler.redundancies.size());
		IO.Output("\nDefects: \n" + FBFIHandler.defects);
		IO.Output("defectNumber = " + FBFIHandler.defects.size() + "\n");

		IO.Output("\nFlaky Defects: \n" + flakyCases + "\n");
		if (!Simulate)
			IO.Output("\nFlaky By Reinject: \n" + flakyCases_Reinject + "\n");
		int node_explored = graph.nodes.size() - 2;
		int node_total = 0;
		List<String> scale_list = IO.Read("./scale.txt");
		for (String scale : scale_list) {
			String layerName = scale.split(" = ")[0];
			int layerSize_total = Integer.valueOf(scale.split(" = ")[1]);
			int layerSize_explored = graph.layers.get(layerName).layerNodes.size();
			scale_map.put(layerName, layerSize_total);
			IO.Output(layerName + ": " + layerSize_explored + "/" + layerSize_total);
		}

		for (String serviceName : graph.layers.keySet())
			node_total += scale_map.getOrDefault(serviceName, 1);
		if (Simulate)
			IO.Output("\nNode Cover = " + node_explored);
		else
			IO.Output("\nNode Coverage = " + node_explored + "/" + node_total);

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
		if (Simulate)
			IO.Output("\nEdge Cover = " + edge_explored);
		else
			IO.Output("\nEdge Coverage = " + edge_explored + "/" + edge_total);
		IO.Output("Missed Edge = " + missedEdges);
		IO.Output("\nPath Number = " + path_number);
		IO.Output("Inject Number = " + inject_number);
		DecimalFormat df = new DecimalFormat(".00");
		IO.Output("Average Inject Size = " + df.format((double) inject_size / inject_number));
		double total_time = (double) (System.currentTimeMillis() - start_time) / 1000;
		IO.Output("\nTotal Time = " + total_time + "s");
		if (algorithm_name.equals("FBFI")) {
			List<String> limit = new ArrayList<String>() {
				{
					add("FBFI_inject_number = " + inject_number);
					add("FBFI_total_time = " + total_time);
				}
			};
			IO.Write("./outputs_Random.txt", true, limit);
			IO.Write("./outputs_LDFI.txt", true, limit);
		}
		IO.Output("HandleConfig Time = " + (double) handle_time / 1000 + "s");
		IO.Output("Algorithm Time = " + (double) algorithm_time / 1000 + "s");
		if (!Simulate)
			IO.Output("ReInject Time = " + (double) reinject_time / 1000 + "s");
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

	static boolean HandleConfig(Set<String> config, String algorithm) throws Exception {
		Graph graph = path_trace.graph;
		if (Simulate)
			IO.Output("\nPlease inject config: " + config+"\n...\n");
		else {
			IO.Output("\nInject Config: " + config);
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
		} else {
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
				reinject_start_time = System.currentTimeMillis();
				if (reinject < 2) {
					subject.Restore(config, inject_type, trace_type == TraceType.Jaeger);
					subject.Inject(config, inject_type, trace_type == TraceType.Jaeger);
				}
			}
			if (reinject > 0)
				reinject_time += System.currentTimeMillis() - reinject_start_time;
		}
		if (success) {
			IO.Output("Path: " + path);
			FBFIHandler.PathProcessing(path);
			path_number++;

			if (Simulate)
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

			if (Simulate)
				System.out.println("Please restore config: " + config);
			else {
				subject.Restore(config, inject_type, trace_type == TraceType.Jaeger);
				CheckSystem(config);
			}
			return false;
		}
	}

	static Void RunLDFI() throws Exception {
		Graph graph = path_trace.graph;
		FBFIHandler = new Algorithm(graph.nodes);
		Set<String> config = new LinkedHashSet<>();
		failedCases = new ArrayList<>();
		boolean satis = true;
		int most = 1;
		boolean updated = true;
		Set<Set<String>> cnf = new HashSet<>();
		while (stop_early == false && (most < FBFIHandler.nodes.size() - FBFIHandler.layers.size())) {
			IO.Output("\nLDFI Round " + ++round_number);
			long algorithm_start_time = System.currentTimeMillis();
			if (updated)
				cnf = FBFIHandler.CNF("end");
			boolean solveResult = FBFIHandler.SolveLDFI(cnf, failedCases, most);
			algorithm_time += System.currentTimeMillis() - algorithm_start_time;
			if (solveResult == false) {
				most++;
				continue;
			}
			config = FBFIHandler.LDFI_Result;
			long handleconfig_start_time = System.currentTimeMillis();
			updated = HandleConfig(config, "LDFI");
			handle_time += System.currentTimeMillis() - handleconfig_start_time;

		}
		return null;
	}

	static void RunFBFI() throws Exception {
		Graph graph = path_trace.graph;
		FBFIHandler = new Algorithm(graph.nodes);
		Set<Set<String>> tested = new LinkedHashSet<>();
		Set<String> config = new LinkedHashSet<>();
		int step_number_FBFI = 0;
		for (int span = 1; span <= graph.layers.size(); span++) {
			for (int start_layer = 1; start_layer + span - 1 <= graph.layers.size(); start_layer++) {
				IO.Output("\n{ FBFI Step " + ++step_number_FBFI + " }");
				IO.Output("[ Span: " + span + " ]");
				IO.Output("[ Layer: " + graph.layersName.subList(start_layer - 1, start_layer + span - 1) + " ]\n");
				boolean updated = true;
				Set<Set<String>> C = new LinkedHashSet<>();
				Algorithm split_handler = new Algorithm(graph.nodes);
				while (stop_early == false) {
					IO.Output("\nFBFI Round " + ++round_number);
					// If the path is updated, the bottleneck is recalculated.
					if (updated) {
						IO.Output("Node Number: " + (graph.nodes.size() - 2));
						long algorithm_start_time = System.currentTimeMillis();
						C = bottleneck_split(split_handler, graph, start_layer, span);
						algorithm_time += System.currentTimeMillis() - algorithm_start_time;
						IO.Output("Bottleneck result: " + C);
					} else
						IO.Output("Graph Unchanged");
					IO.Output("Bottleneck Size: " + C.size());
					boolean hasTestedAll = true;
					for (Set<String> c : C) {
						if (!tested.contains(c)) {
							config = c;
							hasTestedAll = false;
						}
					}
					if (hasTestedAll)
						break;
					tested.add(config);
					long handle_start_time = System.currentTimeMillis();
					updated = HandleConfig(config, "FBFI");
					handle_time += System.currentTimeMillis() - handle_start_time;

				}
			}
		}

	}

	static void RunRandom() throws Exception {
		Double Random_max_time = 3600.0;
		if (!Simulate) {
			Random_max_time = Double.valueOf(IO.Read("./outputs_Random.txt").get(1).split(" = ")[1]);
			IO.Output("\nRandom max time = " + Random_max_time + "s");
		}
		Graph graph = path_trace.graph;
		FBFIHandler = new Algorithm(graph.nodes);
		failedCases = new ArrayList<>();

		Set<Set<String>> tested = new LinkedHashSet<>();
		Set<String> config = new LinkedHashSet<>();

		while (Simulate && stop_early == false
				|| !Simulate && (algorithm_time + handle_time) < Random_max_time * 1000) {
			IO.Output("\nRandom Round " + (inject_number + 1));
			IO.Output("Node Number: " + (graph.nodes.size() - 2));
			if (tested.size() >= Math.pow(2, graph.nodes.size() - 2) - 1) {
				IO.Output("All possibilities have been exhausted.");
				break;
			}
			long algorithm_start_time = System.currentTimeMillis();
			do {
				config = new LinkedHashSet<>();
				Random random = new Random();
				int random_config_size = random.nextInt(graph.nodes.size() - 2) + 1;
				List<String> nodes = new ArrayList<>(graph.nodes.keySet());
				nodes.remove("start");
				nodes.remove("end");
				for (int i = 0; i < random_config_size; i++) {
					int random_index = random.nextInt(nodes.size());
					config.add(nodes.get(random_index));
					nodes.remove(random_index);
				}
				// System.out.println("tested: "+tested.size());
			} while (tested.contains(config));
			tested.add(config);
			long algorithm_end_time = System.currentTimeMillis();
			algorithm_time += algorithm_end_time - algorithm_start_time;
			HandleConfig(config, "Random");
			handle_time += System.currentTimeMillis() - algorithm_end_time;
		}
	}

	static void RunExperiment(String algorithm) throws Exception {
		path_trace = new Path_Trace(subject);
		IO.Output("\nRunning algorithm: " + algorithm);
		// Trace for the first time, recording all headService and their corresponding
		// graph.
		if (Simulate)
			path_trace.InitGraph_Simulate();
		else if (trace_type == TraceType.Jaeger)
			path_trace.InitGraph_Jaeger();
		else
			path_trace.InitGraph_Scope();
		path_number++;
		start_time = System.currentTimeMillis();
		switch (algorithm) {
		case "LDFI":
			Double FBFI_total_time = 3600.0;
			Double LDFI_max_time = FBFI_total_time * 3;
			if (!Simulate) {
				FBFI_total_time = Double.valueOf(IO.Read("./outputs_LDFI.txt").get(1).split(" = ")[1]);
				LDFI_max_time = FBFI_total_time * 3;
				IO.Output("\nLDFI max time = 3 * " + FBFI_total_time + " = " + LDFI_max_time + "s");
			}
			ExecutorService poll = Executors.newFixedThreadPool(1);
			Future<Void> future = poll.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					return RunLDFI();
				}
			});
			try {
				Void result = future.get((long) (LDFI_max_time * 1000), TimeUnit.MILLISECONDS);
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
	}

	public static void main(String[] args) throws Exception {
		subject = new SimulateSubject();
		try {
			// Determine experimental parameters
			for (String arg : args) {
				String arg_key = arg.split("=")[0];
				String arg_value = arg.split("=")[1];
				switch (arg_key) {
				case "port":
					port = arg_value;
					subject.port = arg_value;
					IO.Output("port = " + arg_value);
					break;
				case "subject":
					switch (arg_value) {
					case "TrainTicket":
						subject = new TrainTicket();
						Simulate = false;
						trace_type = TraceType.Jaeger;
						break;
					case "SockShop":
						subject = new SockShop(port);
						Simulate = false;
						trace_type = TraceType.Scope;
						break;
					default:
						throw new Experiment_Exception("< SubjectName ERROR! >");
					}
					break;
				case "algorithm":
					algorithm_name = arg_value;
					if (arg_value.split("-").length == 2)
						random_times = Integer.valueOf((arg_value.split("-")[1]).split("x")[0]);
					break;
				case "injectType":
					inject_type = arg_value;
					break;
				case "deployScale":
					switch (arg_value) {
					case "small":
						deploy_scale = 1;
						break;
					case "medium":
						deploy_scale = 2;
						break;
					case "large":
						deploy_scale = 3;
						break;
					default:
						throw new Experiment_Exception("< DeployScale ERROR! >");
					}
					break;
				default:
					throw new Experiment_Exception("< Arguments ERROR! >");
				}
			}
			if (!algorithm_name.isEmpty()) {
				IO.Output("\n[ < Experiment Start > ]\n");
				IO.Output("subject = " + subject.subject_name);
				IO.Output("algorithm = " + algorithm_name);
				if (trace_type == TraceType.Jaeger)
					subject.ResetJaeger();
				algorithm_name = algorithm_name.split("-")[0];
				RunExperiment(algorithm_name);
			} else if (deploy_scale > 0) {
				System.out.println(subject.GenerateScale(deploy_scale).replaceAll("sock-shop-" + port + "_", ""));
			}

		} catch (Exception e) {
			IO.Output("Encountered exception, Experiment aborted.");
		} finally {
			if (!algorithm_name.isEmpty()) {
				Summary();
				IO.Write("./outputs_" + algorithm_name + ".txt", true, IO.outputs);
			}
		}
	}
}

package Main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import Algorithm.Graph;
import Algorithm.IO;
import Algorithm.Layer;
import Algorithm.Node;
import Subject.ExperimentSubject;
import Subject.SockShop;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class Path_Trace {
	ExperimentSubject subject;
	public Graph graph;
	Set<Trace> graphTraces = new TreeSet<>();
	Map<String, Set<String>> scope_graph = new HashMap<>();
	Map<String, String> scope_nodes = new HashMap<>();

	Path_Trace(ExperimentSubject subject) {
		this.subject = subject;
		this.subject.port = subject.port;
	}

	Set<Trace> Query_Jaeger(String service_name, String operation) throws Exception {
		long end = System.currentTimeMillis();
		long start = end - 60 * 60 * 1000;
		String URL = ("http://localhost:" + subject.jaeger_port + "/api/traces?end=" + (end * 1000)
				+ "&limit=1&lookback=1h&maxDuration&minDuration"
				+ (operation.equals("") ? "" : ("&operation=" + operation)) + "&service=" + service_name + "&start="
				+ (start * 1000)).replaceAll(" ", "%20");
		System.out.println("ServiceQuery: " + URL);
		Http h = new Http("GET", URL, "", "application/json", "application/json", "", "needResponse");
		h.DoHttp();
		JSONObject response = JSONObject.fromObject(h.response_string);
		if (response == null)
			throw new Experiment_Exception(
					"< Error: Jaeger responded null when querying " + service_name + "." + operation + " ! >");
		JSONArray trace_data = response.getJSONArray("data");
		Set<Trace> traces = new HashSet<>();
		Set<String> tracesID = new HashSet<>();
		int limit_number = operation.equals("") ? trace_data.size() : 1;
		for (int trace_index = 0; trace_index < limit_number; trace_index++) {
			String traceID = trace_data.getJSONObject(trace_index).getString("traceID");
			if (tracesID.contains(traceID))
				continue;
			tracesID.add(traceID);

			JSONObject json_processes = trace_data.getJSONObject(trace_index).getJSONObject("processes");
			Map<String, String> processMap = new HashMap<>();
			for (int i = 1; i <= json_processes.size(); i++) {
				String processID = "p" + Integer.toString(i);
				JSONObject p = json_processes.getJSONObject(processID);
				processMap.put(processID,
						p.getString("serviceName") + "~~" + p.getJSONArray("tags").getJSONObject(0).getString("value"));
			}

			JSONArray json_spans = trace_data.getJSONObject(trace_index).getJSONArray("spans");
			List<Span> spans = new ArrayList<>();
			Set<String> spanIDs = new HashSet<>();
			for (int i = 0; i < json_spans.size(); i++) {
				JSONObject json_span = json_spans.getJSONObject(i);
				String spanID = json_span.getString("spanID");
				if (spanIDs.contains(spanID))
					continue;
				spanIDs.add(spanID);
				String processID = json_span.getString("processID");
				String reference_spanID = "/";
				String operationName = "/";
				// Not the source
				if (json_span.getJSONArray("references").size() > 0)
					reference_spanID = json_span.getJSONArray("references").getJSONObject(0).getString("spanID");
				// Is the source
				else
					operationName = json_span.getString("operationName");
				long startTime = json_span.getLong("startTime");
				spans.add(new Span(traceID, spanID, reference_spanID, operationName, processMap.get(processID),
						startTime));
			}
			// Spans sort by timestamp
			Collections.sort(spans);
			if (spans.get(0).operationName.equals("/"))
				continue;
			traces.add(new Trace(spans, processMap));
		}
		return traces;
	}

	public void Query_Scope() throws Experiment_Exception {
		scope_graph = new HashMap<>();
		scope_nodes = new HashMap<>();
		String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Calendar.getInstance().getTime());
		String URL = "http://localhost:4040/api/topology/containers?system=application&stopped=running&pseudo=hide&timestamp="
				+ timestamp;
		Http h = new Http("GET", URL, "", "application/json", "application/json", "", "needResponse");
		h.DoHttp();
		JSONObject response = JSONObject.fromObject(h.response_string);
		if (response == null)
			throw new Experiment_Exception("< Error: WeaveScope responded null when querying !");
		JSONObject nodes_data = response.getJSONObject("nodes");
		Iterator iterator = nodes_data.keys();
		while (iterator.hasNext()) {
			String nodeID = (String) iterator.next();
			JSONObject node_value = JSONObject.fromObject(nodes_data.getString(nodeID));
			String nodeLabel = node_value.getString("label");
			scope_nodes.put(nodeID, nodeLabel);
			Set<String> adjacency = new HashSet<String>();
			if (node_value.containsKey("adjacency")) {
				JSONArray node_adjacency = node_value.getJSONArray("adjacency");
				for (int node_index = 0; node_index < node_adjacency.size(); node_index++)
					adjacency.add(node_adjacency.getString(node_index));
			}
			scope_graph.put(nodeLabel, adjacency);
		}
	}

	public void InitGraph_Abstract(List<String> abstract_path)  {
		System.out.println(abstract_path);
		Map<String, Node> nodes = new HashMap<>();
		Map<String, Layer> layers = new HashMap<>();
		List<String> layersName = new ArrayList<>();
		int layerID = 0;
		for (String nodeID : abstract_path) {
			String serviceName = nodeID.split("_")[0];
			layers.put(serviceName, new Layer(++layerID, serviceName, new HashSet<String>() {
				{
					add(nodeID);
				}
			}));
			nodes.put(nodeID, new Node(nodeID, layerID, serviceName));
			layersName.add(serviceName);
		}
		nodes.put("start", new Node("start", 0, "START LAYER"));
		nodes.put("end", new Node("end", ++layerID, "END LAYER"));
		String preID = "start";
		abstract_path.add("end");
		for (String nodeID : abstract_path) {
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		graph = new Graph(nodes, layers, layersName);
		graph.PrintGraph(false);
	}
	
	void InitGraph_Simulate() {
		System.out.println("Initing: SUT runs without fault.");
		System.out.println("Please input a successful execution path (e.g. A_1-B_1-C_1):");
		Scanner scanner = new Scanner(System.in);
		List<String> path = new ArrayList<String>(Arrays.asList(scanner.nextLine().split("-")));
		System.out.println(path);
		InitGraph_Abstract(path);
	}

	void InitGraph_Jaeger() throws Exception {
		System.out.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Generating Workload ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
		if (subject.GenerateWorkload() == false)
			throw new Experiment_Exception("< Error: System is unavailable! >");
		// Give Jaeger some buffer time.
		Thread.sleep(20 * 1000);
		String URL = "http://localhost:" + subject.jaeger_port + "/api/services";
		Http h = new Http("GET", URL, "", "application/json", "application/json", "", "needResponse");
		h.DoHttp();
		JSONObject response = JSONObject.fromObject(h.response_string);
		if (response == null)
			throw new Experiment_Exception("< Error: Jaeger's response is null when init graphs! >");
		JSONArray service_name_data;
		try {
			service_name_data = JSONObject.fromObject(response).getJSONArray("data");
		} catch (Exception e) {
			throw new Experiment_Exception("< Error: Jaeger could not find any services! >");
		}

		IO.Output("\n[ Covered " + service_name_data.size() + " Services ]");
		for (int service_index = 0; service_index < service_name_data.size(); service_index++) {
			String service_name = service_name_data.getString(service_index);
			IO.Output(service_name);
			graphTraces.addAll(Query_Jaeger(service_name, ""));
		}
		Map<String, Node> nodes = new HashMap<>();
		Map<String, Layer> layers = new HashMap<>();
		List<String> layersName = new ArrayList<>();
		List<String> path = new ArrayList<String>();
		int layerID = 0;
		IO.Output("\n[ Trace List ]");
		for (Trace trace : graphTraces) {
			IO.Output("[ " + trace.headServiceName + "." + trace.operation + " ]");
			trace.PrintTrace();
			for (Span span : trace.spans) {
				String serviceName = span.serviceName_nodeID.split("~~")[0];
				if (!layers.containsKey(serviceName)) {
					String nodeID = span.serviceName_nodeID;
					layers.put(serviceName, new Layer(++layerID, serviceName, new HashSet<String>() {
						{
							add(nodeID);
						}
					}));
					nodes.put(nodeID, new Node(nodeID, layerID, serviceName));
					layersName.add(serviceName);
					path.add(nodeID);
				}
			}
		}
		nodes.put("start", new Node("start", 0, "START LAYER"));
		nodes.put("end", new Node("end", ++layerID, "END LAYER"));
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		graph = new Graph(nodes, layers, layersName);
		graph.PrintGraph(false);
	}

	public void InitGraph_Scope() throws Exception {
		System.out.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Generating Workload ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
		boolean success = false;
		for (int i = 0; i < 3; i++) {
			if (subject.GenerateWorkload() == true) {
				success = true;
				break;
			}
			Thread.sleep(10 * 1000);
		}
		if (success == false)
			throw new Experiment_Exception("< Error: System is unavailable! >");
		Map<String, Node> nodes = new HashMap<>();
		Map<String, Layer> layers = new HashMap<>();
		List<String> layersName = new ArrayList<>();
		int layerID = 0;
		SockShop sockshop = new SockShop(subject.port);
		IO.Output("\n[ Covered " + sockshop.services.size() + " Services ]");
		for (String layername : sockshop.services) {
			IO.Output(layername);
			layersName.add(layername);
			layers.put(layername, new Layer(++layerID, layername, new HashSet<String>()));
		}
		nodes.put("start", new Node("start", 0, "START LAYER"));
		nodes.put("end", new Node("end", ++layerID, "END LAYER"));
		graph = new Graph(nodes, layers, layersName);
		List<String> path = TracePath_Scope();
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			graph.nodes.get(nodeID).preNodes.add(preID);
			graph.nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		graph.PrintGraph(false);
	}
	
	List<String> TracePath_Jaeger() throws Exception {
		// Give Jaeger some buffer time.
		Thread.sleep(10 * 1000);
		Map<Integer, String> nodeIDs = new TreeMap<>();// Automatically sort by key from small to large
		Set<String> tracesID = new HashSet<>();
		for (Trace graphTrace : graphTraces) {
			Set<Trace> traces = Query_Jaeger(graphTrace.headServiceName, graphTrace.operation);
			for (Trace trace : traces) {
				if (tracesID.contains(trace.traceID))
					continue;
				tracesID.add(trace.traceID);
				trace.PrintTrace();
				for (String nodeID : trace.processMap.values()) {
					String serviceName = nodeID.split("~~")[0];
					// Unknown serviceName appears.
					if (!graph.layers.containsKey(serviceName)) {
						IO.Output("< Warning: serviceName \" " + serviceName + " \" is unseen! >");
						return new ArrayList<String>();
					}
					int layerID = graph.layers.get(serviceName).layerID;
					if (!nodeIDs.containsKey(layerID)) {
						graph.layers.get(serviceName).layerNodes.add(nodeID);
						graph.nodes.putIfAbsent(nodeID, new Node(nodeID, layerID, serviceName));
						nodeIDs.put(layerID, nodeID);
					}
				}
			}
		}
		List<String> path = new ArrayList<String>(nodeIDs.values());
		return path;
	}
	
	List<String> TracePath_Scope() throws Exception {
		// Give Scope some buffer time.
		Thread.sleep(5 * 1000);
		Map<Integer, String> nodeIDs = new TreeMap<>();// 自动按key从小到大排序
		Query_Scope();
		for (Entry<String, String> entry : scope_nodes.entrySet()) {
			String nodeID = entry.getValue();
			String layerName = nodeID.substring(0, entry.getValue().length() - 2);
			if (!graph.layers.containsKey(layerName))
				continue;
			int layerID = graph.layers.get(layerName).layerID;
			if (!nodeIDs.containsKey(layerID)) {
				graph.layers.get(layerName).layerNodes.add(nodeID);
				graph.nodes.putIfAbsent(nodeID, new Node(nodeID, layerID, layerName));
				nodeIDs.put(layerID, nodeID);
			}
		}
		List<String> path = new ArrayList<String>(nodeIDs.values());
		return path;
	}

	
	public static void main(String[] args) {

	}
}

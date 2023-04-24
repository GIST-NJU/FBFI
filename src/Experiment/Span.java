package Experiment;

public class Span implements Comparable<Span> {
	String traceID;
	String spanID;
	String reference_spanID;
	String operationName;
	String serviceName_nodeID;
	public long startTime;

	Span(String traceID, String spanID, String reference_spanID, String operationName, String serviceName_nodeID,
			long startTime) {
		this.traceID = traceID;
		this.spanID = spanID;
		this.reference_spanID = reference_spanID;
		this.operationName = operationName;
		this.serviceName_nodeID = serviceName_nodeID;
		this.startTime = startTime;
	}

	@Override
	public int compareTo(Span s) {
		return new Long(this.startTime).compareTo(new Long(s.startTime));
	}

	void PrintSpan() {
		System.out.println("traceID: " + traceID);
		System.out.println("spanID:  " + spanID);
		System.out.println("reference_spanID: " + reference_spanID);
		System.out.println("serviceName_nodeID: " + serviceName_nodeID);
		System.out.println("startTime:  " + startTime + '\n');
	}
}

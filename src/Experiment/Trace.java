package Experiment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trace implements Comparable<Trace>{
	List<Span>spans;
	String traceID;
	long startTime;
	String headServiceName;
	String operation;
	Map<String,String> processMap=new HashMap<>();
	public Trace(List<Span> spans,Map<String,String> processMap) {
		this.spans=spans;
		Span headSpan=this.spans.get(0);
		this.traceID=headSpan.traceID;
		this.startTime=headSpan.startTime;
		this.headServiceName=headSpan.serviceName_nodeID.split("~~")[0];
		this.operation=headSpan.operationName;
		this.processMap=processMap;
	}

	@Override
	public int compareTo(Trace t) {
		return new Long(this.startTime).compareTo(new Long(t.startTime));
	}
	@Override
    public boolean equals(Object obj) {
        if (obj instanceof Trace) 
        	return this.traceID==((Trace)obj).traceID;
        return false;
    }
	
	void PrintTrace() {
		System.out.println("{ Trace }");
		System.out.println("traceID: " + traceID);
		System.out.println("spanSize:  " + spans.size());
		System.out.println("startTime: " + startTime);
		System.out.println("headServiceName: " + headServiceName);
		System.out.println("operation:  " + operation + '\n');
	}
}

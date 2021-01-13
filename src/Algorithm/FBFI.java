package Algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import GenQ.Fault;

public class FBFI {
  public int T = 0;
  public int M;

  public String MainFunc(int model_index, String model, String excludes_edges, int omitSum) {
    String result = "";
    Graph g = new Graph(model, 0);
    g.cutoff_edges(excludes_edges);
    String fault = "";
    Set<Fault> FS = new HashSet<>();
    //FS = g.getFaults();
    //System.out.println(FS.size());
    //System.out.println(FS);
    Map tested = new HashMap();
    long Time1 = System.currentTimeMillis();
    long Time3 = System.currentTimeMillis();
    int count = 0;
    int fail_cases = 0;
    while (true) {
      g.InjectFaults(fault);
      if (fault.split("&").length > T)
        T = fault.split("&").length;
      String trace = g.TrackTrace();
      if (trace.length() > 0) {
        result += "SUCCESS;";
        //System.out.println("SUCCESS;");
        g.AddTrace(trace);
        FS = g.getFaults();
        tested.put(fault, true);
      } else {
        result += "FAIL;";
        //System.out.println("FAIL;");
        fail_cases++;
        tested.put(fault, false);
      }
      result += count + ":edges = " + g.edges.size() + ",fail_cases = " + fail_cases + ",fault = " + fault + ",";
      g.UpdateFaultInfo(FS);
      boolean stop = true;
      Fault optimal = new Fault();
      for (Fault f : FS) {
        if (!tested.containsKey(f.toString())) {
          if (stop) {
            optimal = f;
            stop = false;
            //break;
          } else {
            if (f.time < optimal.time)
              optimal = f;
          }
        }
      }
      count++;
      if (stop) {
        long Time2 = System.currentTimeMillis();
        if (g.omitSum != omitSum) {
          result += "\n" + FS + "\n";
          // System.out.println(index + ":Fail = " + FS.size() + ",Round = " + count + ";Time = " + (Time2 - Time1) + "ms;");
          g.change_omitsum(omitSum);
          FS = g.getFaults();
        } else
          break;
      } else
        fault = optimal.toString();
      result += "\n";
      Time3 = System.currentTimeMillis();
    }
    M = count - 1;

    // output the raw injection data
    new GenQ.GenFile().WriteFile(System.getProperty("user.dir") + "/raw_data/model" + model_index + ".out", result);
    //System.out.println(FS);

    return "FBFI: rounds = " + M + " , fail_cases = " + FS.size() + " , bottlenecks = " + FS.size() +
        " , explore_nodes = " + g.N + " , explore_edges = " + g.edges.size() + " , time = " + (Time3 - Time1) + " ms";
  }
}

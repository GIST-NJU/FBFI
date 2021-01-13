package Algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import GenQ.Fault;

public class RandomFaultSet {
  public Set<Fault> valid_faults = new HashSet();
  public List<String> nodes = new ArrayList();
  public List<String> edges = new ArrayList();
  public Random r = new Random();
  public List<List<String>> service_nodes = new ArrayList<>();

  public RandomFaultSet() {
  }

  public void AddTrace(String trace) {
    String[] hosts = trace.split("-");
    int l = hosts.length;
    if (service_nodes.size() == 0) {
      for (int i = 0; i < l; i++)
        service_nodes.add(new ArrayList());
    }
    String prehost = "";
    for (int i = 0; i < l; i++) {
      if (!service_nodes.get(i).contains(hosts[i]))
        service_nodes.get(i).add(hosts[i]);
      if (!nodes.contains(hosts[i]))
        nodes.add(hosts[i]);
      if (prehost.length() > 0 && !edges.contains(prehost + "-" + hosts[i]))
        edges.add(prehost + "-" + hosts[i]);
      prehost = hosts[i];
    }
    //System.out.println(service_nodes.get(0));
  }

  public Fault getRandomFault(int t) {
    Set<String> set = new HashSet();
    int index = r.nextInt(t - 1) + 1;
    Random r = new Random();
    for (int i = 0; i < index - 1; ) {
      int ran = r.nextInt(nodes.size());
      String node = nodes.get(ran);
      if (!set.contains(node)) {
        set.add(node);
        i++;
      }
    }
    //System.out.println(set.size());
    return new Fault(set);
  }

  /*
  public Set<String> getMTNodesByAlgorithm1(int MT) {
    Set<String> list = new HashSet<>();
    for (int i = 0; i < MT; i++) {
      int Min = Integer.MAX_VALUE;
      int j_min = -1;
      for (int j = 0; j < service_nodes.size(); j++) {
        List<String> ser = service_nodes.get(j);
        if (ser.size() < Min) {
          Min = ser.size();
          j_min = j;
        }
      }
      int M = service_nodes.get(j_min).size();
      String new_node = service_nodes.get(j_min).get(r.nextInt(M));
      list.add(new_node);
      service_nodes.get(j_min).add(new_node + "'");
    }
    return list;
  }*/

  public Set<String> getMTNodesByAlgorithm(int MT) {
    int explored = nodes.size();
    int K = Math.min(explored, MT);
    Set<String> list = new HashSet<>();
    while (list.size() < K) {
      String new_node = nodes.get(r.nextInt(nodes.size())).replace("'", "");
      if (!list.contains(new_node))
        list.add(new_node);
        nodes.add(new_node + "'");
    }
    return list;
  }
}

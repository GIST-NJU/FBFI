package Optimisation;

import java.util.*;

public class Model {
  public Set<Set<String>> B = new HashSet<>();
  public List<Set<String>> B_T = new ArrayList<>();
  public Set<String> ExtraNodes = new HashSet<>();
  public int MinT = Integer.MAX_VALUE;
  public int extra_nodes_num = 0;
  public int Tmax = Integer.MIN_VALUE;
  public int Num = 0;

  public Model(List<String> contents) {
    for (String con : contents) {
      if (!con.contains("-")) {
        Tmax = Math.max(Tmax, con.split("&").length);
        if (con.startsWith("FAIL")) {
          String[] s = con.split(",")[2].replace("fault = ", "").split("&");
          B.add(new HashSet<>(Arrays.asList(s)));
        }
        Num++;
      }
    }
    getT();
    getBT();
    //removeBT();
    getExtraNodes();
  }

  public void getT() {
    for (Set<String> b : B)
      MinT = Math.min(MinT, b.size());
  }

  public void getBT() {
    for (Set<String> b : B) {
      if (MinT == b.size())
        B_T.add(b);
    }
  }

  public void getExtraNodes() {
    List<Integer> index_list = new ArrayList<>();
    for (int i = 0; i < B_T.size(); i++)
      index_list.add(i);
    // System.out.println(indexlist.size());
    while (index_list.size() != 0) {
      String node = get(index_list);
      for (int i = 0; i < B_T.size(); i++) {
        Set<String> b_t = B_T.get(i);
        if (b_t.contains(node) && index_list.contains(i)) {
          index_list.remove(index_list.indexOf(i));
        }
      }
      ExtraNodes.add(node);
      //System.out.println(node + "," + index_list);
    }
    extra_nodes_num = ExtraNodes.size();
  }

  public String get(List<Integer> IndexList) {
    List<String> nodes = new ArrayList<>();
    List<Integer> times = new ArrayList<>();
    for (int index : IndexList) {
      for (String b_t : B_T.get(index)) {
        if (nodes.contains(b_t)) {
          int i = nodes.indexOf(b_t);
          times.set(i, times.get(i) + 1);
        } else {
          nodes.add(b_t);
          times.add(0);
        }
      }
    }
    int Max = Integer.MIN_VALUE;
    int i_max = -1;
    for (int i = 0; i < times.size(); i++) {
      int time = times.get(i);
      if (time > Max) {
        i_max = i;
        Max = time;
      }
    }
    return nodes.get(i_max);
  }

  public void UpdateB() {
    for (Set<String> b : B) {
      for (String node : ExtraNodes) {
        if (b.contains(node))
          b.add(node + "'");
      }
    }
  }

  public int[] ouputB() {
    int TM = Integer.MIN_VALUE;
    for (Set<String> set : B) {
      if (set.size() > TM)
        TM = set.size();
    }
    int[] count = new int[TM];
    for (int i = 0; i < TM; i++) {
      count[i] = 0;
    }
    for (Set<String> set : B) {
      count[set.size() - 1]++;
    }
    return count;
  }
}

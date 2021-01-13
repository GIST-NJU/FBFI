package GenQ;

import java.util.HashSet;
import java.util.Set;

public class Nodes {
  public Set<Integer> PreNodes = new HashSet();
  public String layer;
  public int time;

  public Nodes(String _layer) {
    layer = _layer;
  }

  public void AddInfo(String _layer, int _time) {
    layer = _layer;
    time = _time;
  }

  public void AddPreNodes(int prenode, int prenum) {
    for (int i = 0; i < prenum; i++)
      PreNodes.add(prenode + i);
    //	System.out.println(PreNodes);
  }

  public void AddPreNode(int prenode) {      //���ǰ��node;
    PreNodes.add(prenode);
  }

  public void AddOneNode(int node) {
    if (!PreNodes.contains(node))
      PreNodes.add(node);
  }

  public boolean IsNext(int node) {
    return PreNodes.contains(node);
  }
}

package Optimisation;

import Algorithm.Graph;
import Algorithm.RandomFaultSet;
import GenQ.Fault;

public class RandomOptimisation {

  public int[] MainFunc(String model_file, Model m, int K) {
    String model = model_file.split(" \\[excludes\\] ")[0];
    String excludes_edges = "";

    try {
      excludes_edges = model_file.split(" \\[excludes\\] ")[1];
    } catch (Exception e) {
    }

    Graph g = new Graph(model);
    g.cutoff_edges(excludes_edges);
    //System.out.println(model);
    String path = g.TrackTrace();
    RandomFaultSet rfs = new RandomFaultSet();
    rfs.AddTrace(path);
    int cm = 0;
    for (int j = 0; j < K * m.Num; j++) {
      int t = rfs.nodes.size();
      Fault fault = rfs.getRandomFault(t);
      g.InjectFaults(fault.toString());
      if ((path = g.TrackTrace()).equals("")) {
        rfs.valid_faults.add(fault);
      } else {
        cm++;
        //System.out.println(cm + "," + path);
        rfs.AddTrace(path);
      }
    }

    //  System.out.print(rfs.nodes.size() + ",");
     /*for(List<String> ser: rfs.service_nodes)
         System.out.print(ser.size() + "-");
     System.out.println("");*/
    //System.out.println(rfs.nodes.size());
    //////////////////////
    //	System.out.println("-----------------");
    //    failcases += rfs.valid_faults.size();
    //   random_nodesnum += rfs.nodes.size();
    //    random_edgesnum += rfs.edges.size();

    // bottlecases += fs.size();
    ///////////////////////////////
    // System.out.println(m.extra_nodes_num);
    // m.ouputB();

    //Set<String> nodes = rfs.getMTNodesByAlgorithm2(m.extra_nodes_num * K);
    //if (tag)
      //nodes = rfs.getMTNodesByAlgorithm1(m.extra_nodes_num * K);
    //else
      //nodes = rfs.getMTNodesByAlgorithm2(m.extra_nodes_num * K);

    m.ExtraNodes = rfs.getMTNodesByAlgorithm(m.extra_nodes_num * K);
    m.UpdateB();
    //System.out.println(m.ExtraNodes + " : " + Arrays.toString(m.ouputB()));
    return m.ouputB();
  }

}

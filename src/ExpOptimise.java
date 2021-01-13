import Optimisation.Model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpOptimise {

  public static void main(String[] args) throws Exception {
    String model_file = "subject";
    List<String> models = new GenQ.GenFile().ReadFile(model_file + ".txt");

    //String out = executeModel(models, 49);
    //System.out.println(out);

    StringBuilder sb = new StringBuilder();
    for (int i = 1 ; i <= models.size() ; i++) {
      String out = executeModel(models, i);
      sb.append(out);
    }

    String output_file = model_file + ".opt.out";
    System.out.println("output file path: " + output_file);
    new GenQ.GenFile().WriteFile(output_file, sb.toString());
  }

  private static String executeModel(List<String> models, int i) throws Exception {
    String raw_file = "raw_data/model" + i + ".out";
    Model m = new Model(ReadFile(raw_file));

    System.out.println(models.get(i - 1));

    String out = "[Model " + i + "]\n";
    out += "initial # t-bottlenecks: " + Arrays.toString(m.ouputB()) + "\n";

    int[] count = m.ouputB();
    int smallest_index = 0, smallest_num = 0;
    for(int x = 0 ; x < count.length ; x++) {
      if(count[x] != 0 ) {
        smallest_num = count[x];
        smallest_index = x;
        break;
      }
    }

    // bottleneck based improvement
    int M = m.extra_nodes_num;
    m.UpdateB();
    int[] count1 = m.ouputB();
    out += "bottleneck-based: M = " + M + ", " + Arrays.toString(m.ouputB()) +
        ", reduction = " + (smallest_num - count1[smallest_index])/smallest_num + "\n";

    // random based
    int[] KS = {1, 2, 4};
    for (int K : KS) {
      double sum = 0;
      for (int j = 0; j < 30; j++) {
        Model m1 = new Model(ReadFile(raw_file));
        int[] count2 = new Optimisation.RandomOptimisation().MainFunc(models.get(i - 1), m1, K);
        double reduction = (double) (smallest_num - count2[smallest_index]) / (double) (smallest_num);
        sum += reduction;
      }
      out += "Random-" + K + "x: M = " + K * M + ", reduction = " + sum / 30.0 + "\n";
    }

    System.out.println(out);
    return out;
  }

  private static List<String> ReadFile(String fileaddr) throws IOException {
    List<String> content = new ArrayList();
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileaddr)));
    String line ;
    while ((line = in.readLine()) != null) {
      content.add(line);
    }
    in.close();
    return content;
  }
}

package Tool;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import GenQ.Fault;


public class FBFI {

  public static void main(String[] args) {
    Scanner inner = new Scanner(System.in);
    int omitSum = 0;
    try {
      omitSum = Integer.parseInt(args[0]);
    } catch (Exception e) {
      omitSum = 0;
    }
    String input;
    SolvingBottlenecks sb = new SolvingBottlenecks(omitSum);
    System.out.println("Please input a successful execution path (e.g. A1-B1-C1):");
    Set<Fault> set = new HashSet<>();
    while (!(input = inner.next()).equals("-1")) {
      if (input.contains("-")) {
        //	long Time1 = System.currentTimeMillis();
        sb.AddPath(input);
        set = sb.getFaults();
        //	long Time2 = System.currentTimeMillis();
        System.out.println("Generate " + set.size() + " fault injection configurations that can break the system (please use them to implement fault injections):");
        int i = 0;
        for (Fault f : set) {
          i++;
          System.out.println("c" + i + "=" + f);
        }
      }
      System.out.println("Input the newly tracked execution path (if the system works correctly), or -1 (if the system fails in all cases):");
    }
    String content = "";
    System.out.println("The complete business structure: ");

    String str = sb.output_structure();
    System.out.println(str);
    System.out.println("The set of all fault-tolerance bottlenecks of this structure is saved in " + System.getProperty("user.dir") + "/bottlenecks.txt.");
    content += "The complete business structure: \n";
    content += str + "\n";
    content += "The set of all fault-tolerance bottlenecks of this structure: \n";
    for (Fault fau : set)
      content += fau.toString() + "\n";
    new GenQ.GenFile().WriteFile(System.getProperty("user.dir") + "/bottlenecks.txt", content);
  }

}

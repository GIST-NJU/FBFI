package Utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class getData_back {
    public static DecimalFormat df = new DecimalFormat("######0.00");
//        //fbfi
//     private static String CASE_KEY_WORD="Testcase is '[C";
//    private static String FILEPATH=System.getProperty("user.dir")+"/raw_data/fbfi.txt";
//      private static String PATH_KEY_WORD="path = ";
//      private static String PATH_KEY_WORD2="path = ";
//    private static String FAILED_KEY_WORD="Execution is failed!";
//      private static String COST_TIME_KEY_WORD="Total time is ";
//      private static String SUCCESS_KEY_WORD="Execution is successful!";
//private static int count=0;
//    //ldfi
//    private static String CASE_KEY_WORD="Testcase is '[t";
//    private static String FILEPATH=System.getProperty("user.dir")+"/raw_data/ldfi.txt";
//    private static String PATH_KEY_WORD="path=";
//    private static String PATH_KEY_WORD2="path =";
//    private static String FAILED_KEY_WORD="Failed";
//    private static String COST_TIME_KEY_WORD="time=";
//    private static String SUCCESS_KEY_WORD="Successful";
//    private static int count=1;
    //random
    private static String CASE_KEY_WORD = "Testcase is '[";
    private static String FILEPATH = System.getProperty("user.dir") + "/raw_data/random_84_2.txt";
    private static String PATH_KEY_WORD = "path =";
    private static String PATH_KEY_WORD2="path =";
    private static String FAILED_KEY_WORD = "Execution is failed!";
    private static String COST_TIME_KEY_WORD = "average_time=";
    private static String SUCCESS_KEY_WORD = "Execution is successful!";
    private static int count = 0;


    private static Map<String, Integer> bottlenecks = new HashMap<>();


    public static void main(String[] args) {
        bottlenecks.put("ts-travel-service", 3);
        bottlenecks.put("ts-assurance-service", 3);
        bottlenecks.put("ts-food-service", 2);
        bottlenecks.put("ts-contacts-service", 2);
        bottlenecks.put("ts-preserve-service", 3);
        bottlenecks.put("ts-order-service", 3);
        bottlenecks.put("ts-inside-payment-service", 2);
        bottlenecks.put("ts-order-other-service", 3);
        boolean flag = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(FILEPATH));
            String line = null;
            int testcaseCount = 0;
            int sumOfcaseCount = 0;
            int failedcaseCount = 0;
            int bottlenecksCount = 0;
            String costtime = "";
            List failedcase = new LinkedList();
            List successpath = new LinkedList();
            Set<String> coverbottlenset = new HashSet<>();
            Set<String> coveredgeset = new HashSet<>();
            Set<String> covernodeset = new HashSet<>();
            String testcase = "";
            while ((line = br.readLine()) != null) {
                String path = "";
                if (line.indexOf(CASE_KEY_WORD) != -1) {
                    sumOfcaseCount += line.split(",").length;
                    testcaseCount++;
                    testcase = line.substring(line.indexOf("'"));

                }
                if (line.indexOf(FAILED_KEY_WORD) != -1) {
                    failedcaseCount++;
                    failedcase.add(testcase);
//                    System.out.println(testcase);
                    Set<String>temp1=new HashSet<>();
                    for (String key : bottlenecks.keySet()) {
                        if (subStrCount(testcase, key) == bottlenecks.get(key)) {
//                            coverbottlenset.add(key);
                            temp1.add(key);
//                            System.out.println(key);
                        }
                    }

                    if(coverbottlenset.size()==0&&temp1.size()!=0){
                        coverbottlenset.addAll(temp1);
                        bottlenecksCount++;
                    }
                     else if(temp1.size()!=0){
                        Set<String> commont=new HashSet<>();
                        commont.addAll(coverbottlenset);
                        commont.retainAll(temp1);
                        if(commont.size()==0){
                            coverbottlenset.addAll(temp1);
                            bottlenecksCount++;
                        }


                    }
//                    System.out.println(testcase);

                }
                if (line.indexOf(SUCCESS_KEY_WORD) != -1) {
                    if (count > 0) {
                        flag = true;
                    }
                    count++;
                }
                if (line.indexOf(PATH_KEY_WORD) != -1||line.indexOf(PATH_KEY_WORD2)!=-1) {
                    String pathtemp = line.substring(line.indexOf("ts"));
                    if (flag) {
                        testcase = testcase.replaceAll("'", "").replaceAll("\\[", "").replaceAll("Crash\\(", "").replaceAll("\\)", "").replaceAll("]", "").replaceAll(" ", "");
                        String checkArr[] = testcase.split(",");
//                        System.out.println(testcase);
                        for (String key : checkArr) {
                            if (pathtemp.indexOf(key) != -1&&!key.equals("")) {
                                System.out.println("ERROR");
                                break;
                            }
                        }
//                        System.out.println(pathtemp);
                    }
                    successpath.add(pathtemp);
                    String arrtemp[] = pathtemp.split("---");
                    for (int i = 0; i < arrtemp.length - 1; i++) {
                        coveredgeset.add(arrtemp[i] + "---" + arrtemp[i + 1]);
                        covernodeset.add(arrtemp[i]);
                        covernodeset.add(arrtemp[i + 1]);
                    }
                }
                if (line.indexOf(COST_TIME_KEY_WORD) != -1) {
                    costtime = df.format(Double.parseDouble(line.split(COST_TIME_KEY_WORD)[1].split("s")[0]) / 60);
                }
            }
            System.out.println("number of fault injections:" + testcaseCount + "\n" +
                    "average number of faults in each fault injection:" + df.format((double) sumOfcaseCount / testcaseCount) + "\n" +
                    "coverage of nodes:" + covernodeset.size() + "\n" +
                    "coverage of edges:" + coveredgeset.size() + "\n" +
                    "coverage of bottlenecks:" + bottlenecksCount + "\n" +
                    "testing cost (minutes)" + costtime);
            System.out.println("failed case count:" + failedcaseCount);
//            System.out.println("failed case:"+failedcase);
            System.out.println("success path:\n" + successpath);
            br.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public static int subStrCount(String s1, String s2) {
        int index, count = 0;
        if (!s1.contains(s2)) {
            return 0;
        }
        index = s1.indexOf(s2);
        while (index != -1) {
            count++;

            index = s1.indexOf(s2, index + 1);//方法一：使用indexOf方法；

            //s1=s1.substring(index+s2.length());//方法二：使用subString方法
            //index=s1.indexOf(s2);
        }
        return count;
    }
}

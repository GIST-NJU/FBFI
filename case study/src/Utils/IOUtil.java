package Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class IOUtil {
    public static String readToString(String fileName) {
        StringBuffer sdf = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = br.readLine()) != null) {
                sdf.append(line+"\n\r");
            }
            br.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return sdf.toString();
    }/**
              * 使用BufferedWriter类写文本文件
              */
    public static void writeMethod3(String fileName,String fileContent){
        try{
            BufferedWriter out=new BufferedWriter(new FileWriter(fileName,true));
            out.write(fileContent);
            out.close();
        } catch (IOException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
              * 使用BufferedWriter类写文本文件
              */
    public static void writeMethod2(String fileName,String fileContent){
        try{
            BufferedWriter out=new BufferedWriter(new FileWriter(fileName,false));
            out.write(fileContent);
            out.close();
        } catch (IOException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 获取该文件夹下所有文件名
     * @param path 文件夹地址
     * @return 返回所有文件名
     */
    public static ArrayList<String> getAllFileName(String path){
        ArrayList<String> listFileName=new ArrayList<>();
        File file = new File(path);
        File [] files = file.listFiles();
        String [] names = file.list();
        if(names != null){
            String [] completNames = new String[names.length];
            for(int i=0;i<names.length;i++){
                completNames[i]=path+names[i];
            }
            listFileName.addAll(Arrays.asList(completNames));
        }
        return listFileName;
    }
    /**
     *删除文件或文件夹
     * @param file
     * @return
     */
   public static boolean delFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
        return file.delete();
    }

    /**
     * 清空某个文件夹
     * @param path
     * @return
     */
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }
    //删除文件夹
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

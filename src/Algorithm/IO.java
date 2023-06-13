package Algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class IO {
	public static List<String> outputs = new ArrayList<>();

	public static void Output(String content) {
		outputs.add(content);
		System.out.println(content);
	}

	public static List<String> Read(String fileaddr) {
		
		List<String> content = new ArrayList<>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileaddr), "GBK"));
			String line = null;
			while ((line = in.readLine()) != null) {
				content.add(line);
			}
			in.close();
		} catch (IOException e) {
		}
		return content;
	}

	public static void Write(String fileaddr, boolean append, List<String> content) {
		
		File file = new File(fileaddr);

		boolean dr = file.getParentFile().mkdirs();
		try {
		    boolean fr = file.createNewFile();
		}catch (Exception e){
		    e.printStackTrace();
		}
		try {
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileaddr, append), "GBK"));
			for (String line : content) {
				out.write(line);
				out.newLine();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		
	}
}

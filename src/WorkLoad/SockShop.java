package WorkLoad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Experiment.Http;
import MainAlgorithm.IO;
import io.jsonwebtoken.Header;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SockShop extends ExperimentSubject {
	String prefix = "prefix";
	String logged_in = "templogged_in";
	String time = new SimpleDateFormat("MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
	Workload[] workloads = {
			new Workload("POST", prefix + "register",
					"{\"username\":\"" + time
							+ "\",\"password\":\"p\",\"email\":\"e\",\"firstName\":\"f\",\"lastName\":\"l\"}",
					"", "logged_in"),
			new Workload("GET", prefix + "catalogue?sort=id&size=3&tags=blue", "", "", "q"),
			new Workload("GET", prefix + "cart", "", "", "q"),
			new Workload("GET", prefix + "customers/" + logged_in, "", "tempCookie", "q"),
			new Workload("GET", prefix + "catalogue/808a2de1-1aaa-4c25-a9b9-6612e8f29a38", "", "", "q"), // Crossed
			new Workload("POST", prefix + "cart", "{\"id\":\"808a2de1-1aaa-4c25-a9b9-6612e8f29a38\"}", "", "q"), // 201
			new Workload("GET", prefix + "cart", "", "", "q"),
			new Workload("POST", prefix + "cards", "{\"longNum\":\"11111\",\"expires\":\"q\",\"ccv\":\"q\"}", "", "q"),
			new Workload("POST", prefix + "addresses",
					"{\"number\":\"\",\"street\":\"\",\"city\":\"\",\"postcode\":\"\",\"country\":\"\"}", "", "q"),
			new Workload("GET", prefix + "cart", "", "", "q"), new Workload("GET", prefix + "card", "", "", "q"),
			new Workload("GET", prefix + "address", "", "", "q"),
			new Workload("GET", prefix + "catalogue/808a2de1-1aaa-4c25-a9b9-6612e8f29a38", "", "", "q"),
			new Workload("GET", prefix + "customers/7WIsB7Yc2Bz2u0xTSP7GL5nzxdyl1NPd", "", "", "q"),
			new Workload("GET", prefix + "cart", "", "", "q"),
			new Workload("GET", prefix + "catalogue/808a2de1-1aaa-4c25-a9b9-6612e8f29a38", "", "", "q"),
			new Workload("POST", prefix + "orders", "{}", "", "q"),
			new Workload("DELETE", prefix + "cart", "", "", "q"), new Workload("GET", prefix + "orders", "", "", "q"),
			new Workload("GET", prefix + "cart", "", "", "q"), };

	public SockShop(String port) {
		this.subject_name = "SockShop";
		this.port = port;
		String[] services = { "user", "catalogue", "catalogue-db", "carts", "carts-db", "orders", "orders-db",
				"payment", "shipping", };

		for (int i = 0; i < services.length; i++)
			this.services.add("sock-shop-" + port + "_" + services[i]);
	}

	@Override
	public boolean GenerateWorkload() throws Exception {
		workloads = new SockShop(port).workloads;
		String Cookie = "";
		for (Workload workload : workloads) {
			workload.URL = workload.URL.replace("templogged_in", logged_in);
			workload.URL = workload.URL.replace("prefix", "http://localhost:" + this.port + "/");
			Http http = new Http(workload.type, workload.URL, workload.parameter, Cookie, "application/json", "",
					workload.needResponse);
			if (http.DoHttp() == false)
				return false;
			if (workload.needResponse.equals("logged_in")) {
				Cookie = http.Cookie;
				// System.out.println("Cookie: " + Cookie);
				logged_in = http.Cookie.split("logged_in=")[1].split(";")[0];
				// System.out.println("logged_in: " + logged_in);
			}
		}
		return true;
	}

	public static void main(String[] args) throws Exception {
		String port = args[0];
		if (port.equals("0"))
			port = "";
		if (args.length == 1)
			System.out.println(new SockShop(port).GenerateWorkload());
		else
			System.out.println(new SockShop(port).GenerateScale(Integer.valueOf(args[1]))
					.replaceAll("sock-shop-" + port + "_", ""));
	}
}

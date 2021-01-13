package FBFI;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import Basic.HttpRequest;
import Basic.SolvingBottleNeckByPath;
import CreateToken.JWT;
import Utils.RemoteExecuteCommand;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
public class Main {
	//远程调用
	public static RemoteExecuteCommand rec = new RemoteExecuteCommand("47.114.159.122", "root", "15150650719aB");
	//train-ticket ip
	private static String ipaddress="47.114.159.122:32677";
	//追踪ip
	private static String jaegerIP="47.114.159.122:32688";
	//当前时间
	public String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
	//服务名前缀
	public static String[] services = {
			"ts-travel-service",
			"ts-assurance-service",
			"ts-food-service",
			"ts-contacts-service",
			"ts-preserve-service",
			"ts-order-service",
			"ts-inside-payment-service",
			"ts-order-other-service"
			};
	//初始pod实例个数
	public static int []scaleNum={3,3,2,2,3,3,2,3};
	//当前pod实例个数
	public static Map<String,Integer> scaleMap=new HashMap<>();

	public Set<String> InsertServices = new HashSet<>();

	public static Map<String,Map<String,String>> serviceNameForAlg=new HashMap<>();
	public static Map<String,Map<String,String>> serviceNameForAlg2=new HashMap<>();


	public  void init (){
		System.out.println("begin init...");
		long time1 = System.currentTimeMillis() ;
		scaleMap=new HashMap<>();
		scaleNum= new int[]{3, 3, 2, 2, 3, 3, 2, 3};
		for (int i = 0; i <services.length ; i++) {
			scaleMap.put(services[i],scaleNum[i]);
			if(rec.executeSuccess("kubectl scale deployment "+services[i]+" --replicas="+scaleNum[i]).equals("")){
				throw new RuntimeException("init error");
			}
		}
		String initresult=rec.execute("kubectl get pods -o wide |grep 'ts-travel-service\\|ts-assurance-service\\|ts-food-service\\|ts-contacts-service\\|ts-preserve-service" +
				"\\|ts-order-service\\|ts-inside-payment-service\\|ts-order-other-service\\|jaeger'");
		try {
			while (initresult.indexOf("0/1") != -1) {
				Thread.sleep(30 * 1000);
				 initresult=rec.execute("kubectl get pods -o wide |grep 'ts-travel-service\\|ts-assurance-service\\|ts-food-service\\|ts-contacts-service\\|ts-preserve-service" +
						"\\|ts-order-service\\|ts-inside-payment-service\\|ts-order-other-service\\|jaeger'");
			}

			for (int i = 0; i <services.length ; i++) {
				String temp[]=rec.execute("kubectl get pods -o wide |grep "+services[i]+" |awk {'print $1'}").split("\n");
				Map<String,String> map=new HashMap<>();
				Map<String,String> map2=new HashMap<>();
				for (int j = 0; j <temp.length ; j++) {
					map.put(temp[j],services[i]+"_"+(j+1));
					map2.put(services[i]+"_"+(j+1),temp[j]);
				}
				serviceNameForAlg.put(services[i],map);
				serviceNameForAlg2.put(services[i],map2);
			}
			long time2 = System.currentTimeMillis() ;
			System.out.println("init end, cost time "+(time2-time1)/1000+"s");
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		long time1 = System.currentTimeMillis();
		Main m = new Main();
		SolvingBottleNeckByPath sb = new SolvingBottleNeckByPath(0);
		//System.out.println(m.Execution(args[0]));
		//m.ExecutionPath();
		Set<Set<String>> TestcaseSet = new HashSet<>();	//测试用例集
		TestcaseSet.add(new HashSet<>());
		Set<Set<String>> FailcaseSet = new HashSet<>();		//已测用例集
		int count = 1;
		int testcaseLength=0;
		while(true){
			JWT jwt = new JWT();
			String auth = jwt.createToken();
			////////////选取测试用例//////////////
			System.out.println("-----------" + count + "------------FailcaseSet.size(): " + FailcaseSet.size() + ", TestcaseSet.size(): " + TestcaseSet.size());
			System.out.println("testcasseset:");
			for(Set<String> tc:TestcaseSet){
				for(String temp:tc){
					System.out.println(temp);
				}
				System.out.println();
			}
			Set<String> testcase = new HashSet<>();
			boolean tag = false;
			for(Set<String> tc:TestcaseSet){
				if(!FailcaseSet.contains(tc)){
					testcase = tc;
					tag = true;
					break;
				}
			}
			if(!tag)
				break;
			///恢复系统正常
			System.out.println("Reset system...");
			m.init();
//			m.RestoreFaults(testcaseLength);
//			testcaseLength=testcase.size();
			/////////测试用例注入////////////
			System.out.println("Insert faults is " + testcase);
			m.InsertFaults(testcase);
			if(m.Execution(auth)){
				System.out.println("Execution is successful!");
				Thread.sleep(60*1000);
				String path = m.ExecutionPath();		//追踪调用链
				System.out.println("\npath = " + path+'\n');
				sb.AddPath(path);
				TestcaseSet = sb.getCases();
				System.out.println("TestcaseSum = " + TestcaseSet.size());
			}else{
				System.out.println("Execution is failed!");
				FailcaseSet.add(testcase);
			}
			count++;
		}
		long time2 = System.currentTimeMillis() ;
		System. out.println("Total time is " + (time2 - time1)/1000 +"s");
		//restart system
		m.init();
	}

	public void InsertFaults(Set<String> testcase){
		try {
			System.out.println("Testcase is '" + testcase + "'");
			System.out.println("Insert faults...");
			InsertServices = new HashSet<>();
			for(String serviceInstance : testcase){
				if(serviceInstance.startsWith("Crash"))
					serviceInstance = serviceInstance.substring(6,serviceInstance.length() - 1);
				//String ser = serviceInstance.substring(6,serviceInstance.length() - 1);
				for(String str:services){
					if(serviceInstance.indexOf(str) > 0){
						InsertServices.add(str);
						break;
					}
				}
				String currentDeployService=serviceInstance.split("service")[0]+"service";
				String res=rec.execute("kubectl delete pod "+serviceNameForAlg2.get(currentDeployService).get(serviceInstance))+rec.execute("kubectl scale deployment "+currentDeployService+" --replicas="+(scaleMap.get(currentDeployService)-1));
				scaleMap.put(currentDeployService,scaleMap.get(currentDeployService)-1);
				System.out.println("delete info:"+res);
				Thread.sleep(10*1000);
			    System.out.println("stop " + serviceInstance + " success");
			}
		} catch (Exception e) {
			 System.out.println("Insert faults failed");
		}
		
	}
	public void RestoreFaults(int testcaseLength){
		try {
			/*for(String service : InsertServices){
				Process ps = Runtime.getRuntime().exec("docker-compose -f deployment/quickstart-docker-compose/quickstart-docker-compose.yml start " + service);
				BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			    String line = null;
			    while ((line = br.readLine()) != null) {
			    	 //System.out.println(line);
			    }
			    Thread.sleep(40*1000);
			    System.out.println("restart " + service + " success");
			}*/
			/*Process ps = Runtime.getRuntime().exec("../Desktop/restartTrainWeb.sh");

			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
		    String line = null;
		    while ((line = br.readLine()) != null) {
		    	System.out.println(line);
		    	System.out.println(line.length());
		    	System.out.println(ps.exitValue());
		    }*/
//			Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "cd ../train-ticket" }, null);
//			Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "sh restartTrainWeb.sh" }, null);
			System.out.println("testcaseLength=" + testcaseLength);
			System.out.println("cost"+(testcaseLength+2)*5+"s");
			Thread.sleep((testcaseLength+2)*5*1000);
			System.out.println("restart success");
		} catch (Exception e) {
			System.out.println("Reset system failed");
		}
	}
	public boolean Execution(String auth) throws Exception{
		boolean tag = true;
		HttpRequest request = new HttpRequest(auth);
		String url;
		String para;
		
		
		//travel service
		url = "http://"+ipaddress+"/api/v1/travelservice/trips/left";
		para = "{\"departureTime\":\""+date+"\", \"endPlace\":\"Su Zhou\", \"startingPlace\":\"Shang Hai\"}";
		request.doPost(url,para);
		if(request.ResponseCode != 200){
			System.out.println("travelservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("travel service is true");
		Thread.sleep(500);
		//assurance service
		url = "http://"+ipaddress+"/api/v1/assuranceservice/assurances/types";
		request.doGet(url);
		if(request.ResponseCode != 200){
			System.out.println("assuranceservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("assurance service is true");
		Thread.sleep(500);
		//	System.out.println(request.response);
		//food service
		url = "http://"+ipaddress+"/api/v1/foodservice/foods/"+date+"/Shang%20Hai/Su%20Zhou/D1345";
		request.doGet(url);
		if(request.ResponseCode != 200){
			System.out.println("foodservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("food service is true");
		Thread.sleep(500);
		//contact service
		url = "http://"+ipaddress+"/api/v1/contactservice/contacts/account/4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";
		request.doGet(url);
		if(request.ResponseCode != 200){
			System.out.println("contactservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("contact service is true");
		Thread.sleep(500);
		//preserve service
		url = "http://"+ipaddress+"/api/v1/preserveservice/preserve";
		para = "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\",\"contactsId\":\"d314c5ee-cc71-4da0-afa5-bcf33b96cc89\",\"tripId\":\"D1345\",\"seatType\":\"2\",\"date\":\""+date+"\",\"from\":\"Shang Hai\",\"to\":\"Su Zhou\",\"assurance\":\"0\",\"foodType\":1,\"foodName\":\"Bone Soup\",\"foodPrice\":2.5,\"stationName\":\"\",\"storeName\":\"\"}";
		request.doPost(url,para);
		if(request.ResponseCode != 200){
			System.out.println("preserveservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("preserve service is true");
		Thread.sleep(500);
		//order service
		String orderid = "";
		url = "http://"+ipaddress+"/api/v1/orderservice/order/refresh";
		para = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}";
		request.doPost(url,para);
		if(request.ResponseCode != 200){
			System.out.println("orderservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}else{
			//获取
			JSONObject response = JSONObject.fromObject(request.response);

			if(response.containsKey("data")){
				JSONArray orders = JSONArray.fromObject(response.get("data"));
				JSONObject latestorder = orders.getJSONObject(orders.size() - 1);
				//System.out.println(latestorder);
				orderid = (String)latestorder.get("id");
				// System.out.println(latestorder.get("id"));
			}

		}
		System.out.println("order service is true");
		Thread.sleep(500);
		//order other service
		url = "http://"+ipaddress+"/api/v1/orderOtherService/orderOther/refresh";
		para = "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}";
		request.doPost(url,para);
		if(request.ResponseCode != 200){
			System.out.println("orderotherservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("order other service is true");
		Thread.sleep(500);
		//inside_pay_service
		url = "http://"+ipaddress+"/api/v1/inside_pay_service/inside_payment";
		para = "{\"orderId\":\"" + orderid + "\",\"tripId\":\"D1345\"}";
		//System.out.println(para);
		request.doPost(url,para);
		if(request.ResponseCode != 200){
			System.out.println("insidepaymentservice failed,resondeCode is " + request.ResponseCode + ",response is " + request.response);
			return false;
		}
		System.out.println("inside payment service is true");
		Thread.sleep(500);
		return tag;
	}
	public String getServiceValue(String service) throws Exception{
		HttpRequest request = new HttpRequest("");
		//request.doGet("http://"+ipaddress+"/api/v1/assuranceservice/assurances/types");
		long end = System.currentTimeMillis();
		long start = end - 3600000;
		String url = "http://"+jaegerIP+"/api/traces?end="+(end*1000)+"&limit=1&lookback=1h&maxDuration&minDuration&service=" + service + "&start="+(start*1000);
		//System.out.println("URL: "+url);
		request.doGet(url);
		Thread.sleep(500);
		System.out.println("------" + service + "--------");
		JSONObject processes = JSONObject.fromObject(request.response).getJSONArray("data").getJSONObject(0).getJSONObject("processes");
		System.out.println("processes.size = " + processes.size());
		for(int i=1;i<=processes.size();i++) {
			JSONObject p=processes.getJSONObject("p"+Integer.toString(i));
			if(p.get("serviceName").equals(service)){
				String result=p.getJSONArray("tags").getJSONObject(0).getString("value");
				System.out.println("Matched in p"+i+", Value= "+result);
				return result;
			}
		}
		return "";
	}
	public String ExecutionPath() throws Exception{
		String path = "";
		for(String service:services){
			String value = getServiceValue(service);
			path += serviceNameForAlg.get(service).get(value) + "---";
		}
		path = path.substring(0, path.length() - 3);
		//System.out.println(path);
		return path;
	}




}
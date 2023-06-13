package Subject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import Algorithm.IO;
import Main.Http;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class TrainTicket extends ExperimentSubject {
	String dashboard_port = "32677";
	String prefix = "http://localhost:" + dashboard_port + "/api/v1/";
	String accountID = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";
	String contactsId = "tempContactsId";
	String orderId = "tempOrderId";
	String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

	Workload[] workloads = { new Workload("POST", prefix + "travelservice/trips/left",
			"{\"departureTime\":\"" + date + "\", \"endPlace\":\"Su Zhou\", \"startingPlace\":\"Shang Hai\"}", "", ""),
			new Workload("GET", prefix + "assuranceservice/assurances/types", "", "", ""),
			new Workload("GET", prefix + "foodservice/foods/" + date + "/Shang%20Hai/Su%20Zhou/D1345", "", "", ""),
			new Workload("GET", prefix + "contactservice/contacts/account/" + accountID, "", "", "contactsId"),
			new Workload("POST", prefix + "preserveservice/preserve", "{\"accountId\":\"" + accountID
					+ "\",\"contactsId\":\"" + contactsId + "\",\"tripId\":\"D1345\",\"seatType\":\"2\",\"date\":\""
					+ date
					+ "\",\"from\":\"Shang Hai\",\"to\":\"Su Zhou\",\"assurance\":\"0\",\"foodType\":1,\"foodName\":\"Bone Soup\",\"foodPrice\":2.5,\"stationName\":\"\",\"storeName\":\"\"}",
					"", ""),
			new Workload("POST", prefix + "orderservice/order/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", "orderId"),
			new Workload("POST", prefix + "orderOtherService/orderOther/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("GET", prefix + "consignservice/consigns/order/" + orderId, "", "", ""),
			new Workload("GET", prefix + "cancelservice/cancel/refound/" + orderId, "", "", ""),
			new Workload("POST", prefix + "inside_pay_service/inside_payment",
					"{\"orderId\":\"" + orderId + "\",\"tripId\":\"D1345\"}", "", ""),
			new Workload("POST", prefix + "orderservice/order/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("POST", prefix + "orderOtherService/orderOther/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("POST", prefix + "travel2service/trips/left",
					"{\"departureTime\":\"" + date + "\", \"endPlace\":\"Su Zhou\", \"startingPlace\":\"Shang Hai\"}",
					"", ""),
			new Workload("POST", prefix + "travelservice/trips/left",
					"{\"departureTime\":\"" + date + "\", \"endPlace\":\"Su Zhou\", \"startingPlace\":\"Shang Hai\"}",
					"", ""),
			new Workload("POST", prefix + "rebookservice/rebook",
					"{\"date\":\"" + date + "\", \"oldTripId\":\"D1345\", \"orderId\":\"" + orderId
							+ "\", \"seatType\":2,\"tripId\":\"D1345\"}",
					"", ""),
			new Workload("POST", prefix + "travelplanservice/travelPlan/cheapest",
					"{\"departureTime\":\"" + date + "\", \"endPlace\":\"Shang Hai\", \"startingPlace\":\"Nan Jing\"}",
					"", ""),
			new Workload("POST", prefix + "orderservice/order/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"application/json", ""),
			new Workload("POST", prefix + "orderOtherService/orderOther/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("GET", prefix + "executeservice/execute/collected/" + orderId, "", "", ""),
			new Workload("POST", prefix + "orderservice/order/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("POST", prefix + "orderOtherService/orderOther/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("GET", prefix + "executeservice/execute/execute/" + orderId, "", "", ""),
			new Workload("POST", prefix + "orderservice/order/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("POST", prefix + "orderOtherService/orderOther/refresh", "{\"loginId\":\"" + accountID
					+ "\",\"enableStateQuery\":false,\"enableTravelDateQuery\":false,\"enableBoughtDateQuery\":false,\"travelDateStart\":null,\"travelDateEnd\":null,\"boughtDateStart\":null,\"boughtDateEnd\":null}",
					"", ""),
			new Workload("POST", "http://localhost:" + dashboard_port + "/getVoucher",
					"{\"orderId\":\"" + orderId + "\", \"type\":1}", "application/json", ""),
			new Workload("POST", prefix + "users/login", "{\"password\":\"222222\", \"username\":\"admin\"}", "",
					"token"),
			new Workload("GET", prefix + "adminorderservice/adminorder", "", "", ""),
			new Workload("GET", prefix + "adminrouteservice/adminroute", "", "", ""),
			new Workload("GET", prefix + "admintravelservice/admintravel", "", "", ""),
			new Workload("GET", prefix + "adminuserservice/users", "", "", ""),
			new Workload("GET", prefix + "adminbasicservice/adminbasic/contacts", "", "", ""),
			new Workload("GET", prefix + "adminbasicservice/adminbasic/stations", "", "", ""),
			new Workload("GET", prefix + "adminbasicservice/adminbasic/trains", "", "", ""),
			new Workload("GET", prefix + "adminbasicservice/adminbasic/prices", "", "", ""),
			new Workload("GET", prefix + "adminbasicservice/adminbasic/configs", "", "", ""), };

	public TrainTicket() {
		this.subject_name="TrainTicket";
		this.jaeger_port = "16686";
		this.jaeger_service = "train-ticket_jaeger_1";
		String[] services = { "ts-ticketinfo-service", "ts-assurance-service", "ts-inside-payment-service",
				"ts-rebook-service", "ts-travel-plan-service", "ts-admin-order-service", "ts-admin-route-service",
				"ts-price-service", "ts-contacts-service", "ts-cancel-service", "ts-payment-service",
				"ts-basic-service", "ts-consign-service", "ts-route-plan-service", "ts-seat-service",
				"ts-security-service", "ts-admin-user-service", "ts-route-service", "ts-order-other-service",
				"ts-user-service", "ts-execute-service", "ts-admin-travel-service", "ts-station-service",
				"ts-travel-service", "ts-train-service", "ts-food-service", "ts-preserve-service", "ts-travel2-service",
				"ts-config-service", "ts-food-map-service", "ts-order-service", "ts-notification-service",
				"ts-auth-service", "ts-admin-basic-info-service" };
		for (int i = 0; i < services.length; i++)
			this.services.add(services[i]);
	}

	String GetToken() {
		Claims claims = Jwts.claims().setSubject("fdse_microservice");
		claims.put("roles", new HashSet<>(Arrays.asList("ROLE_USER")));
		claims.put("id", UUID.fromString(accountID));
		Date now = new Date();
		Date validate = new Date(now.getTime() + 60 * 60 * 1000);
		String secretKey = Base64.getEncoder().encodeToString("secret".getBytes());
		return Jwts.builder().setClaims(claims).setIssuedAt(now).setExpiration(validate)
				.signWith(SignatureAlgorithm.HS256, secretKey).compact();
	}

	@Override
	public boolean GenerateWorkload() throws Exception {
		workloads = new TrainTicket().workloads;
		String token = GetToken();
		for (Workload workload : workloads) {
			workload.URL = workload.URL.replace("tempContactsId", contactsId);
			workload.parameter = workload.parameter.replace("tempContactsId", contactsId);
			workload.URL = workload.URL.replace("tempOrderId", orderId);
			workload.parameter = workload.parameter.replace("tempOrderId", orderId);
			if (firstTime)
				System.out.println(workload.type + ":" + workload.URL);
			Http http = new Http(workload.type, workload.URL, workload.parameter, workload.Cookie, "application/json",
					token, workload.needResponse);

			if (http.DoHttp() == false) {
				IO.Output("[ Workload Failed ]");
				return false;
			}
			JSONObject response = null;
			if (!workload.needResponse.isEmpty())
				response = JSONObject.fromObject(http.response_string);
			switch (workload.needResponse) {

			case "contactsId":
				contactsId = (String) JSONArray.fromObject(response.get("data")).getJSONObject(0).get("id");
				// System.out.println("contactsId: " + contactsId);
				break;
			case "orderId":
				JSONArray orders = JSONArray.fromObject(response.get("data"));
				JSONObject latestorder = orders.getJSONObject(orders.size() - 1);
				orderId = (String) latestorder.get("id");
				// System.out.println("orderId: " + orderId);
				break;
			case "token":
				token = response.getJSONObject("data").getString("token");
				// System.out.println("authToken: " + token);
				break;
			default:
				break;
			}
			if (firstTime)
				Thread.sleep(3000);
		}
		firstTime = false;
		IO.Output("[ Workload Success ]");
		return true;
	}

	public static void main(String[] args) throws Exception {

		if (args.length == 0)
			System.out.println(new TrainTicket().GenerateWorkload());
		else
			System.out
					.println(new TrainTicket().GenerateScale(Integer.valueOf(args[0]),"./scale.txt").replaceAll("train-ticket_", ""));
	}

}
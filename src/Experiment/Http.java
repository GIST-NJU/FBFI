package Experiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import MainAlgorithm.IO;
import net.sf.json.JSONObject;

public class Http {
	HttpURLConnection connection = null;
	public String type = "";
	public String URL = "";
	public String parameter = "";
	public String logged_in = "";
	public String Cookie = "";
	public String contentType = "";
	public String authToken = "";
	public String needResponse = "";
	public String response_string = "";

	public Http() {
	};

	public Http(String type, String URL, String parameter, String Cookie, String contentType, String authToken,
			String needResponse) {
		this.type = type;
		this.URL = URL;
		this.parameter = parameter;
		this.Cookie = Cookie;
		this.contentType = contentType;
		this.authToken = authToken;
		this.needResponse = needResponse;
	};

	public boolean DoHttp() {
		OutputStreamWriter writer = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(URL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(type);
			connection.setConnectTimeout(120 * 1000);
			connection.setReadTimeout(60 * 1000);
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("Content-Type", contentType);
			if (!Cookie.isEmpty())
				connection.setRequestProperty("Cookie", Cookie);
			if (!authToken.isEmpty())
				connection.addRequestProperty("Authorization", "Bearer " + authToken);
			switch (type) {
			case "POST":
				connection.setDoOutput(true);
				connection.setDoInput(true);
				writer = new OutputStreamWriter(connection.getOutputStream());
				writer.write(parameter);
				writer.flush();
				writer.close();
				break;
			case "GET":
				break;
			case "DELETE":
				connection.setDoOutput(true);
				connection.setRequestMethod("DELETE");
				break;
			}
			int ResponseCode = connection.getResponseCode();
			List<String> CookieList = connection.getHeaderFields().get("Set-Cookie");
			Cookie = StringUtils.join(CookieList, "; ");
			if (ResponseCode < 400) {
				if (!needResponse.equals("")) {
					reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
					String line;
					while ((line = reader.readLine()) != null) {
						response_string += line;
					}
					//System.out.println("Response_String:\n" + response_string);
					if (response_string.contains("\"status_code\":500")) {
						IO.Output("Got an error status_code: 500 when " + type + " " + URL);
						if (type.equals("POST"))
							IO.Output("Parameter is: " + parameter);
						return false;
					}
				}
				return true;
			} else {
				IO.Output("Got an error code: " + ResponseCode + " when " + type + " " + URL);
				if (type.equals("POST"))
					IO.Output("Parameter is: " + parameter);
				return false;
			}
		} catch (Exception e) {
			IO.Output("Got an exception" + " when " + type + " " + URL);
			System.err.println(e);
			if (type.equals("POST"))
				IO.Output("Parameter is: " + parameter);
			return false;
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			connection.disconnect();
		}
	}
}

package WorkLoad;

public class Workload {
	String type;
	String URL;
	String parameter;
	String Cookie;
	String needResponse;

	Workload(String type, String URL, String parameter, String Cookie, String needResponse) {
		this.type = type;
		this.URL = URL;
		this.parameter = parameter;
		this.Cookie = Cookie;
		this.needResponse = needResponse;
	}

	public static void main(String[] args) throws Exception {
		System.out.println(System.getProperty("java.library.path"));
	}
}

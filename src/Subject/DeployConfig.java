package Subject;

import java.util.HashMap;
import java.util.Map;

public class DeployConfig {
	static Map<Integer, String> Scale;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Scale = new HashMap<Integer, String>() {
			{
				put(1, "small");
				put(2, "medium");
				put(3, "large");
			}
		};
		for (int i = 1; i <= 30; i++) {
			for (int j = 1; j <=3; j++) {
				new SockShop().GenerateScale(j, "../Data/SockShop/" + Scale.get(j) + "/" + i + "/scale" + ".txt");
				new TrainTicket().GenerateScale(j, "../Data/TrainTicket/" + Scale.get(j) + "/" + i + "/scale" + ".txt");
			}
		}
	}
}

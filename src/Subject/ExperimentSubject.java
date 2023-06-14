package Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Algorithm.IO;
import Main.Experiment_Exception;

public abstract class ExperimentSubject {
	public String subject_name;
	public String port;
	public String jaeger_port;
	public String jaeger_service;

	public List<String> services = new ArrayList<>();
	public boolean firstTime = true;

	ExperimentSubject() {
	}

	ExperimentSubject(String port) {
		this.port = port;
	}

	public static void Execute_Command(String command) throws Exception {
		System.out.println(command);
		Process process = Runtime.getRuntime().exec(new String[] { "sh", "-c", command });
		if (process.waitFor() == 1)
			throw new Experiment_Exception("< Error: Execute command [" + command + "] failed! >");
	}

	public String GenerateScale(int scale, String fileaddr) {
		String scale_command = "docker-compose up";
		Random random = new Random();
		List<String> scale_list = new ArrayList<>();
		if (scale == 0) {
			List<String> content = IO.Read(fileaddr);
			for (String c : content) {
				scale_command += " --scale " + c.replaceAll(" ", "");
			}
		} else {
			for (String service : services) {
				int random_scale = random.nextInt(2) + scale;
				scale_list.add(service + " = " + random_scale);
				scale_command += " --scale " + service + "=" + String.valueOf(random_scale);
				IO.Write(fileaddr, false, scale_list);
			}
		}
		return scale_command;
	}

	public boolean Deploy(String fileaddr) throws Exception {
		System.out.println("\nCleaning Docker Environment");
		Execute_Command("docker-compose down --volumes");
		System.out.println("\nDocker Environment Cleaned");
		String deploy = GenerateScale(0, fileaddr);
		System.out.println("\nDeploy Scale:\n" + deploy);
		Runtime.getRuntime().exec(deploy);
		System.out.println("\nDeploy Start, please wait for about 300s ...\n");
		Thread.sleep(300 * 1000);
		for (int i = 0; i < 10; i++) {
			if (GenerateWorkload()) {
				System.out.println("\nDeploy Finish\n");
				return true;
			}
			Thread.sleep(60 * 1000);
		}
		System.out.println("\nDeploy Failed!\n");
		return false;
	}

	public void ResetJaeger() throws Exception {
		System.out.println("\nReset Jaeger");
		Execute_Command("docker restart " + jaeger_service);
		Thread.sleep(30 * 1000);
	}

	public void ResetDashboard(boolean dashboard) throws Exception {
		if (dashboard) {
			System.out.println("\nReset Dashboard");
			Execute_Command("docker restart train-ticket_ts-ui-dashboard_1");
		} else {
			System.out.println("\nReset Frontend");
			Execute_Command("docker restart sock-shop-" + port + "_edge-router_1 sock-shop-" + port + "_front-end_1");
		}

	}

	public void ResetSubject() throws Exception {
		System.out.println("\nReset Subject");
		Execute_Command("systemctl restart docker");
	}

	public void Inject(Set<String> config, String inject_type, boolean split) throws Exception {
		if (config.isEmpty())
			return;
		switch (inject_type) {
		case "stop":
		case "pause":
			String inject_command = "docker " + inject_type;
			for (String node : config)
				if (split)
					inject_command += " " + node.split("~~")[1];
				else
					inject_command += " " + node;
			Execute_Command(inject_command);
			break;
		case "net":
			for (String node : config)
				Execute_Command("nsenter -n -t $(docker inspect -f {{.State.Pid}} " + node.split("~~")[1]
						+ ") iptables -A INPUT -p tcp -j DROP");
			break;
		}
		Thread.sleep(20 * 1000);
	}

	public void Restore(Set<String> config, String inject_type, boolean split) throws Exception {
		if (config.isEmpty())
			return;
		String inject_command = "";
		switch (inject_type) {
		case "stop":
			inject_command = "docker start";
			break;
		case "pause":
			inject_command = "docker unpause";
			break;
		case "restart":
			inject_command = "docker restart";
			break;
		case "net":
			for (String node : config)
				Execute_Command("nsenter -n -t $(docker inspect -f {{.State.Pid}} " + node.split("~~")[1]
						+ ") iptables -D INPUT 1");
			return;
		}
		for (String node : config)
			if (split)
				inject_command += " " + node.split("~~")[1];
			else
				inject_command += " " + node;
		Execute_Command(inject_command);
		Thread.sleep(20 * 1000);
	}

	public abstract boolean GenerateWorkload() throws Exception;
}

package Main;

import Algorithm.IO;

@SuppressWarnings("serial")
public class Experiment_Exception extends Exception {
	public Experiment_Exception(String error) {
		super();
		IO.Output(error);
	}
}

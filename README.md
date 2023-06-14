# FBFI

FBFI (Fault-tolerance Bottleneck driven Fault Injection) is a fault injection testing (FIT) approach to the effective and efficient validation of redundant components deployed in a service system. It uses the concept of **fault-tolerance bottleneck** to repeatedly generate fault injection configurations, and does not rely on *a priori*, complete knowledge of the system's business structure.

### Usage

To use FBFI to test a system, the tester should have a *system workload* that exercises a certain number of services, a *fault injector* that implements the concrete fault injections, and a *tracing tool* that captures the execution paths of the system. Given such an application scenario, FBFI will take the system execution results observed as the input (either a successful execution path, or a system failure observed), and infer fault-tolerance bottlenecks as the locations of fault injection.

#### Usage for simulation
We currently provide a command-line utility that implements the core algorithms of the FBFI (with `algorithm=FBFI`). In addition, the `algorithm` parameter can also be configured to `Random` or `LDFI`, in order to use different strategies to generate fault injection configurations.

```bash
java -jar Main.jar algorithm=[Algorithm_Name]
```
##### Main process of FBFI

At first, since FBFI is unaware of the complete business structure, the tester should provide an initial successful execution path, and FBFI will use this path as the initial business structure. Here, each *execution path* should be encoded as `x_1-x_2-...-x_n`, where `x_i` is a string that indicates the name of a particular business node of the *i*-th service.

```
Please input a successful execution path (e.g. A_1-B_1-C_1):
A_1-B_1-C_1
```

FBFI will then update the business structure, and generate its fault-tolerance bottlenecks. The tester should use the given bottleneck to implement concrete fault injections and trace the system execution. At this moment, if an alternative execution path is obtained, this path should be given to FBFI for structure update. Otherwise, the input `-` should be given to FBFI. Note that the system should be reset (removes faults injected) after this step.

```
Please inject config: [A_1]
...

Please input the new execution path (if the system works correctly), 
or "-" (if the system fails in all cases), 
or "stop" (if you want to stop the test process early):
A_2-B_1-C_2
Path: [A_2, B_1, C_2]
Please restore config: [A_1]
```
FBFI will repeat the above process until all bottlenecks inferred can break the system. At this moment, FBFI has explored the complete business structure, and the algorithm terminates.

```
~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Graph ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
[ Layer 1: A ]
LayerSize: 2
LayerNodes: [A_1, A_2]

NodeID: A_1
Layer: 1
SubNodes: B_2 B_1 
PreNodes: start 

NodeID: A_2
Layer: 1
SubNodes: B_2 B_1 B_3 
PreNodes: start 

[ Layer 2: B ]
LayerSize: 3
LayerNodes: [B_2, B_1, B_3]

NodeID: B_2
Layer: 2
SubNodes: C_1 
PreNodes: A_1 A_2 

NodeID: B_1
Layer: 2
SubNodes: C_1 C_2 
PreNodes: A_1 A_2 

NodeID: B_3
Layer: 2
SubNodes: C_1 C_2 
PreNodes: A_2 

[ Layer 3: C ]
LayerSize: 2
LayerNodes: [C_1, C_2]

NodeID: C_1
Layer: 3
SubNodes: end 
PreNodes: B_2 B_1 B_3 

NodeID: C_2
Layer: 3
SubNodes: end 
PreNodes: B_1 B_3 
~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

Bottlenecks: 
[[A_1, A_2], [B_2, B_1, B_3], [C_1, C_2], [C_1, B_1, B_3], [C_1, B_1, A_2], [B_2, B_1, A_2]]
bottleneckNumber = 6

Defects: 
[]
defectNumber = 0 
...
```

##### The random and LDFI approaches 

Like FBFI, the random and LDFI approaches are also unaware of the complete business structure, and will rely on only the successful execution paths to update the business structure. The main difference between these approaches is in the generation of fault injection configurations: the random approach generates fault injection configurations randomly, while LDFI uses a SAT solver to infer bottlenecks.

#### Usage by interface

We currently provide an interface of FBFI algorithm. To use the FBFI algorithm in your code, you need to implement the method `genPath(Set<String> config)` in `Main.FBFIUsage` first.  You can use the method `getInjectConfig()` to get the configuration that needs to be injected into the SUT. After injecting the fault, you need to execute the SUT workload and get the new execution path as the output of the method `genPath()`. After implementing the method `genPath()`, you can use the method `RunFBFIExperiment()` to perform the FBFI algorithm in your code. `FBFIExample.java` shows an example of the usage of FBFI algorithm by interface `Main.FBFIUsage`.

```java
import Main.FBFIUsage;
		FBFIUsage FBFI = new FBFIUsage() {
			@Override
			/**
			 * @param config The configuration that needs to be injected into the SUT
			 * @return The execution path with the following form: {"A_1", "B_1", "C_1"}, 
			 *          or an empty list if the SUT workload execution failed.
			 */
			public List<String> genPath(Set<String> config) throws Exception {
				List<String> path = new ArrayList<String>();
				// TODO Your code may like this:
				// Inject(config);
				// path = Tracer();
				// Restore(config);
				return path;
			}
		};
		FBFI.RunFBFIExperiment();
```

### Experiment

The performance of FBFI is evaluated under two microservice benchmark systems, [TrainTicket](https://github.com/FudanSELab/train-ticket/tree/jaeger) and [SockShop](https://github.com/microservices-demo/microservices-demo). Two additional automated FIT approaches, Radnom and LDFI (an adapted version), are used for comparison.

To reproduce the experimental results, first run the following command to generate a series of configurations for deploying the benchmark system under different scales (note that the default deployment configuration of benchmark system does not introduce redundancy to service instances):

```bash
java -jar DeployConfig.jar
```

Executing this command will generate the following series of files:

``../Data/[Subject]/[Scale]/[id]/scale.txt``

Here, `[Subject]` can take `TrainTicket` or `SockShop`, `[Scale]` indicates the deployment scale of the system, which can take `small`, `medium` or `large` (that is, each service has `{1, 2}`, `{2, 3}`, or `{3, 4}` redundant nodes), and `[id]`can take from 1 to 30. That is to say, a total of 2\*3\*30=180 `scale.txt` files were generated. For example, the content form of the `scale.txt` may be as follows

```
ts-ticketinfo-service = 4
ts-seat-service = 3
```

Next, for the deployment configuration generated, run the following command to deploy the SUT and carry out the experiment. Here, the `subject` parameter indicates the address of deployment configuration file `scale.txt` (e.g. subject=../Data/TrainTicket/large/26). 

```bash
java -jar Main.jar subject=[Deployment Configuration Addr] algorithm=[Algorithm Name] time=[Time Limit]
```
Take the deployment file above as an example, the program will first automatically execute the following code to deploy the SUT.

```bash
docker-compose up --scale ts-ticketinfo-service=4 --scale ts-seat-service=3
```

After the successful deployment of the benchmark system, the FIT approach selected will then be invoked to automatically perform fault injection experiments. The `algorithm` parameter indicates the FIT approach that will be used, which can take `FBFI`, `LDFI` or `Random`. The `algorithm` parameter can be defaulted, which means that the three FIT approaches, `FBFI`, `LDFI` and `Random`, will be executed in turn. Note that according to the experiment setting, FBFI should be executed first, because the maximum execution time constraints allocated to the random and LDFI approaches are dependent on the time costs of FBFI (1x and 3x of FBFI, respectively). If you want to execute LDFI or Random alone, you can specify the parameter `time` to determine the upper limit of running time (unit: seconds, default value: 3600).
Once the experiment terminates, the following data will be reported:

* `Total Graph`: the whole business structure explored by algorithm
* `Bottlenecks`: the set of fault-tolerance bottlenecks identified by algorithm
* `Defects`: the set of defects identified by algorithm
* `Flaky Defects`: the set of flaky failure-causing configurations identified by algorithm
* `Node Coverage`: number of covered business nodes / total number of business nodes
* `Edge Coverage`: number of covered message transmissions / total number of message transmissions of the corresponding symmetric structure
* `Path Number`: the number of execution paths
* `Inject Number`: the number of fault injection configurations
* `Total Time`:  the total cost of experiment (Seconds)
* `HandleConfig Time`:  the injection cost of experiment, including the time cost of implementing injections and resetting the system (Seconds)
* `Algorithm Time`:  the generation cost of the algorithm (Seconds)

The `Data` directory gives the raw experimental data observed in our experiment. In the directory of each deployment configuration file `scale.txt`, we provide:

* `outputs_[Algorithm].txt`: the raw experiment results of running each algorithm under this deployment configuration
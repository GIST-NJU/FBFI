# FBFI

FBFI (Fault-tolerance Bottleneck driven Fault Injection) is a fault injection testing (FIT) approach to the effective and efficient validation of redundant components deployed in a service system. It uses the concept of **fault-tolerance bottleneck** to repeatedly generate fault injection configurations, and does not rely on *a priori*, complete knowledge of the system's business structure.

For the implementation details of FBFI, please refer to the following paper:

> Huayao Wu, Senyao Yu, Xintao Niu, Changhai Nie, Yu Pei, Qiang He, and Yun Yang. Enhancing Fault Injection Testing of Service Systems via Fault-Tolerance Bottleneck. IEEE Transactions on Software Engineering (TSE), doi: [10.1109/TSE.2023.3285357](https://doi.org/10.1109/TSE.2023.3285357)

## Usage

To use FBFI to test a system, the tester should have a *system workload* that exercises a certain number of services, a *fault injector* that implements the concrete fault injections, and a *tracing tool* that captures the execution paths of the system. Given such an application scenario, FBFI will take the system execution results observed as the input (either a successful execution path, or a system failure observed), and infer fault-tolerance bottlenecks as the locations of fault injection.

### For simulation
We currently provide a command-line utility that implements the core algorithms of FBFI (with `algorithm=FBFI`). In addition, the `algorithm` parameter can also be configured to `Random` or `LDFI`, in order to use different strategies to generate fault injection configurations.

```bash
java -jar Main.jar algorithm=[Algorithm_Name]
```
At first, since FBFI is unaware of the complete business structure, the tester should provide an initial successful execution path, and FBFI will use this path as the initial business structure. Here, each *execution path* should be encoded as `x_1-x_2-...-x_n`, where `x_i` is a string that indicates the name of a particular business node of the *i*-th service.

```
Please input a successful execution path (e.g. A_1-B_1-C_1):
A_1-B_1-C_1
```

FBFI will then update the business structure, and generate its fault-tolerance bottlenecks. The tester should use the given bottleneck to implement concrete fault injections and trace the system execution. At this moment, if an alternative execution path is obtained, this path should be given to FBFI for structure update. Otherwise, the input `-` should be given to FBFI. Note that the system should be reset (removes faults injected) after each fault injection.

```
Please inject config: [A_1]

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

Like FBFI, the random and LDFI approaches are also unaware of the complete business structure, and will rely on only the successful execution paths to update the business structure. The main difference between these approaches is in the generation of fault injection configurations: the random approach generates fault injection configurations randomly, while LDFI uses a SAT solver to infer bottlenecks.

### Interface

We also provide a programming interface for using the FBFI algorithm in your codes. To this end, you need to implement the `genPath(Set<String> config)` method in `Main.FBFIUsage`, which takes a fault injection configuration, `config`, as the input and returns the system execution path observed, `path`. After this, you can use the `RunFBFIExperiment()` method to perform FBFI. A detailed example can be found in `FBFIExample.java`.

```java
import Main.FBFIUsage;

FBFIUsage FBFI = new FBFIUsage() {
  @Override
  /**
   * @param config The configuration that needs to be injected into the SUT
   * @return The execution path observed, in the format of {"A_1", "B_1", "C_1"}, 
   *         or an empty list if the workload execution fails
   */
  public List<String> genPath(Set<String> config) throws Exception {
    List<String> path = new ArrayList<String>();
    // Your code may like this:
    // Inject(config);     // implement the concrete fault injection
    // executeWorkload();  // execute the workload
    // path = Tracer();    // get the execution path from the tracing tool
    // Restore(config);    // remove the faults injected 
    return path;
  }
};

FBFI.RunFBFIExperiment();
```





## Experiment

The performance of FBFI is evaluated under two microservice benchmark systems, [TrainTicket](https://github.com/FudanSELab/train-ticket/tree/jaeger) and [SockShop](https://github.com/microservices-demo/microservices-demo). Two additional automated FIT approaches, Radnom and LDFI (an adapted version), are used for comparison.

To reproduce the experiments, first run the following command to generate a series of configurations for deploying the benchmark system under different scales (note that the default deployment configuration does not introduce redundancy to service instances):

```bash
java -jar DeployConfig.jar
```

This will generate a series of configuration files, `../ExperimentData/[Subject]/[Scale]/[id]/scale.txt`, where `[Subject]` can be `TrainTicket` or `SockShop`, `[Scale]` can be `small`, `medium` or `large`, and `[id]` is in the rang of [1, 30]. This includes a total number of `2 * 3 * 30 = 180` configuration files, each of which corresponds to an experiment subject.

Next, for each experiment subject, run the following command to deploy the SUT and carry out the expeiment. 

```bash
java -jar Main.jar subject=[Subject Path]
# for example
# java -jar Main.jar subject=../ExperimentData/TrainTicket/small/1
```

Accoridng to the experiment setting, the three FIT approaches (that is, `FBFI`, `LDFI`, and `Random`) will be executed in order, and the maximum execution time constraints allocated to the random and LDFI approaches will be 1x and 3x of that of FBFI.

Alternatively, if you want to execute a specific FIT algorithm on the subject, run the following command:

```bash
java -jar Main.jar subject=[Subject Path] algorithm=[Algorithm Name] time=[Time Limit]
# for example
# java -jar Main.jar subject=../ExperimentData/TrainTicket/small/1 algorithm=FBFI time=3600
```

Here, the `algorithm` parameter can take `FBFI`, `LDFI` or `Random`; and the `time` parameter indicates the maximum execution time constraint allocated (in seconds, 3600 by default).

Once the experiment terminates, the following data will be reported:

* `Total Graph`: the whole business structure explored by algorithm
* `Bottlenecks`: the set of fault-tolerance bottlenecks identified by algorithm
* `Defects`: the set of defects identified by algorithm
* `Flaky Defects`: the set of flaky failure-causing configurations identified by algorithm
* `Node Coverage`: number of covered business nodes / total number of business nodes
* `Edge Coverage`: number of covered message transmissions / total number of message transmissions of the corresponding symmetric structure
* `Path Number`: the number of execution paths
* `Inject Number`: the number of fault injection configurations
* `Total Time`:  the total cost of experiment (in seconds)
* `HandleConfig Time`:  the injection cost of experiment, including the time cost of implementing injections and resetting the system (in seconds)
* `Algorithm Time`:  the generation cost of the algorithm (in seconds)

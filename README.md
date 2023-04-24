# FBFI

FBFI (Fault-tolerance Bottleneck driven Fault Injection) is a fault injection testing (FIT) approach to the effective and efficient validation of redundant components deployed in a service system. It uses the concept of **fault-tolerance bottleneck** to repeatedly generate fault injection configurations, and does not rely on *a priori*, complete knowledge of the system's business structure.

### Usage

To use FBFI to test a system, the tester should have a *system workload* that exercises a certain number of services, a *fault injector* that implements the concrete fault injections, and a *tracing tool* that captures the execution paths of the system. Given such an application scenario, FBFI will take the system execution results observed as the input (either a successful execution path, or a system failure observed), and infer fault-tolerance bottlenecks as the locations of fault injection.

We currently provide a command-line utility that implements the core algorithm of FBFI. The `algorithm` parameter can be configured to indicate which specific algorithm (namely, `FBFI`, `LDFI` or `Random`) will be used to generate fault injection configurations.

```bash
java -jar Experiment.jar algorithm=[Algorithm]
```
At first, the tester should provide an initial successful execution path of the system. Here, each *execution path* should be encoded as `x_1-x_2-...-x_n`, where `x_i` is a string that indicates the name of a particular business node of the *i*-th service.

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
Next, repeat the above process, until all bottlenecks inferred can break the system. At this moment, FBFI has explored the complete business structure, and the algorithm terminates.

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



### Experiment

The performance of FBFI is evaluated under two microservice benchmark systems, [TrainTicket](https://github.com/FudanSELab/train-ticket/tree/jaeger) and [SockShop](https://github.com/microservices-demo/microservices-demo). Two additional automated FIT approaches, Radnom and LDFI (an adapted version), are used for comparison.

To reproduce the experiments, first run the following command to generate deployment configurations of a specific deployment scale for each benchmark system (to introduce redundancy of different levels):

```bash
java -jar Experiment.jar subject=[Subject_Name] deployScale=[Scale]
```

Here, the `subject` parameter indicates the name of benchmark system, which can be `TrainTicket` or `SockShop`. The `deployScale` parameter indicates the deployment scale, which can be `small`, `medium` or `large` (that is, each service has `{1,2}`, `{2,3}`, or `{3,4}` redundant nodes).

Next, for each deployment configuration generated, use `docker-compose` command to deploy the benchmark system. For example, running the following command will deploy 4 nodes for service `ts-ticketinfo-service` and 3 nodes for service `ts-seat-service`.

```bash
docker-compose up --scale ts-ticketinfo-service=4 --scale ts-seat-service=3
```
After the successful deployment of the benchmark system, run the following command to carry out the experiment:

```bash
java -jar Experiment.jar subject=[Subject_Name] algorithm=[Algorithm_Name]
```
The `algorithm` parameter indicates the FIT approach that will be used to perform fault injection experiments, which can take `FBFI`, `LDFI` or `Random`.

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

The `ExperimentData` directory gives the raw experimental data observed in our experiment. For each deployment scale of the benchmark system, 30 different deployment configurations are used (i.e., 30 different business structures). So, there will be 30 sub-directories for each deployment scale. In each of these sub-directories, we provide:

* `scale.txt`: the particular deployment configuration
* `outputs_[Algorithm].txt`: the raw experiment results of running each FIT approach under this deployment configuration

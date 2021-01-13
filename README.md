# FBFI

FBFI (Fault-tolerance Bottleneck driven Fault Injection) is a fault injection testing approach to the effective and efficient validation of redundant components deployed in a cloud system. It uses the concept of **fault-tolerance bottleneck** to repeatedly generate fault injection configurations. FBFI does not particularly rely on the system’s complete business structure, but requires that the successful execution paths of the system can be traced.

### Usage

We currently provide a command-line utility that implements the main process of FBFI:

```bash
java -jar FBFI.jar [k]
```

where `[k]` is optional, indicating the maximum number of permissible edge disconnection faults when calculating fault-tolerance bottlenecks (zero by default).

To start FBFI, the tester should provide an initial succesful execution path of the system:

```
Please input a successful execution path (e.g. A1-B1-C1):
A1-B1-C1
```

Here, each *execution path* should be encoded as `x_1-x_2-...-x_n`, where `x_i` is a string that indicates the name of a particular business node of the *i*-th service.

FBFI will then update the business structure, and generate its fault-tolerance bottlenecks. The tester should use each of these bottlenecks to implement concrete fault injections in the testing environment and record the results.

```
Generate 3 fault injection configurations that can break the system (please use them to implement fault injections):
c1 = Crash(A1)
c2 = Crash(B1)
c3 = Crash(C1)
Input the newly tracked execution path (if the system works correctly), or -1 (if the system fails in all cases):
A1-B2-C1
```

Specifically, if an alternative execution path is obtained, the tester should terminate the injection and input this new path to FBFI (as the current structure is considered incomplete). FBFI will then update the structure, and calculate new bottlenecks.

Otherwise, if all bottlenecks can break the system, the tester should input -1 to indicate the end of the testing process. At this moment, FBFI will output the complete business structure constructed, and its all fault-tolerance bottlenecks.

```
The complete business structure: 
[A1, A2]-B1
[A1, A2]-B2
[B1, B2]-C1
[B1, B2]-C2
[C1, C2]-Final_goal

The set of all fault-tolerance bottlenecks of this structure is saved in bottlenecks.txt
```

Here, a *business structure* is composed of a number of lines. Each line is in the form of `[x_1, x_2, ..., x_m]-y_j`, indicating that there is a connection edge between nodes *x_i* and *y_j* for *1 <= i <= m*; and *x_i* belongs to the last service if *y_j* is *Final_goal*.



### Experiment

To reproduce the experimental results reported in the paper, use the following commands:

```bash
java -jar ExpExplore.jar		// fault injection testing experiment
java -jar ExpOptimise.jar		// structure improvement experiment
```

All subject business structures can be found in `subject.txt`. Specifically,

* A sysmetric business structure is represented as `c_1-c_2-...-c_n`, where `c_i` is an integer value that indicates the number of business nodes in the *i*-th service (a total number of *n* services), e.g., *2-3-2-3* (two nodes in the first service, three nodes in the second service, etc.).
* In the case of an asysmetric structure,  `[exlcudes] x_a-x_b, ..., x_p-x_q` is appended at the end of the above representation. Here, each `x_a-x_b` indicates a particular connection edge that will be excluded in the structure.

The dierctory `raw_data` contains all raw experimental data obtained.
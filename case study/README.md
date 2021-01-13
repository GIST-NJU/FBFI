# Case Study

The system under testing is named TrainTicket, which is an open source project and can be obtained from https://github.com/FudanSELab/train-ticket (commit 6bef294). We first deploy the system using Kubernetes, and then apply the different fault injection testing approaches to perform testing.



1. To deploy the system, use the following command:

   ```bash
   kubectl apply -f ts-deployment-part1.yml
   kubectl apply -f ts-deployment-part2.yml
   kubectl apply -f ts-deployment-part3.yml
   ```

2. Initialize the services under testing:

   ```bash
   kubectl scale deployment ts-travel-service --replicas=3
   kubectl scale deployment ts-assurance-service --replicas=3
   kubectl scale deployment ts-food-service --replicas=2
   kubectl scale deployment ts-contacts-service --replicas=2
   kubectl scale deployment ts-preserve-service --replicas=3
   kubectl scale deployment ts-order-service --replicas=3
   kubectl scale deployment ts-inside-payment-service --replicas=2
   kubectl scale deployment ts-order-other-service --replicas=3
   ```

3. Modify the IP addresses of TrainTicket, and the Jaeger tracking system in  `src/FBFI/main.java`:

   ```java
   private static String ipaddress="47.114.159.122:32677";		// train-ticket
   private static String jaegerIP="47.114.159.122:32688":		// jaeger
   ```

4. Run code in the following files to perform testing:
   * `src/FBFI/Main.java`: the FBFI approach
   * `src/LDFI/LDFIExp.java`: the LDFI approach (note that the execution of LDFI relies on the [Z3 solver](https://github.com/Z3Prover/z3/releases))
   * `src/Random/RandomExp`: the random approach

The raw experimental data obtained can be found in the `raw_data` directory.
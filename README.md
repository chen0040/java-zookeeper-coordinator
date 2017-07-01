# java-zookeeper-coordinator

A java library that makes easy to develop distributed-tasking system with fail-safe and load balancing master-slave cluster via zookeeper

# Usage

The distributed system consists of three different types of nodes:

* request nodes: these nodes take data streamed from external system (e.g., via Kafka) and pass them to the master nodes
* master nodes: these nodes are responsible for routing the requests to individual worker nodes
* worker nodes: these nodes are responsible for the bulk of task processing

Each of these types of nodes form a cluster so as to achieve load balancing and fail-safe.

When a request node send data to master node cluster, it can either:

Scenario 1. uses a partition algorithms to determine which master node to send the data
Scenario 2. send the data the the currently active leader master node only.

Each type of nodes have a life cycle in which the following API method can be exposed:

* startSystem(ipAddress: String, port: int, String nodeId)
* stopSystem()


### Create and run a master node

Firstly defines a class that inherits from MasterNode:

```java
public class MasterApplication extends MasterNode {

   private static final Logger logger = LoggerFactory.getLogger(MasterApplication.class);

   public MasterApplication(ZkConfig zkConfig) {
      super(zkConfig);
   }

   @Override public void takeLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has become leader at {}:{}", masterId, ipAddress, port);
   }

   @Override public void resignLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has resigned from leadership at {}:{}", masterId, ipAddress, port);
   }

   @Override public void startSystem(String ipAddress, int port, String masterId){
      logger.info("start system at {}:{} with id = {}", ipAddress, port, masterId);
   }

   @Override public void stopSystem() {
      logger.info("system shutdown");
   }

   public static void main(String[] args) throws IOException, InterruptedException {
      ZkConfig config = new ZkConfig();
      config.setZkConnect("192.168.10.12:2181,192.168.10.13:2181,192.168.10.14:2181");
      final MasterApplication application = new MasterApplication(config);
      application.addShutdownHook();
      application.runForever();
   }

}
```


### Create and run a worker node

Firstly defines a class that inherits from WorkerNode:

```java

```


### Create and run a request node

Firstly defines a class that inherits from RequestNode:

```java

```

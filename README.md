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

Firstly defines a main class MasterApplication that inherits from MasterNode:

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
      // zookeeper connection string
      String zkConnectionString = "192.168.10.12:2181,192.168.10.13:2181,192.168.10.14:2181"; 
      config.setZkConnect(zkConnectionString);
      final MasterApplication application = new MasterApplication(config);
      application.addShutdownHook();
      application.runForever();
   }
}
```
The MasterApplication.main(...) is the main entry point for the master node java program. 

The 4 APIs that user can overwrite during the lifecycle of the master node are explained below:

* The MasterApplication.startSystem() will be invoked when the master node managed to connect to the zookeeper
* The MasterApplication.resignLeadership() will be invoked when the master node starts to compete for the leadership,
which occurs either when the master node has just connected to the zookeeper or disconnected (e.g. due to networking 
problem or some other issues) and then reconnected again.
* The MasterApplication.takeLeadership() will be invoked when the master node is notified that it is currently the
active leader. 
* The MasterApplication.stopSystem() will be invoked when the application.shutdown() is called (Note that shutdown() is
call at the end in the above code due to application.addShutdownHook())

Inside the MasterApplication.runForever(), the method MasterApplication.start() is called first to start
 communication with zookeeper, the application is then entered into a forever while loop. 
 
In the case the MasterApplication needs to be incorporated with another framework (e.g. Spring framework) which
has its own forever loop, then calls MasterApplication.start() instead of MasterApplication.runForever.



### Create and run a worker node

Firstly defines a class that inherits from WorkerNode:

```java
public class WorkerApplication extends WorkerNode {

   private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

   public WorkerApplication(ZkConfig zkConfig) {
      super(zkConfig);
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
      final WorkerApplication application = new WorkerApplication(config);
      application.addShutdownHook();
      application.runForever();
   }

}
```

The WorkerApplication.main(...) is the main entry point for the worker node java program. 

The 2 APIs that user can overwrite during the lifecycle of the worker node are explained below:

* The WorkerApplication.startSystem() will be invoked when the worker node managed to connect to the zookeeper
* The WorkerApplication.stopSystem() will be invoked when the application.shutdown() is called (Note that shutdown() is
call at the end in the above code due to application.addShutdownHook())

Inside the WorkerApplication.runForever(), the method WorkerApplication.start() is called first to start
 communication with zookeeper, the application is then entered into a forever while loop. 
 
In the case the WorkerApplication needs to be incorporated with another framework (e.g. Spring framework) which
has its own forever loop, then calls WorkerApplication.start() instead of WorkerApplication.runForever.



### Create and run a request node

Firstly defines a class that inherits from RequestNode:

```java

```

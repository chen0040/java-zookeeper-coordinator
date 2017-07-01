# java-zookeeper-coordinator

A java library that makes easy to develop distributed-tasking system with fail-safe and load balancing master-slave cluster via zookeeper


# Install

Add the following dependency to your POM file:

```xml
<dependency>
  <groupId>com.github.chen0040</groupId>
  <artifactId>java-zookeeper-coordinator</artifactId>
  <version>1.0.2</version>
</dependency>
```

# Features

Compared to Curator, java-zookeeper-coordinator is very light-weight, also it has many features built for
building request-master-slave cluster architecture. It is also designed to handle CRUD on 
large number of tasks and task assignments in the zookeeper (using a partition technique which controls
the number of children under each zk node)

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

Create a new java project, in the project, define a main class MasterApplication that inherits from MasterNode:

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
      config.setStartingPort(6000); // the master node java program will find an un-used port from the port range starting at 6000
      
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

During the lifecycle of the master node, the MasterApplication has access to the following api to create tasks and assign tasks to worker nodes, as well
as route message to these worker nodes:

* taskExists(String taskId, BiConsumer<String, Boolean> callback): this api check whether a particular task
has been created in the task cluster.
* createTask(String taskId, Consumer<String> callback): this api create the task in the zookeeper task cluster
* isTaskAssigned(String taskId, BiConsumer<String, Boolean> callback): this api check whether a particular task
has been assigned to any worker node in the worker cluster.
* getWorkerAssigned2Task(String taskId, Consumer<NodeUri> callback): this api query the uri of the worker node
for which the task has been assigned to
* assignTask(String taskId, BiConsumer<String, NodeUri> callback): this api assign a task to one of the worker
in the worker cluster.



### Create and run a worker node

Create a new java project, in the project, define a main class WorkerApplication that inherits from WorkerNode:

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
      config.setStartingPort(7000); // the worker node java program will find an un-used port from the port range starting at 7000
      
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

By default worker node does not know anything about the masters, if it needs access to masters cluster, then
call one of the following api before calling WorkerApplication.runForever() or WorkerApplication.start():

* WorkerApplication.setTrackingLeader(true): this call will enable the worker node to access to the currently
active master leader by calling WorkerApplication.leaderExists() and WorkerApplication.getLeaderUri() during 
the application's lifecycle.
* WorkerApplication.setTrackingMasters(true): this call will enable the worker node to access the master
cluster by calling WorkerApplication.getMasters() during the application's lifecycle.
* WorkerApplication.setCapableOfTaskCreation(true): this call will enable the worker node to either
create a new task (calling WorkerApplication.createTask(...)) or check whether a task exists (calling 
WorkerApplication.taskExists(...)) during the application's lifecycle.


### Create and run a request node

Create a new java project, in the project, define a main class RequestApplication that inherits from RequestNode:

```java
public class RequestApplication extends RequestNode {

   private static final Logger logger = LoggerFactory.getLogger(RequestApplication.class);

   public RequestApplication(ZkConfig zkConfig) {
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
      config.setStartingPort(9000); // the request node java program will find an un-used port from the port range starting at 9000
      
      final RequestApplication application = new RequestApplication(config);
      application.addShutdownHook();
      application.runForever();
   }

}
```


The RequestApplication.main(...) is the main entry point for the request node java program. 

The 2 APIs that user can overwrite during the lifecycle of the request node are explained below:

* The RequestApplication.startSystem() will be invoked when the request node managed to connect to the zookeeper
* The RequestApplication.stopSystem() will be invoked when the application.shutdown() is called (Note that shutdown() is
call at the end in the above code due to application.addShutdownHook())

Inside the RequestApplication.runForever(), the method RequestApplication.start() is called first to start
 communication with zookeeper, the application is then entered into a forever while loop. 
 
In the case the RequestApplication needs to be incorporated with another framework (e.g. Spring framework) which
has its own forever loop, then calls RequestApplication.start() instead of RequestApplication.runForever.

The following api allows the request node to communicate with the master leader or master cluster during the lifecycle
of the request node:

* boolean leaderExists(): this api checks whether the master leader exists
* NodeUri getLeaderUri(): this api returns the uri of the currently active master leader
* List<NodeUri> getMasters(): this api returns the uris of the active masters in the master cluster
* taskExists(String taskId, BiConsumer<String, Boolean> callback): this api checks if a particular task has been
created in the zookeeper task cluster
* createTask(String taskId): this api creates a task in the zookeeper task cluster.

### Interact with the cluster using ClientNode

For an application external to the cluster to interact with the cluster, a ClientNode class can be used:

```java
ZkConfig config = new ZkConfig();
config.setZkConnect("192.168.10.12:2181,192.168.10.13:2181,192.168.10.14:2181");

final ClientNode application = new ClientNode(config);
application.setTrackingWorkers(true); // default set to false 
application.connect();

// return the list of uris of all active master nodes
List<NodeUri> masters = application.getMasters();

// return the list of uris of all active request nodes
List<NodeUri> producers = application.getProducers();

// return the list of uris of all active worker nodes (only available when trackingWorkers is set to true)
List<NodeUri> workers = application.getWorkers();

application.disconnect();
```

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

```


### Create and run a worker node

Firstly defines a class that inherits from WorkerNode:

```java

```


### Create and run a request node

Firstly defines a class that inherits from RequestNode:

```java

```

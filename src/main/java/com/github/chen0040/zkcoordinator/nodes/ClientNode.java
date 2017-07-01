package com.github.chen0040.zkcoordinator.nodes;


import com.github.chen0040.zkcoordinator.models.NodeUri;
import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.services.*;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import lombok.Getter;
import lombok.Setter;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Created by xschen on 1/7/2017.
 */
public class ClientNode implements Watcher, AutoCloseable, ZookeeperActor {
   private static final Logger logger = LoggerFactory.getLogger(WorkerNode.class);

   @Getter
   private final String ipAddress;

   private ZkConnector connector;
   private LeaderWatchService leaderWatchService;
   private MasterClusterService masterClusterService;
   private TaskAssignmentService taskAssignmentService;
   private WorkerClusterService workerClusterService;
   private RequestClusterService requestClusterService;

   @Getter
   private String workerId = null;

   @Getter
   private int registeredPort = -1;

   @Getter
   private final int initialPort;

   @Getter
   private final String zkConnect;

   @Getter
   private final String groupName;

   @Getter
   private boolean running = false;

   @Getter
   private final ZkConfig zkConfig;

   @Setter
   private boolean trackingLeader = true;
   @Setter
   private boolean trackingMasters = true;
   @Setter
   private boolean capableOfTaskCreation = false;
   @Setter
   private boolean trackingRequests = true;
   @Setter
   private boolean trackingWorkers = false;

   public ClientNode(ZkConfig config) {
      this.zkConfig = config;
      this.zkConnect = zkConfig.getZkConnect();
      this.initialPort = zkConfig.getStartingPort();
      this.groupName = zkConfig.getWorkerGroupName();
      ipAddress = IpTools.getIpAddress();

   }

   protected void onZkReconnected(String state) {
      logger.info("this instance (id = {}) is connected to zookeeper", workerId);
   }

   public void connect(long timeout) throws IOException {
      start();
      try {
         Thread.sleep(timeout);
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void connect() throws IOException {
      connect(2000);
   }

   public void disconnect() throws InterruptedException {
      shutdown();
   }

   @Override
   public void start() throws IOException {

      connector = new ZkConnector(zkConfig.getZkConnect(), zkConfig.getReconnectDelayWhenSessionExpired());

      connector.onZkStarted(zk -> {
         logger.info("Zookeeper connected!");

         taskAssignmentService = createTaskAssignmentService(zk);

         leaderWatchService = createLeaderWatchService(zk);
         masterClusterService = createMasterClusterService(zk);

         requestClusterService = createRequestClusterService(zk);
         workerClusterService = createWorkerClusterService(zk);

         if(leaderWatchService != null) {
            leaderWatchService.watchLeader();
         }
         if(masterClusterService != null) {
            masterClusterService.watchMasters();
         }

         if(requestClusterService != null) {
            requestClusterService.watchProducers();
         }

         if(workerClusterService != null) {
            workerClusterService.watchWorkers();
         }

      });

      connector.start(zkConfig.getSessionTimeout());
   }

   @Override
   public void shutdown() throws InterruptedException {
      connector.stopZk();
      running = false;
   }


   @Override public void process(WatchedEvent watchedEvent) {

   }


   @Override public void close() throws Exception {
      disconnect();
   }

   protected TaskAssignmentService createTaskAssignmentService(ZooKeeper zk) {
      if(!capableOfTaskCreation) return null;
      return  new TaskAssignmentServiceImpl(zk, zkConfig);
   }

   protected LeaderWatchService createLeaderWatchService(ZooKeeper zk) {
      if(!trackingLeader) return null;
      return new LeaderWatchServiceImpl(zk, zkConfig);
   }

   protected MasterClusterService createMasterClusterService(ZooKeeper zk) {
      if(!trackingMasters) return null;
      return new MasterClusterServiceImpl(zk, zkConfig);
   }

   protected RequestClusterService createRequestClusterService(ZooKeeper zk) {
      if(!trackingRequests) return null;
      return new RequestClusterServiceImpl(zk, zkConfig.getRequestPath(), zkConfig.getRequestSystemName());
   }

   protected WorkerClusterService createWorkerClusterService(ZooKeeper zk) {
      if(!trackingWorkers) return null;
      return new WorkerClusterServiceImpl(zk, zkConfig.getWorkerPath(), zkConfig.getWorkerSystemName());
   }

   public boolean leaderExists() {
      if(leaderWatchService == null){
         logger.error("This node is not capable of tracking leader!");
         return false;
      }
      return leaderWatchService.leaderExists();
   }


   public NodeUri getLeaderUri() {
      if(leaderWatchService == null) {
         logger.error("This node is not capable of tracking leader!");
         return null;
      }
      return leaderWatchService.getLeaderUri();
   }


   public void taskExists(String taskId, BiConsumer<String, Boolean> callback) {
      if(taskAssignmentService == null) {
         logger.error("This node is not capable of task assignment!");
         return;
      }
      taskAssignmentService.taskExists(taskId, callback);
   }


   public void createTask(String taskId) {
      if(taskAssignmentService == null) {
         logger.error("This node is not capable of task assignment!");
         return;
      }
      taskAssignmentService.createTask(taskId, (tsId) -> logger.info("Task created: {}", tsId));
   }


   public List<NodeUri> getMasters() {
      if(masterClusterService == null) {
         logger.error("This node is not capable of tracking masters!");
         return new ArrayList<>();
      }
      return masterClusterService.masters();
   }

   public List<NodeUri> getProducers() {
      if(requestClusterService == null) {
         logger.error("This node is not capable of tracking requests!");
         return new ArrayList<>();
      }
      return requestClusterService.producers();
   }

   public List<NodeUri> getWorkers() {
      if(workerClusterService == null) {
         logger.error("This node is not capable of tracking workers!");
         return new ArrayList<>();
      }
      return workerClusterService.workers();
   }

}

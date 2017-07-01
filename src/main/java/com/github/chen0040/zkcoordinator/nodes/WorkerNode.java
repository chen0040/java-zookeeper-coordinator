package com.github.chen0040.zkcoordinator.nodes;

import com.github.chen0040.zkcoordinator.models.NodeUri;
import com.github.chen0040.zkcoordinator.models.RegistrationCompleted;
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
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Created by xschen on 12/4/15.
 */
public class WorkerNode implements Watcher, AutoCloseable, SystemActor, ZookeeperActor {

   private static final Logger logger = LoggerFactory.getLogger(WorkerNode.class);

   @Getter
   private final String ipAddress;

   private RegistrationService registrationService;
   private LeaderWatchService leaderWatchService;
   private MasterClusterService masterClusterService;
   private TaskAssignmentService taskAssignmentService;

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
   private boolean trackingLeader = false;
   @Setter
   private boolean trackingMasters = false;
   @Setter
   private boolean capableOfTaskAssignment = false;

   public WorkerNode(ZkConfig config) {
      this.zkConfig = config;
      this.zkConnect = zkConfig.getZkConnect();
      this.initialPort = zkConfig.getStartingPort();
      this.groupName = zkConfig.getWorkerGroupName();
      ipAddress = IpTools.getIpAddress();

   }

   protected void onZkReconnected(String state) {
      logger.info("this instance (id = {}) is connected to zookeeper", workerId);
   }

   protected void onGroupJoined(ZooKeeper zk, RegistrationCompleted rc) {

      int port = rc.getPort();
      if(!running) {
         String serverId = rc.getServerId();
         registeredPort = port;

         logger.info("join group: {}:{}", ipAddress, port);

         workerId = ipAddress + "-" + port;
         MDC.put("nodeid", workerId); // this is defined for the ${nodeid} in the logback.xml

         startSystem(ipAddress, port, workerId);

         running =true;

      } else if(port != registeredPort){
         logger.error("Fatal error: port on re-registration differs from the original registered port!");
         System.exit(0);
      }

   }

   @Override
   public void start() throws IOException {

      registrationService = new RegistrationServiceImpl(this, zkConnect, zkConfig, groupName, ipAddress);

      registrationService.onZkStarted(zk -> {
         logger.info("Zookeeper connected!");

         taskAssignmentService = createTaskAssignmentService(zk);

         leaderWatchService = createLeaderWatchService(zk);
         masterClusterService = createMasterClusterService(zk);

         if(leaderWatchService != null) {
            leaderWatchService.watchLeader();
         }
         if(masterClusterService != null) {
            masterClusterService.watchMasters();
         }
      });

      registrationService.addGroupJoinListener(this::onGroupJoined);

      registrationService.onZkReconnected(this::onZkReconnected);

      registrationService.start(zkConfig.getSessionTimeout(), initialPort);
   }

   @Override
   public void shutdown() throws InterruptedException {
      registrationService.stopZk();
      stopSystem();
      running = false;
   }

   @Override public void startSystem(String ipAddress, int port, String masterId){
      logger.info("start system at {}:{} with id = {}", ipAddress, port, masterId);
   }

   @Override
   public void stopSystem() {
      logger.info("system shutdown");
   }


   @Override public void process(WatchedEvent watchedEvent) {

   }


   @Override public void close() throws Exception {

   }

   protected TaskAssignmentService createTaskAssignmentService(ZooKeeper zk) {
      if(!capableOfTaskAssignment) return null;
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
}

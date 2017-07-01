package com.github.chen0040.zkcoordinator.nodes;


import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;
import com.github.chen0040.zkcoordinator.model.RegistrationCompleted;
import com.github.chen0040.zkcoordinator.model.ZkConfig;
import com.github.chen0040.zkcoordinator.services.*;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import lombok.Getter;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Created by xschen on 19/12/15.
 */
public class RequestNode implements Watcher, SystemActor, ZookeeperActor {

   private static final Logger logger = LoggerFactory.getLogger(RequestNode.class);

   private RegistrationService registrationService;
   private LeaderWatchService leaderWatchService;
   private TaskAssignmentService taskAssignmentService;
   private MasterClusterService masterClusterService;

   @Getter
   private final String ipAddress;

   @Getter
   private int registeredPort = -1;

   @Getter
   private String requestId;

   @Getter
   private final String groupName;

   @Getter
   private final String zkConnect;

   @Getter
   private final int initialPort;

   @Getter
   private final ZkConfig zkConfig = new ZkConfig();

   @Getter
   private boolean running = false;

   public RequestNode(String zkConnect, int initialPort, String groupName) {
      ipAddress = IpTools.getIpAddress();
      this.initialPort = initialPort;
      this.zkConnect = zkConnect;
      this.groupName = groupName;
   }

   @Override public void startSystem(String ipAddress, int port, String masterId){
      logger.info("start system at {}:{} with id = {}", ipAddress, port, masterId);
   }

   @Override
   public void stopSystem() {
      logger.info("system shutdown");
   }

   protected void onZkReconnected(String state) {
      logger.info("this instance (id = {}) is now connected to zookeeper", requestId);
   }

   protected void onGroupJoined(ZooKeeper zk, RegistrationCompleted rc) {
      int port = rc.getPort();

      if(!running) {

         registeredPort = port;

         requestId = ipAddress + "-" + port;
         MDC.put("nodeid", requestId); // this is defined for the ${nodeid} in the logback.xml

         logger.info("join group: {}:{}", ipAddress, port);

         startSystem(ipAddress, port, requestId);

         running = true;
      } else if(port != registeredPort){
         logger.error("Fatal error: port on re-registration differs from the original registered port!");
         System.exit(0);
      }
   }

   @Override
   public void start() throws IOException {

      registrationService = new RegistrationServiceImpl(this, zkConnect, zkConfig, groupName, ipAddress);

      registrationService.onZkStarted(zk -> {
         leaderWatchService = createLeaderWatchService(zk);
         taskAssignmentService = createTaskAssignmentService(zk);

         masterClusterService = createMasterClusterService(zk);

         leaderWatchService.watchLeader();
         masterClusterService.watchMasters();
      });

      registrationService.onZkReconnected(this::onZkReconnected);


      registrationService.addGroupJoinListener(this::onGroupJoined);

      registrationService.start(zkConfig.getSessionTimeout(), initialPort);
   }

   @Override
   public void shutdown() throws InterruptedException {
      registrationService.stopZk();
      stopSystem();
      running = false;
   }

   protected TaskAssignmentService createTaskAssignmentService(ZooKeeper zk) {
      return  new TaskAssignmentServiceImpl(zk, zkConfig);
   }

   protected LeaderWatchService createLeaderWatchService(ZooKeeper zk) {
      return new LeaderWatchServiceImpl(zk, zkConfig);
   }

   protected MasterClusterService createMasterClusterService(ZooKeeper zk) {
      return new MasterClusterServiceImpl(zk, zkConfig);
   }




   @Override public void process(WatchedEvent watchedEvent) {

   }


   public boolean leaderExists() {
      return leaderWatchService.leaderExists();
   }


   public AkkaNodeUri getLeaderUri() {
      return leaderWatchService.getLeaderUri();
   }


   public void taskExists(String taskId, BiConsumer<String, Boolean> callback) {
      taskAssignmentService.taskExists(taskId, callback);
   }


   public void createTask(String taskId) {
      taskAssignmentService.createTask(taskId, (tsId) -> logger.info("Task created: {}", tsId));
   }


   public List<AkkaNodeUri> getMasters() {
      return masterClusterService.masters();
   }
}

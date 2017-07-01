package com.github.chen0040.zkcoordinator.controllers;

import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;
import com.github.chen0040.zkcoordinator.model.RegistrationCompleted;
import com.github.chen0040.zkcoordinator.model.ZkConfigs;
import com.github.chen0040.zkcoordinator.consts.ZkNodePaths;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class MasterController implements Watcher, Master {

   private static final Logger logger = LoggerFactory.getLogger(MasterController.class);

   private TaskAssignmentService taskAssignmentService;
   private WorkerClusterService workerClusterService;
   private ProducerClusterService producerClusterService;

   private RegistrationService registrationService;
   private LeaderElectionService leaderElectionService;
   private BootstrapService bootstrapService;

   @Getter
   private final String ipAddress;

   @Getter
   private String masterId = null;

   @Getter
   private int registeredPort = -1;

   @Getter
   private final String zkConnect;

   @Getter
   private final String groupName;

   @Getter
   private boolean running = false;

   @Getter
   private final int initialPort;

   public MasterController(String zkConnect, String groupName, int initialPort) {
      this.zkConnect = zkConnect;
      this.groupName = groupName;
      this.ipAddress = IpTools.getIpAddress();
      this.initialPort = initialPort;
   }


   @Override public void takeLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has become leader at {}:{}", masterId, ipAddress, port);
   }

   @Override public void resignLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has resigned from leadership at {}:{}", masterId, ipAddress, port);
   }

   @Override public void stopSystem() {
      logger.info("system shutdown");
   }

   protected void onZkReconected(String state) {
      logger.info("zookeeper reconnected");
   }

   protected void onZkStarted(ZooKeeper zk) {

      bootstrapService = createBootstrapService(zk);
      taskAssignmentService = createTaskAssignmentService(zk);
      workerClusterService = createWorkerClusterService(zk);
      producerClusterService = createProducerClusterService(zk);

      workerClusterService.addWorkerChangeListener((workers) -> taskAssignmentService.reassignAndSet(workers));


      bootstrapService.bootstrap();
      workerClusterService.watchWorkers();
      producerClusterService.watchProducers();

      try {
         Thread.sleep(100L);
      }
      catch (InterruptedException e) {
         logger.warn("sleep interrupted", e);
      }
   }

   protected void onZkGroupJoined(ZooKeeper zk, RegistrationCompleted rc){
      String serverId = rc.getServerId();
      int port = rc.getPort();

      if(!running) {

         logger.info("join group: {}:{}", ipAddress, port);

         registeredPort = port;

         masterId = ipAddress + "-" + port;

         MDC.put("nodeid", masterId); // this is defined for the ${nodeid} in the logback.xml

         leaderElectionService = createLeaderElectionService(zk, serverId, port);

         leaderElectionService.addLeadershipListener((id, prt) -> takeLeadership(ipAddress, prt, masterId));
         leaderElectionService.addResignListener((id, prt) -> resignLeadership(ipAddress, prt, masterId));
         leaderElectionService.runForLeader();

         startSystem(ipAddress, port, masterId);

         running = true;

      } else if(port != registeredPort){
         logger.error("Fatal error: port on re-registration differs from the original registered port!");
         System.exit(0);
      }
   }

   public void start() throws IOException {
      registrationService = createRegistrationService();

      registrationService.onZkStarted(this::onZkStarted);

      registrationService.onZkReconnected(this::onZkReconected);

      registrationService.addGroupJoinListener(this::onZkGroupJoined);

      registrationService.start(ZkConfigs.sessionTimeout, initialPort);
   }

   protected RegistrationService createRegistrationService(){
      return new RegistrationServiceImpl(this, zkConnect, groupName, ipAddress, ZkConfigs.reconnectDelayWhenSessionExpired);
   }

   protected LeaderElectionService createLeaderElectionService(ZooKeeper zk, String serverId, int port){
      return new LeaderElectionServiceImpl(zk, serverId, port, ZkNodePaths.DevLeader);
   }

   protected BootstrapService createBootstrapService(ZooKeeper zk) {
      return new BootstrapServiceImpl(zk);
   }

   protected TaskAssignmentService createTaskAssignmentService(ZooKeeper zk) {
      return  new TaskAssignmentServiceImpl(zk, ZkNodePaths.DevTasks, ZkNodePaths.DevTaskAssignments);
   }

   protected WorkerClusterService createWorkerClusterService(ZooKeeper zk) {
      return new WorkerClusterServiceImpl(zk, ZkNodePaths.DevWorkers);
   }

   protected ProducerClusterService createProducerClusterService(ZooKeeper zk) {
      return new ProducerClusterServiceImpl(zk, ZkNodePaths.DevReaders);
   }


   @Override public void startSystem(String ipAddress, int port, String masterId){
      logger.info("start system at {}:{} with id = {}", ipAddress, port, masterId);
   }

   public void shutdown() throws InterruptedException {
      registrationService.stopZk();
      running = false;
      stopSystem();
   }

   @Override public void process(WatchedEvent watchedEvent) {

   }

   public void isTaskAssigned(String taskId, BiConsumer<String, Boolean> callback) {
      taskAssignmentService.isTaskAssigned(taskId, callback);
   }


   public void getWorkerAssigned2Task(String taskId, Consumer<AkkaNodeUri> callback) {
      taskAssignmentService.getWorkerAssigned2Task(taskId, callback);
   }


   public void assignTask(String taskId, BiConsumer<String, AkkaNodeUri> callback) {
      taskAssignmentService.assignTask(taskId, callback);
   }


   public void taskExists(String taskId, BiConsumer<String, Boolean> callback) {
      taskAssignmentService.taskExists(taskId, callback);
   }


   public void createTask(String taskId, Consumer<String> callback) {
      taskAssignmentService.createTask(taskId, callback);
   }

}
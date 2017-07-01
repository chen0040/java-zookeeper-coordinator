package com.github.chen0040.zkcoordinator.nodes;

import com.github.chen0040.zkcoordinator.models.NodeUri;
import com.github.chen0040.zkcoordinator.models.RegistrationCompleted;
import com.github.chen0040.zkcoordinator.models.ZkConfig;
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


public class MasterNode implements Watcher, MasterActor, ZookeeperActor {

   private static final Logger logger = LoggerFactory.getLogger(MasterNode.class);

   private TaskAssignmentService taskAssignmentService;
   private WorkerClusterService workerClusterService;
   private RequestClusterService requestClusterService;

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

   @Getter
   private final ZkConfig zkConfig;

   public MasterNode(ZkConfig zkConfig) {
      this.zkConfig = zkConfig;
      this.zkConnect = zkConfig.getZkConnect();
      this.groupName = zkConfig.getMasterGroupName();
      this.ipAddress = IpTools.getIpAddress();
      this.initialPort = zkConfig.getStartingPort();
   }


   protected void onZkReconected(String state) {
      logger.info("zookeeper reconnected");
   }

   protected void onZkStarted(ZooKeeper zk) {

      bootstrapService = createBootstrapService(zk);
      taskAssignmentService = createTaskAssignmentService(zk);
      workerClusterService = createWorkerClusterService(zk);
      requestClusterService = createRequestClusterService(zk);

      workerClusterService.addWorkerChangeListener((workers) -> taskAssignmentService.reassignAndSet(workers));


      bootstrapService.bootstrap();
      workerClusterService.watchWorkers();
      requestClusterService.watchProducers();

      try {
         Thread.sleep(100L);
      }
      catch (InterruptedException e) {
         logger.warn("sleep interrupted", e);
      }
   }

   public boolean isSelf(NodeUri nodeUri) {
      return masterId.equals(nodeUri.nodeId());
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

   @Override
   public void start() throws IOException {
      registrationService = createRegistrationService();

      registrationService.onZkStarted(this::onZkStarted);

      registrationService.onZkReconnected(this::onZkReconected);

      registrationService.addGroupJoinListener(this::onZkGroupJoined);

      registrationService.start(zkConfig.getSessionTimeout(), initialPort);
   }

   @Override
   public void shutdown() throws InterruptedException {
      registrationService.stopZk();
      running = false;
      stopSystem();
   }

   protected RegistrationService createRegistrationService(){
      return new RegistrationServiceImpl(this, zkConnect, zkConfig, groupName, ipAddress);
   }

   protected LeaderElectionService createLeaderElectionService(ZooKeeper zk, String serverId, int port){
      return new LeaderElectionServiceImpl(zk, serverId, port, zkConfig.getLeaderPath());
   }

   protected BootstrapService createBootstrapService(ZooKeeper zk) {
      return new BootstrapServiceImpl(zk, zkConfig);
   }

   protected TaskAssignmentService createTaskAssignmentService(ZooKeeper zk) {
      return  new TaskAssignmentServiceImpl(zk, zkConfig);
   }

   protected WorkerClusterService createWorkerClusterService(ZooKeeper zk) {
      return new WorkerClusterServiceImpl(zk, zkConfig.getWorkerPath(), zkConfig.getWorkerSystemName());
   }

   protected RequestClusterService createRequestClusterService(ZooKeeper zk) {
      return new RequestClusterServiceImpl(zk, zkConfig.getRequestPath(), zkConfig.getRequestSystemName());
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

   @Override public void process(WatchedEvent watchedEvent) {

   }

   public void isTaskAssigned(String taskId, BiConsumer<String, Boolean> callback) {
      taskAssignmentService.isTaskAssigned(taskId, callback);
   }


   public void getWorkerAssigned2Task(String taskId, Consumer<NodeUri> callback) {
      taskAssignmentService.getWorkerAssigned2Task(taskId, callback);
   }


   public void assignTask(String taskId, BiConsumer<String, NodeUri> callback) {
      taskAssignmentService.assignTask(taskId, callback);
   }


   public void taskExists(String taskId, BiConsumer<String, Boolean> callback) {
      taskAssignmentService.taskExists(taskId, callback);
   }


   public void createTask(String taskId, Consumer<String> callback) {
      taskAssignmentService.createTask(taskId, callback);
   }

}

package com.github.chen0040.zkcoordinator.nodes;

import com.github.chen0040.zkcoordinator.models.RegistrationCompleted;
import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.services.RegistrationService;
import com.github.chen0040.zkcoordinator.services.RegistrationServiceImpl;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import lombok.Getter;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;


/**
 * Created by xschen on 12/4/15.
 */
public class WorkerNode implements Watcher, AutoCloseable, SystemActor, ZookeeperActor {

   private static final Logger logger = LoggerFactory.getLogger(WorkerNode.class);

   @Getter
   private final String ipAddress;

   private RegistrationService registrationService;

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

   public WorkerNode(ZkConfig config) {
      this.zkConfig = config;
      this.zkConnect = zkConfig.getZkConnect();
      this.initialPort = zkConfig.getInitialPort();
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
}

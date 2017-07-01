package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.consts.TaskStates;
import com.github.chen0040.zkcoordinator.model.UTF8;
import com.github.chen0040.zkcoordinator.model.ZkConfig;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import com.github.chen0040.zkcoordinator.model.RegistrationCompleted;
import com.github.chen0040.zkcoordinator.utils.ZkTimer;
import com.github.chen0040.zkcoordinator.utils.ZkTimerFactory;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public class RegistrationServiceImpl implements RegistrationService {

   private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);
   private ZooKeeper zk;
   private Watcher watcher;
   private int port;
   private String serverId;
   private final String ipAddress;
   private final String groupName;
   private final String zkConnect;
   private int sessionTimeout;
   private final ZkTimer timer;
   private long reconnectDelayWhenSessionExpired;
   private final AtomicBoolean clientState = new AtomicBoolean(true);

   private Consumer<ZooKeeper> zkStarted;
   private Consumer<String> zkClosed;
   private Consumer<String> zkReconnected;

   private String zkSessionId = "";
   private long lastTimeout = 0L;

   private boolean isAlreadyRegisteredOneTime = false;

   private final String zkRootPath;
   private final String zkNodePath;


   private List<BiConsumer<ZooKeeper, RegistrationCompleted>> groupJoinListeners = new ArrayList<>();
   private AsyncCallback.StringCallback callbackJoinGroup = (rc, path, context, name) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            joinGroup();
            break;
         case OK:
            logger.info("Register node successfully: " + serverId);
            notifyGroupJoined();
            break;
         case NODEEXISTS:
            logger.info("Already registered: " + serverId);
            break;
         default:
            logger.error("Something went wrong.", KeeperException.create(KeeperException.Code.get(rc), path));
      }
   };


   public RegistrationServiceImpl(Watcher watcher, String zkConnect, ZkConfig zkConfig, String groupName, String ipAddress) {
      this.watcher = watcher;
      this.zkConnect = zkConnect;

      this.groupName = groupName;
      this.ipAddress = ipAddress;

      this.zkRootPath = zkConfig.getRootPath();
      this.zkNodePath = zkConfig.getNodePath();

      this.reconnectDelayWhenSessionExpired = zkConfig.getReconnectDelayWhenSessionExpired();

      this.timer = ZkTimerFactory.createTimer();
   }

   @Override public String getZkSessionId(){
      return zkSessionId;
   }


   private void notifyGroupJoined() {
      groupJoinListeners.forEach(listener -> listener.accept(zk, new RegistrationCompleted(serverId, port)));
   }


   public void addGroupJoinListener(BiConsumer<ZooKeeper, RegistrationCompleted> listener) {
      groupJoinListeners.add(listener);
   }

   public void onZkStarted(Consumer<ZooKeeper> listener){
      zkStarted = listener;
   }


   private void joinGroup() {
      MDC.put("port", "" + port);
      zk.create(zkRootPath + "/" + groupName + "/" + serverId, UTF8.getBytes("Idle"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL,
              callbackJoinGroup, null);
   }


   private void register(int port) {
      this.port = port;
      this.serverId = ipAddress + "_" + port;

      AsyncCallback.StringCallback callback = (rc, path, context, name) -> {
         switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
               register(port);
               break;
            case OK:
               logger.info("Register successfully: " + serverId);
               this.isAlreadyRegisteredOneTime = true;
               joinGroup();
               break;
            case NODEEXISTS:
               logger.info("Already registered: " + serverId);
               register(IpTools.getNextAvailablePort(port));
               break;
            default:
               logger.error("Something went wrong: " + KeeperException.create(KeeperException.Code.get(rc), path));
         }
      };

      zk.create(zkNodePath + "/" + serverId, UTF8.getBytes(TaskStates.Idle), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, callback, null);
   }

   @Override
   public long getLastTimeout(){
      return lastTimeout;
   }

   private void reconnectWhenSessionExpired() {
      lastTimeout = new Date().getTime();

      if (!clientState.get()) {
         logger.warn("Zk will not reconnect: already closed.");
         return;
      }

      ZooKeeper zookeeper = this.zk;
      assert zookeeper != null;

      if (zookeeper.getState().isConnected()) {
         zkSessionId = Long.toHexString(zookeeper.getSessionId());
         logger.info("ZkSession(0x{}) is connected.", zkSessionId);

         createTimeout();
         return;
      } else {
         zkSessionId = "";
      }


      warnReconnect(zookeeper);

      closeZooKeeper(zookeeper);

      timer.newTimeout(timeout -> {
         if (timeout.isCancelled()) {
            return;
         }

         logger.info("Start reconnecting ...");

         try {
            start(this.sessionTimeout, this.port);
         }
         catch (IOException e) {
            logger.error("Failed to start zk", e);
         }

      }, reconnectDelayWhenSessionExpired, TimeUnit.MILLISECONDS);



   }

   private void warnReconnect(ZooKeeper zookeeper) {
      String warningMessage = "Execute reconnectWhenSessionExpired()(Expired session:0x" + Long.toHexString(zookeeper.getSessionId()) + ").";
      logger.warn(warningMessage);

      if(zkReconnected != null){
         zkReconnected.accept(warningMessage);
      }
   }

   private void closeZooKeeper(ZooKeeper zookeeper){
      if (zookeeper != null) {
         try {
            zookeeper.close();
         }
         catch (InterruptedException e) {
            logger.error("zk closed interrupted", e);
         } finally {
            if (zkClosed != null) {
               zkClosed.accept("zk-closed");
            }
         }

      }
   }


   @Override public void start(int sessionTimeout, int initialPort) throws IOException {
      this.sessionTimeout = sessionTimeout;
      ZooKeeper newZookeeper = createNewZookeeper();
      if (newZookeeper == null) {
         logger.warn("Failed to create new Zookeeper instance. It will be retry  after {} ms.", reconnectDelayWhenSessionExpired);
      } else {
         this.zk = newZookeeper;
         if(zkStarted != null) {
            zkStarted.accept(newZookeeper);
         }

         if(!isAlreadyRegisteredOneTime && !IpTools.isPortAvailable(initialPort)){
            initialPort = IpTools.getNextAvailablePort(initialPort);
         }

         register(initialPort);
      }

      createTimeout();
   }

   private void createTimeout(){
      timer.newTimeout(timeout -> {
         if (timeout.isCancelled()) {
            return;
         }

         reconnectWhenSessionExpired();
      }, reconnectDelayWhenSessionExpired, TimeUnit.MILLISECONDS);
   }


   @Override public void stopZk() throws InterruptedException {
      if (clientState.compareAndSet(true, false)) {
         if (timer != null) {
            timer.stop();
         }
         closeZooKeeper(zk);
      }
   }


   @Override public void onZkClosed(Consumer<String> listener) {
      zkClosed = listener;
   }

   @Override public void onZkReconnected(Consumer<String> listener) { zkReconnected = listener; }


   private ZooKeeper createNewZookeeper() {
       ZooKeeper zookeeper = null;
       try {
            zookeeper = new ZooKeeper(zkConnect, sessionTimeout, watcher);
            return zookeeper;
        } catch (IOException e) {
            closeZooKeeper(zookeeper);
        }

       return null;
   }


}

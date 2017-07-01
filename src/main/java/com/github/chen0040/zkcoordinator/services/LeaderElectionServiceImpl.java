package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.UTF8;
import com.github.chen0040.zkcoordinator.enums.MasterStates;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Created by xschen on 5/10/16.
 */
public class LeaderElectionServiceImpl implements LeaderElectionService {
   private static final Logger logger = LoggerFactory.getLogger(LeaderElectionServiceImpl.class);
   private ZooKeeper zk;
   private MasterStates state = MasterStates.NOT_ELECTED;
   private int port;
   private String serverId;
   private List<BiConsumer<String, Integer>> leadershipListeners = new ArrayList<>();
   private List<BiConsumer<String, Integer>> resignListeners = new ArrayList<>();
   private final String zkLeaderPath;

   Watcher awaitForLeaderFailedWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
         runForLeader();
      }
   };
   private AsyncCallback.DataCallback callbackQueryLeader = (rc, path, context, datat, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            queryLeader();
            return;
         case NONODE:
            runForLeader();
      }
   };
   AsyncCallback.StatCallback awaitForLeaderFailedCallback = (rc, path, context, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            awaitForLeaderFailed();
            break;
         case OK:
            if (stat == null) {
               state = MasterStates.RUNNING;
               runForLeader();
            }
            break;
         default:
            queryLeader();
            break;
      }
   };
   AsyncCallback.StringCallback callbackRunForLeader = (rc, path, context, name) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            queryLeader();
            return;
         case OK:
            state = MasterStates.ELECTED;
            logger.info(serverId + " is the leader");
            takeLeadership();
            break;
         case NODEEXISTS:
            state = MasterStates.NOT_ELECTED;
            awaitForLeaderFailed();
            break;
         default:
            state = MasterStates.NOT_ELECTED;
            logger.error("Something went wrong when running for master.", KeeperException.create(KeeperException.Code.get(rc), path));
      }
   };

   public LeaderElectionServiceImpl(ZooKeeper zk, String serverId, int port, String zkLeaderPath) {
      this.zk = zk;
      this.serverId = serverId;
      this.port = port;
      this.zkLeaderPath = zkLeaderPath;
   }


   public void addLeadershipListener(BiConsumer<String, Integer> listener) {
      leadershipListeners.add(listener);
   }


   public void addResignListener(BiConsumer<String, Integer> listener) {
      resignListeners.add(listener);
   }


   private void queryLeader() {
      zk.getData(zkLeaderPath, false, callbackQueryLeader, null);
   }


   public void runForLeader() {
      resign();
      zk.create(zkLeaderPath, UTF8.getBytes(serverId), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, callbackRunForLeader, null);
   }


   private void resign() {
      resignListeners.forEach(listener -> listener.accept(serverId, port));
   }


   private void awaitForLeaderFailed() {
      zk.exists(zkLeaderPath, awaitForLeaderFailedWatcher, awaitForLeaderFailedCallback, null);
   }


   private void takeLeadership() {
      awaitForLeaderFailed(); //in case the leader lost connection to zk for a prolonged period and resurface again later when

      leadershipListeners.forEach(listener -> listener.accept(serverId, port));
   }
}

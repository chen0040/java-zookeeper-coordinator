package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.consts.ActorSystemIdentifiers;
import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;
import com.github.chen0040.zkcoordinator.utils.ZkUtils;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


/**
 * Created by xschen on 5/11/16.
 */
public class LeaderServiceImpl implements LeaderService {
   private AkkaNodeUri masterConfig;
   private ZooKeeper zk;

   private final String masterActorSystemIdentifier = ActorSystemIdentifiers.ACTORSYSTEMNAME_MASTERNODE;
   private final String zkLeaderPath;

   private Watcher awaitForLeaderElectedWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeCreated) {
         getLeader();
      }
   };
   private AsyncCallback.StatCallback waitForLeaderElectedCallback = (rc, path, context, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            awaitForLeaderElected();
            break;
         case OK:
            if (stat == null) {
               masterConfig = null;
               awaitForLeaderElected();
            }
            else {
               getLeader();
            }
            break;
         default:
            getLeader();
            break;
      }
   };
   private AsyncCallback.DataCallback getLeaderCallback = (rc, path, context, data, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case OK:
            String leaderInfo = new String(data);
            masterConfig = ZkUtils.toAkkaNodeUri(leaderInfo, masterActorSystemIdentifier);
            awaitForLeaderFailed();
            break;
         case CONNECTIONLOSS:
            getLeader();
            break;
         case NONODE:
            awaitForLeaderElected();
      }
   };
   AsyncCallback.StatCallback awaitForLeaderFailedCallback = (rc, path, context, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            awaitForLeaderFailed();
            break;
         case OK:
            if (stat == null) {
               masterConfig = null;
               awaitForLeaderFailed();
            }
            break;
         default:
            getLeader();
            break;
      }
   };
   Watcher awaitForLeaderFailedWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
         awaitForLeaderElected();
      }
   };

   public LeaderServiceImpl(ZooKeeper zk, String zkLeaderPath) {
      this.zk = zk;
      this.zkLeaderPath = zkLeaderPath;
   }


   public boolean leaderExists() {
      return masterConfig != null;
   }


   public AkkaNodeUri getLeaderUri() {
      return masterConfig;
   }


   public void watchLeader() {
      getLeader();
   }


   private void getLeader() {
      zk.getData(zkLeaderPath, false, getLeaderCallback, null);
   }


   private void awaitForLeaderElected() {
      zk.exists(zkLeaderPath, awaitForLeaderElectedWatcher, waitForLeaderElectedCallback, null);
   }


   private void awaitForLeaderFailed() {
      zk.exists(zkLeaderPath, awaitForLeaderFailedWatcher, awaitForLeaderFailedCallback, null);
   }
}

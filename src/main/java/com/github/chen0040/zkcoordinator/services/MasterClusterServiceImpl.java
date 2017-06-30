package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.consts.ActorSystemIdentifiers;
import com.github.chen0040.zkcoordinator.model.ZkNodePaths;
import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;
import com.github.chen0040.zkcoordinator.utils.ZkUtils;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Created by xschen on 5/10/16.
 */
public class MasterClusterServiceImpl implements MasterClusterService {

   private static final Logger logger = LoggerFactory.getLogger(MasterClusterServiceImpl.class);
   private ZooKeeper zk;

   private List<Consumer<List<AkkaNodeUri>>> masterAddedListeners = new ArrayList<>();
   private List<Consumer<List<AkkaNodeUri>>> masterChangeListeners = new ArrayList<>();

   private Set<String> masters = new HashSet<>();
   private List<AkkaNodeUri> masterUris = new ArrayList<>();

   private String masterActorSystemIdentifier = ActorSystemIdentifiers.ACTORSYSTEMNAME_MASTERNODE;
   private String masterZkNodeIdentifier = ZkNodePaths.Masters;


   private AsyncCallback.ChildrenCallback mastersGetChildrenCallback = (rc, path, context, children) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            getMastersAsync();
            break;
         case OK:
            logger.info("Successfully got a list of masters: {} masters", children.size());
            notifyMasterChanged(children);
            break;
         default:
            logger.error("getMaster failed", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private Watcher mastersChangeWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
         assert masterZkNodeIdentifier.equals(watchedEvent.getPath());
         getMastersAsync();
      }
   };


   public MasterClusterServiceImpl(ZooKeeper zk) {
      this.zk = zk;
   }

   public MasterClusterServiceImpl(ZooKeeper zk, String masterZkNodeIdentifier) {
      this.zk = zk;
      this.masterZkNodeIdentifier = masterZkNodeIdentifier;
   }


   public void addMasterChangeListener(Consumer<List<AkkaNodeUri>> listener) {
      masterChangeListeners.add(listener);
   }


   private void getMastersAsync() {
      zk.getChildren(masterZkNodeIdentifier, mastersChangeWatcher, mastersGetChildrenCallback, null);
   }


   public void watchMasters() {
      getMastersAsync();
   }


   private void notifyMasterChanged(List<String> children) {
      masters = new HashSet<>(children);

      Set<AkkaNodeUri> oldMasterUris = new HashSet<>(masterUris);

      List<String> temp = new ArrayList<>(children);
      temp.sort(String::compareTo);
      masterUris = temp.stream().map(master -> ZkUtils.toAkkaNodeUri(master, masterActorSystemIdentifier)).collect(Collectors.toList());

      masterChangeListeners.forEach(listener -> listener.accept(masterUris));

      List<AkkaNodeUri> newlyAddedMasterUris = new ArrayList<>();
      for(AkkaNodeUri newMaster : masterUris){
         if(!oldMasterUris.contains(newMaster)){
            newlyAddedMasterUris.add(newMaster);
         }
      }

      if(!newlyAddedMasterUris.isEmpty()) {
         masterAddedListeners.forEach(listener -> listener.accept(newlyAddedMasterUris));
      }
   }

   public List<AkkaNodeUri> masters(){
      return masterUris;
   }


   @Override public void addMasterAddedListener(Consumer<List<AkkaNodeUri>> listener) {
      masterAddedListeners.add(listener);
   }


   public boolean masterExists(String masterId) {
      return masters.contains(masterId);
   }

}

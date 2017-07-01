package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.NodeUri;
import com.github.chen0040.zkcoordinator.models.ZkConfig;
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

   private List<Consumer<List<NodeUri>>> masterAddedListeners = new ArrayList<>();
   private List<Consumer<List<NodeUri>>> masterChangeListeners = new ArrayList<>();

   private final Set<String> masters = new HashSet<>();
   private final List<NodeUri> masterUris = new ArrayList<>();

   private final String masterSystemName;
   private final String zkMasterPath;


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
         getMastersAsync();
      }
   };


   public MasterClusterServiceImpl(ZooKeeper zk, ZkConfig zkConfig) {
      this.zk = zk;
      this.zkMasterPath = zkConfig.getMasterPath();
      this.masterSystemName = zkConfig.getMasterSystemName();
   }


   public void addMasterChangeListener(Consumer<List<NodeUri>> listener) {
      masterChangeListeners.add(listener);
   }


   private void getMastersAsync() {
      zk.getChildren(zkMasterPath, mastersChangeWatcher, mastersGetChildrenCallback, null);
   }


   public void watchMasters() {
      getMastersAsync();
   }


   private void notifyMasterChanged(List<String> children) {
      masters.clear();
      masters.addAll(children);

      Set<NodeUri> oldMasterUris = new HashSet<>(masterUris);

      List<String> temp = new ArrayList<>(children);
      temp.sort(String::compareTo);

      masterUris.clear();
      masterUris.addAll(temp.stream().map(master -> ZkUtils.toAkkaNodeUri(master, masterSystemName)).collect(Collectors.toList()));

      masterChangeListeners.forEach(listener -> listener.accept(masterUris));

      List<NodeUri> newlyAddedMasterUris = new ArrayList<>();
      for(NodeUri newMaster : masterUris){
         if(!oldMasterUris.contains(newMaster)){
            newlyAddedMasterUris.add(newMaster);
         }
      }

      if(!newlyAddedMasterUris.isEmpty()) {
         masterAddedListeners.forEach(listener -> listener.accept(newlyAddedMasterUris));
      }
   }

   public List<NodeUri> masters(){
      return masterUris;
   }


   @Override public void addMasterAddedListener(Consumer<List<NodeUri>> listener) {
      masterAddedListeners.add(listener);
   }


   public boolean masterExists(String masterId) {
      return masters.contains(masterId);
   }

}

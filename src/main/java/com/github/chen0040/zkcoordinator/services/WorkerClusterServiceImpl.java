package com.github.chen0040.zkcoordinator.services;


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


/**
 * Created by xschen on 5/10/16.
 */
public class WorkerClusterServiceImpl implements WorkerClusterService {

   private static final Logger logger = LoggerFactory.getLogger(WorkerClusterServiceImpl.class);
   private final ZooKeeper zk;
   private final List<Consumer<List<String>>> workerChangeListeners = new ArrayList<>();
   private final String zkWorkerPath;

   private Set<String> workers = new HashSet<>();
   private AsyncCallback.ChildrenCallback workersGetChildrenCallback = (rc, path, context, children) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            getWorkersAsync();
            break;
         case OK:
            logger.info("Successfully got a list of workers: {} workers", children.size());
            notifyWorkerChanged(children);
            break;
         default:
            logger.error("getWorker failed", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private Watcher workersChangeWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
         getWorkersAsync();
      }
   };

   public WorkerClusterServiceImpl(ZooKeeper zk, String zkWorkerPath) {
      this.zk = zk;
      this.zkWorkerPath = zkWorkerPath;
   }


   public void addWorkerChangeListener(Consumer<List<String>> listener) {
      workerChangeListeners.add(listener);
   }


   private void getWorkersAsync() {
      zk.getChildren(zkWorkerPath, workersChangeWatcher, workersGetChildrenCallback, null);
   }


   public void watchWorkers() {
      getWorkersAsync();
   }


   private void notifyWorkerChanged(List<String> children) {
      workers = new HashSet<>(children);

      logger.info("reporting new worker's clusters: {}", children.size());

      int count = Math.min(20, children.size());
      for(int i=0; i < count; ++i) {
         logger.info("worker: {}", children.get(i));
      }

      workerChangeListeners.forEach(listener -> listener.accept(children));
   }


   public boolean workerExists(String workerId) {
      return workers.contains(workerId);
   }

}

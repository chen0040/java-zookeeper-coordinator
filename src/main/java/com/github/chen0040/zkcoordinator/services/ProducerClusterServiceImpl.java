package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.ZkNodePaths;
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
public class ProducerClusterServiceImpl implements ProducerClusterService {

   private static final Logger logger = LoggerFactory.getLogger(ProducerClusterServiceImpl.class);
   private ZooKeeper zk;
   private List<Consumer<List<String>>> producerChangeListeners = new ArrayList<>();

   private String producerZkNodeIdentifier = ZkNodePaths.Producers;

   private Set<String> producers = new HashSet<>();
   private AsyncCallback.ChildrenCallback producersGetChildrenCallback = (rc, path, context, children) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            getProducersAsync();
            break;
         case OK:
            logger.info("Successfully got a list of producers: {} producers", children.size());
            notifyProducerChanged(children);
            break;
         default:
            logger.error("getProducer failed", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private Watcher producersChangeWatcher = watchedEvent -> {
      if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
         assert producerZkNodeIdentifier.equals(watchedEvent.getPath());
         getProducersAsync();
      }
   };


   public ProducerClusterServiceImpl(ZooKeeper zk) {
      this.zk = zk;
   }

   public ProducerClusterServiceImpl(ZooKeeper zk, String producerZkNodeIdentifier) {
      this.zk = zk;
      this.producerZkNodeIdentifier = producerZkNodeIdentifier;
   }


   public void addProducerChangeListener(Consumer<List<String>> listener) {
      producerChangeListeners.add(listener);
   }


   private void getProducersAsync() {
      zk.getChildren(producerZkNodeIdentifier, producersChangeWatcher, producersGetChildrenCallback, null);
   }


   public void watchProducers() {
      getProducersAsync();
   }


   private void notifyProducerChanged(List<String> children) {
      producers = new HashSet<>(children);

      logger.info("reporting new producer's clusters: {}", children.size());

      int count = Math.min(20, children.size());
      for(int i=0; i < count; ++i) {
         logger.info("producer: {}", children.get(i));
      }

      producerChangeListeners.forEach(listener -> listener.accept(children));
   }


   public boolean producerExists(String producerId) {
      return producers.contains(producerId);
   }

}

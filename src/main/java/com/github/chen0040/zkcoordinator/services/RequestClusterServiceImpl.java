package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.NodeUri;
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
public class RequestClusterServiceImpl implements RequestClusterService {

   private static final Logger logger = LoggerFactory.getLogger(RequestClusterServiceImpl.class);
   private ZooKeeper zk;
   private List<Consumer<List<String>>> producerChangeListeners = new ArrayList<>();

   private final String zkRequestPath;
   private final String requestSystemName;

   private final List<NodeUri> producerUris = new ArrayList<>();

   private final Set<String> producers = new HashSet<>();
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
         getProducersAsync();
      }
   };

   public RequestClusterServiceImpl(ZooKeeper zk, String zkRequestPath, String requestSystemName) {
      this.zk = zk;
      this.zkRequestPath = zkRequestPath;
      this.requestSystemName = requestSystemName;
   }


   public void addProducerChangeListener(Consumer<List<String>> listener) {
      producerChangeListeners.add(listener);
   }


   @Override public List<NodeUri> producers() {
      return producerUris;
   }


   private void getProducersAsync() {
      zk.getChildren(zkRequestPath, producersChangeWatcher, producersGetChildrenCallback, null);
   }


   public void watchProducers() {
      getProducersAsync();
   }


   private void notifyProducerChanged(List<String> children) {
      producers.clear();
      producers.addAll(children);

      logger.info("reporting new producer's clusters: {}", children.size());

      int count = Math.min(20, children.size());
      for(int i=0; i < count; ++i) {
         logger.info("producer: {}", children.get(i));
      }

      producerUris.clear();
      producerUris.addAll(producers.stream().map(producer -> ZkUtils.toAkkaNodeUri(producer, requestSystemName)).collect(Collectors.toList()));

      producerChangeListeners.forEach(listener -> listener.accept(children));
   }


   public boolean producerExists(String producerId) {
      return producers.contains(producerId);
   }

}

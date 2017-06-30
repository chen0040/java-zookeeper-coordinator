package com.github.chen0040.zkcoordinator.utils;


import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * Created by xschen on 9/20/17.
 */
public class ZkController implements Watcher {

   private static final Logger logger = LoggerFactory.getLogger(ZkController.class);

   private ZooKeeper zk;
   private final ZkTimer timer;
   private long reconnectDelayWhenSessionExpired;
   private final AtomicBoolean clientState = new AtomicBoolean(true);

   private String zkConnect;

   private Consumer<ZooKeeper> zkCreated;
   private Consumer<String> zkClosed;

   private int sessionTimeout;
   private String zkSessionId = "";
   private long lastTimeout = 0L;

   public ZkController(String zkConnect, long reconnectDelayWhenSessionExpired) {
      this.reconnectDelayWhenSessionExpired = reconnectDelayWhenSessionExpired;
      this.zkConnect = zkConnect;

      this.timer = ZkTimerFactory.createTimer();
   }

   public ZooKeeper zk(){
      return zk;
   }

   public String getZkSessionId(){
      return zkSessionId;
   }

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
      }

      logger.warn("Execute reconnectWhenSessionExpired()(Expired session:0x{}).", Long.toHexString(zookeeper.getSessionId()));

      closeZooKeeper(zookeeper);

      try {
         start(this.sessionTimeout);
      }
      catch (IOException e) {
         logger.error("Failed to start zk", e);
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


   public void start(int sessionTimeout) throws IOException {
      this.sessionTimeout = sessionTimeout;
      ZooKeeper newZookeeper = createNewZookeeper();
      if (newZookeeper == null) {
         logger.warn("Failed to create new Zookeeper instance. It will be retry  after {} ms.", reconnectDelayWhenSessionExpired);
      } else {
         this.zk = newZookeeper;
         if(zkCreated != null) {
            zkCreated.accept(newZookeeper);
         }
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


   public void stopZk() throws InterruptedException {
      if (clientState.compareAndSet(true, false)) {
         if (timer != null) {
            timer.stop();
         }
         closeZooKeeper(zk);
      }
   }


   public void onZkClosed(Consumer<String> listener) {
      zkClosed = listener;
   }

   public void onZkStarted(Consumer<ZooKeeper> listener){
      zkCreated = listener;
   }


   private ZooKeeper createNewZookeeper() {
      ZooKeeper zookeeper = null;
      try {
         zookeeper = new ZooKeeper(zkConnect, sessionTimeout, this);
         return zookeeper;
      } catch (IOException e) {
         closeZooKeeper(zookeeper);
      }

      return null;
   }


   @Override public void process(WatchedEvent watchedEvent) {

   }
}

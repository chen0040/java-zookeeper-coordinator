package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.RegistrationCompleted;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public interface RegistrationService {
   String getZkSessionId();

   void addGroupJoinListener(BiConsumer<ZooKeeper, RegistrationCompleted> listener);

   void onZkStarted(Consumer<ZooKeeper> listener);

   long getLastTimeout();

   void start(int sessionTimeout, int initialPort) throws IOException;

   void stopZk() throws InterruptedException;

   void onZkClosed(Consumer<String> listener);

   void onZkReconnected(Consumer<String> listener);

}

package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.utils.IpTools;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;


/**
 * Created by xschen on 9/9/2016.
 */
public class ZooKeeperConfigurationContext implements Watcher {
   protected TestingServer zkTestServer;
   protected int zkPort = 9901;


   public void startZkServer() throws Exception {

      if(!IpTools.isPortAvailable(zkPort)){
         zkPort = IpTools.getNextAvailablePort(zkPort);
      }

      zkTestServer = new TestingServer(zkPort);
      //zkPort = zkTestServer.getPort();

      Thread.sleep(5000);


   }


   public void stopZkServer() throws IOException {
      zkTestServer.stop();
   }

   @BeforeClass public void setUp() throws Exception {
      startZkServer();
   }


   @AfterClass public void tearDown() throws Exception {
      stopZkServer();
   }


   @Override public void process(WatchedEvent watchedEvent) {

   }
}

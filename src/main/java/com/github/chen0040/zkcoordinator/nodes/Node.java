package com.github.chen0040.zkcoordinator.nodes;


/**
 * Created by xschen on 1/7/2017.
 */
public interface Node {
   // System usually stops when the master is shutdown
   void stopSystem();

   // System usually starts when the master node joins the zookeeper (can happen multiple times as master disconnect and reconnects to zookeeper)
   void startSystem(String ipAddress, int port, String masterId);
}

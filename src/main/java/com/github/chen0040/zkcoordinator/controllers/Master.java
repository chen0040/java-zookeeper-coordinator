package com.github.chen0040.zkcoordinator.controllers;


/**
 * Created by xschen on 1/7/2017.
 */
public interface Master {
   // System takes leader ship when it becomes a leader in the zookeeper cluster
   void takeLeadership(String ipAddress, int port, String masterId);

   // System resigns leadership when it tries reconnect to zookeeper (either never connect or disconnect previously)
   void resignLeadership(String ipAddress, int port, String masterId);

   // System usually stops when the master is shutdown
   void stopSystem();

   // System usually starts when the master node joins the zookeeper (can happen multiple times as master disconnect and reconnects to zookeeper)
   void startSystem(String ipAddress, int port, String masterId);
}

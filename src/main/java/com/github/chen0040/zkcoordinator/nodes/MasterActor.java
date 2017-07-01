package com.github.chen0040.zkcoordinator.nodes;


/**
 * Created by xschen on 1/7/2017.
 */
public interface MasterActor extends SystemActor {
   // System takes leader ship when it becomes a leader in the zookeeper cluster
   void takeLeadership(String ipAddress, int port, String masterId);

   // System resigns leadership when it tries reconnect to zookeeper (either never connect or disconnect previously)
   void resignLeadership(String ipAddress, int port, String masterId);
}

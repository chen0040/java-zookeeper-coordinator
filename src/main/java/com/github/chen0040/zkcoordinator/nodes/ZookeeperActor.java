package com.github.chen0040.zkcoordinator.nodes;


import java.io.IOException;


/**
 * Created by xschen on 1/7/2017.
 */
public interface ZookeeperActor {
   void start() throws IOException;
   void shutdown() throws InterruptedException;
}

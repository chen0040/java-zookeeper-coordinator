package com.github.chen0040.zkcoordinator.nodes;


import java.io.IOException;


/**
 * Created by xschen on 1/7/2017.
 */
public interface ZookeeperActor {

   void start() throws IOException;
   void shutdown() throws InterruptedException;

   default void addShutdownHook(){
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         try {
            this.shutdown();
         }
         catch (InterruptedException exception) {
            exception.printStackTrace();
         }
      }));
   }

   default void runForever() {
      try {
         start();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      while (true) {
         try {
            Thread.sleep(1000);
         }
         catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}

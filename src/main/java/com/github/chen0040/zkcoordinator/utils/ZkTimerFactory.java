package com.github.chen0040.zkcoordinator.utils;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Created by xschen on 8/26/17.
 */
public class ZkTimerFactory {
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


   public static ZkTimer createTimer() {
      return new ZkTimer(scheduler);
   }


}

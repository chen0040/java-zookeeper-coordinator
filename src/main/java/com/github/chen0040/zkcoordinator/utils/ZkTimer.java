package com.github.chen0040.zkcoordinator.utils;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Created by xschen on 21/9/2016.
 */
public class ZkTimer {

   private ScheduledExecutorService scheduler;
   private Map<String, TimeOut> timeOuts = new ConcurrentHashMap<>();

   public ZkTimer(ScheduledExecutorService scheduler) {
      this.scheduler = scheduler;
   }


   public TimeOut newTimeout(Consumer<TimeOut> action, long delay, TimeUnit timeUnit){
      final TimeOut timeOut = new TimeOut();
      timeOuts.put(timeOut.getId(), timeOut);

      scheduler.schedule(() -> {
         if(timeOuts.containsKey(timeOut.getId())) {
            timeOuts.remove(timeOut.getId());
            action.accept(timeOut);
         }
      }, delay, timeUnit);

      return timeOut;
   }


   public void stop() {
      List<TimeOut> result = timeOuts.values().stream().collect(Collectors.toList());
      result.forEach(TimeOut::cancel);
      timeOuts.clear();
   }


   public int remainingTimeoutCount() {
      return timeOuts.size();
   }
}

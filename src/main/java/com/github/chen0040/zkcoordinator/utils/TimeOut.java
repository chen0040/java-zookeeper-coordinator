package com.github.chen0040.zkcoordinator.utils;


import java.io.Serializable;
import java.util.UUID;


/**
 * Created by xschen on 21/9/2016.
 */
public class TimeOut implements Serializable {
   private static final long serialVersionUID = -3521885578638780173L;
   private boolean cancelled;
   private String id;

   public TimeOut(){
      cancelled = false;
      id = UUID.randomUUID().toString();
   }

   public boolean isCancelled(){
      return cancelled;
   }


   public void cancel() {
      cancelled = true;
   }

   public String getId(){
      return id;
   }
}

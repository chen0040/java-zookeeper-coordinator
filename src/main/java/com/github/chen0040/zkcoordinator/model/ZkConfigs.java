package com.github.chen0040.zkcoordinator.model;


import java.io.Serializable;


/**
 * Created by xschen on 8/26/16.
 */
public class ZkConfigs implements Serializable {

   public static final long reconnectDelayWhenSessionExpired = 180000;
   public static final int sessionTimeout = 15000;
   private static final long serialVersionUID = -4252079975890701023L;
}

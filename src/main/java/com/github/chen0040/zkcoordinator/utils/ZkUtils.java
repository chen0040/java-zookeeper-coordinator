package com.github.chen0040.zkcoordinator.utils;


import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;

import java.io.Serializable;


/**
 * Created by xschen on 6/9/2016.
 */
public class ZkUtils implements Serializable {
   private static final long serialVersionUID = 8753707255122523807L;

   public static AkkaNodeUri toAkkaNodeUri(String leaderInfo, String actorSystemName){

      String[] leaderInfoParts = leaderInfo.split("_");
      String leaderIp = leaderInfoParts[0];
      String leaderPortString = leaderInfoParts[1];
      int leaderPort = Integer.parseInt(leaderPortString);
      AkkaNodeUri masterConfig = new AkkaNodeUri();
      masterConfig.setHost(leaderIp);
      masterConfig.setMessage(leaderInfo);
      masterConfig.setSystem(actorSystemName);
      masterConfig.setPort(leaderPort);

      return masterConfig;
   }
}

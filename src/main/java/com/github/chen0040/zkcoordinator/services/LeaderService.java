package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;


/**
 * Created by xschen on 5/11/16.
 */
public interface LeaderService {
   boolean leaderExists();

   AkkaNodeUri getLeaderUri();

   void watchLeader();
}

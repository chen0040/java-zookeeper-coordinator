package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.NodeUri;


/**
 * Created by xschen on 5/11/16.
 */
public interface LeaderWatchService {
   boolean leaderExists();

   NodeUri getLeaderUri();

   void watchLeader();
}

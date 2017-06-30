package com.github.chen0040.zkcoordinator.services;


import java.util.function.BiConsumer;


/**
 * Created by xschen on 5/10/16.
 */
public interface LeaderElectionService {
   void addLeadershipListener(BiConsumer<String, Integer> listener);

   void addResignListener(BiConsumer<String, Integer> listener);

   void runForLeader();
}

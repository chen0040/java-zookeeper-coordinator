package com.github.chen0040.zkcoordinator;


import java.util.function.BiConsumer;


/**
 * Created by xschen on 5/10/17.
 */
public interface LeaderElectionService {
   void addLeadershipListener(BiConsumer<String, Integer> listener);

   void addResignListener(BiConsumer<String, Integer> listener);

   void runForLeader();
}

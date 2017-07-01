package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.ZkConfig;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import com.github.chen0040.zkcoordinator.model.RegistrationCompleted;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertTrue;


/**
 * Created by xschen on 9/9/2016.
 */
public class LeaderWatchServiceImplUnitTest extends ZooKeeperConfigurationContext {

   private RegistrationService registrationService;
   private ZooKeeper zkClient;
   private long reconnectDelayWhenSessionExpired = 20000;
   private String zkConnect = "localhost:" + zkPort;
   private String groupName = "groupName";
   private RegistrationCompleted registrationCompleted = null;
   private BootstrapService bootstrapService;
   private LeaderWatchService leaderWatchService;
   private LeaderElectionService leaderElectionService;
   private String leaderServerId;
   private int leaderPort;

   private ZkConfig zkConfig = new ZkConfig();

   private static final Logger logger = LoggerFactory.getLogger(LeaderWatchServiceImplUnitTest.class);

   @BeforeMethod @Override public void setUp() throws Exception {
      super.setUp();

      Thread.sleep(5000);

      zkConnect = IpTools.getIpAddress() + ":" + zkPort;
      groupName = "masters";

      registrationService = new RegistrationServiceImpl(this, zkConnect, zkConfig.getRootPath(), zkConfig.getNodePath(), groupName, IpTools.getIpAddress(), reconnectDelayWhenSessionExpired);
      registrationService.onZkStarted(zk -> {
         zkClient = zk;

         bootstrapService = new BootstrapServiceImpl(zk, zkConfig);
         bootstrapService.bootstrap();


      });
      registrationService.onZkClosed(message -> zkClient = null);
      registrationService.addGroupJoinListener((zk, rc) -> {
         logger.info("group join success");
         registrationCompleted = rc;

         leaderWatchService = new LeaderWatchServiceImpl(zk, zkConfig);
         leaderWatchService.watchLeader();

         leaderElectionService = new LeaderElectionServiceImpl(zk, rc.getServerId(), rc.getPort(), zkConfig.getLeaderPath());
         leaderElectionService.addLeadershipListener((serverId, port) -> {
            logger.info("Take leadership");
            leaderPort = port;
            leaderServerId = serverId;
         });
         leaderElectionService.runForLeader();
      });
   }


   @AfterMethod @Override public void tearDown() throws Exception {
      super.tearDown();
   }

   @Test
   public void test_start_stopZk_checkLeaderExists() throws IOException, InterruptedException {
      registrationService.start(5000, 9901);

      Thread.sleep(10000);

      assertThat(zkClient, is(notNullValue()));
      assertThat(registrationCompleted, is(notNullValue()));
      assertTrue(IpTools.isPortAvailable(registrationCompleted.getPort()));

      assertThat(leaderWatchService, is(notNullValue()));
      assertTrue(leaderWatchService.leaderExists());

      assertThat(registrationCompleted.getServerId(), is(leaderServerId));
      assertThat(registrationCompleted.getPort(), is(leaderPort));
      assertThat(leaderWatchService.getLeaderUri().getHost(), is(IpTools.getIpAddress()));
      assertThat(leaderWatchService.getLeaderUri().getPort(), is(leaderPort));

      registrationService.stopZk();

      Thread.sleep(1000);

      assertThat(zkClient, is(nullValue()));
   }
}

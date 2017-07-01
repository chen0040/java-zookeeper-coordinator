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
public class LeaderElectionServiceImplUnitTest extends ZooKeeperConfigurationContext {

   private RegistrationService registrationService;
   private ZooKeeper zkClient;
   private long reconnectDelayWhenSessionExpired = 20000;
   private String zkConnect = "localhost:" + zkPort;
   private String groupName = "groupName";
   private RegistrationCompleted registrationCompleted = null;
   private BootstrapService bootstrapService;
   private MasterClusterService masterClusterService;

   private ZkConfig zkConfig = new ZkConfig();

   private static final Logger logger = LoggerFactory.getLogger(LeaderElectionServiceImplUnitTest.class);

   @BeforeMethod @Override public void setUp() throws Exception {
      super.setUp();

      Thread.sleep(5000);

      zkConnect = IpTools.getIpAddress() + ":" + zkPort;
      groupName = "masters";

      registrationService = new RegistrationServiceImpl(this, zkConnect, zkConfig, groupName, IpTools.getIpAddress());
      registrationService.onZkStarted(zk -> {
         zkClient = zk;

         bootstrapService = new BootstrapServiceImpl(zk, zkConfig);
         bootstrapService.bootstrap();
      });
      registrationService.onZkClosed(message -> zkClient = null);
      registrationService.addGroupJoinListener((zk, rc) -> {
         logger.info("group join success");
         registrationCompleted = rc;
         masterClusterService = new MasterClusterServiceImpl(zk, zkConfig);
         masterClusterService.watchMasters();
      });
   }


   @AfterMethod @Override public void tearDown() throws Exception {
      super.tearDown();
   }

   @Test
   public void test_start_stopZk_successOnWatchMasters() throws IOException, InterruptedException {
      registrationService.start(5000, 9901);

      Thread.sleep(10000);

      assertThat(zkClient, is(notNullValue()));
      assertThat(registrationCompleted, is(notNullValue()));
      assertTrue(IpTools.isPortAvailable(registrationCompleted.getPort()));

      assertThat(masterClusterService, is(notNullValue()));
      assertThat(masterClusterService.masters(), hasSize(1));

      registrationService.stopZk();

      Thread.sleep(1000);

      assertThat(zkClient, is(nullValue()));
   }
}

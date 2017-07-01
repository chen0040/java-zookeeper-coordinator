package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.utils.IpTools;
import com.github.chen0040.zkcoordinator.models.RegistrationCompleted;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Created by xschen on 9/9/2016.
 */
public class RegistrationServiceImplUnitTest extends ZooKeeperConfigurationContext {

   private RegistrationService registrationService;
   private ZooKeeper zkClient;
   private long reconnectDelayWhenSessionExpired = 10000;
   private String zkConnect = "localhost:" + zkPort;
   private String groupName = "groupName";
   private RegistrationCompleted registrationCompleted = null;
   private BootstrapService bootstrapService;

   private ZkConfig zkConfig = new ZkConfig();

   private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImplUnitTest.class);

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
      });
   }


   @AfterMethod @Override public void tearDown() throws Exception {
      super.tearDown();
   }

   @Test
   public void test_start_stopZk_successOnRegistration() throws IOException, InterruptedException {
      registrationService.start(5000, 9901);

      Thread.sleep(10000);

      assertThat(zkClient, is(notNullValue()));
      assertThat(registrationCompleted, is(notNullValue()));

      logger.info("register port: {}", registrationCompleted.getPort());
      for(int i=0; i < 4; ++i){
         Thread.sleep(10000);
         logger.info("lastTimeout: {}", new Date(registrationService.getLastTimeout()));
      }

      registrationService.stopZk();

      Thread.sleep(1000);

      assertThat(zkClient, is(nullValue()));
   }
}

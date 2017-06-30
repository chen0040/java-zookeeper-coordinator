package com.github.chen0040.zkcoordinator.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;


/**
 * Created by xschen on 30/6/2017.
 */
public class ZkTimerUnitTest {

   private static final Logger logger = LoggerFactory.getLogger(ZkTimerUnitTest.class);
   private int count = 0;

   private ZkTimer timer;
   @BeforeMethod public void setUp() throws Exception {
      timer = ZkTimerFactory.createTimer();

      count = 0;
   }

   private void runNextTimeout(){

      count++;
      logger.info("timeout: {} ({})", count, timer.remainingTimeoutCount());

      timer.newTimeout(timeOut -> {
         assertFalse(timeOut.isCancelled());
         runNextTimeout();
      }, 5000, TimeUnit.MILLISECONDS);
   }


   @Test public void testNewTimeout() throws Exception {
      timer.newTimeout(timeOut -> {
         runNextTimeout();
      }, 5000, TimeUnit.MILLISECONDS);

      Thread.sleep(40010);

      timer.stop();
      logger.info("timer stopped");

      assertThat(count).isEqualTo(8);

      Thread.sleep(20000);
      logger.info("method exit");

      assertThat(count).isEqualTo(8);
   }

}

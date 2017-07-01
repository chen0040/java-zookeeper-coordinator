package com.github.chen0040.zkcoordinator.samples;


import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.nodes.MasterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Created by xschen on 1/7/2017.
 */
public class MasterApplication extends MasterNode {

   private static final Logger logger = LoggerFactory.getLogger(MasterApplication.class);

   public MasterApplication(ZkConfig zkConfig) {
      super(zkConfig);
   }

   @Override public void takeLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has become leader at {}:{}", masterId, ipAddress, port);
   }

   @Override public void resignLeadership(String ipAddress, int port, String masterId) {
      logger.info("This instance (id = {}) has resigned from leadership at {}:{}", masterId, ipAddress, port);
   }

   @Override public void startSystem(String ipAddress, int port, String masterId){
      logger.info("start system at {}:{} with id = {}", ipAddress, port, masterId);
   }

   @Override public void stopSystem() {
      logger.info("system shutdown");
   }

   public static void main(String[] args) throws IOException, InterruptedException {
      ZkConfig config = new ZkConfig();
      config.setZkConnect("192.168.10.12:2181,192.168.10.13:2181,192.168.10.14:2181");
      final MasterApplication application = new MasterApplication(config);
      application.addShutdownHook();
      application.runForever();
   }

}

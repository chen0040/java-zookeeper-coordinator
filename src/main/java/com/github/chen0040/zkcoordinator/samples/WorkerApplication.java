package com.github.chen0040.zkcoordinator.samples;


import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.nodes.MasterNode;
import com.github.chen0040.zkcoordinator.nodes.WorkerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Created by xschen on 1/7/2017.
 */
public class WorkerApplication extends WorkerNode {

   private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

   public WorkerApplication(ZkConfig zkConfig) {
      super(zkConfig);
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
      final WorkerApplication application = new WorkerApplication(config);
      application.addShutdownHook();
      application.runForever();
   }

}

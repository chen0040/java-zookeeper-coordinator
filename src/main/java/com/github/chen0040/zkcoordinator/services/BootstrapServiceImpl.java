package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.ZkConfig;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by xschen on 5/10/16.
 */
public class BootstrapServiceImpl implements BootstrapService {

   private static final Logger logger = LoggerFactory.getLogger(BootstrapServiceImpl.class);
   private ZooKeeper zk;
   private final ZkConfig paths;

   AsyncCallback.StringCallback createParentCallback = (rc, path, context, name) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            createParent(path, (byte[]) context);
            break;
         case OK:
            logger.info("parent created");
            break;
         case NODEEXISTS:
            //logger.info("Parent already registered: " + path);
            break;
         default:
            logger.error("Something went wrong: ", KeeperException.create(KeeperException.Code.get(rc), path));
      }
   };


   public BootstrapServiceImpl(ZooKeeper zk, ZkConfig paths) {
      this.zk = zk;
      this.paths = paths;
   }


   public void bootstrap() {

      List<String> paths = this.paths.getAllPaths();

      for(String p : paths){
         createParent(p, new byte[0]);
      }
   }


   private void createParent(String path, byte[] data) {
      zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createParentCallback, data);
   }
}

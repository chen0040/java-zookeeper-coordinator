package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.ZkNodePaths;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by xschen on 5/10/16.
 */
public class BootstrapServiceImpl implements BootstrapService {

   private static final Logger logger = LoggerFactory.getLogger(BootstrapServiceImpl.class);
   private ZooKeeper zk;
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


   public BootstrapServiceImpl(ZooKeeper zk) {
      this.zk = zk;
   }


   public void bootstrap() {

      createParent(ZkNodePaths.Root, new byte[0]);
      createParent(ZkNodePaths.Nodes, new byte[0]);
      createParent(ZkNodePaths.Masters, new byte[0]);
      createParent(ZkNodePaths.Workers, new byte[0]);
      createParent(ZkNodePaths.Producers, new byte[0]);
      createParent(ZkNodePaths.Tasks, new byte[0]);
      createParent(ZkNodePaths.Corr, new byte[0]);

      createParent(ZkNodePaths.DevMasters, new byte[0]);
      createParent(ZkNodePaths.DevWorkers, new byte[0]);
      createParent(ZkNodePaths.DevReaders, new byte[0]);
      createParent(ZkNodePaths.DevTasks, new byte[0]);

      createParent(ZkNodePaths.TaskAssignments, new byte[0]);
      createParent(ZkNodePaths.DevTaskAssignments, new byte[0]);
   }


   private void createParent(String path, byte[] data) {
      zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createParentCallback, data);
   }
}

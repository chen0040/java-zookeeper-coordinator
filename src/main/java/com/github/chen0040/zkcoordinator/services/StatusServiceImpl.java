package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.UTF8;
import com.github.chen0040.zkcoordinator.consts.ZkNodePaths;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;


/**
 * Created by xschen on 5/11/16.
 */
public class StatusServiceImpl implements StatusService {
   private ZooKeeper zk;
   private String status;
   private String groupName;
   private String serverId;
   AsyncCallback.StatCallback callback = (rc, path, context, stat) -> {
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            updateStatus((String) context);
      }
   };


   public StatusServiceImpl(ZooKeeper zk, String groupName, String serverId) {
      this.zk = zk;
      this.groupName = groupName;
      this.serverId = serverId;
   }


   synchronized private void updateStatus(String status) {
      if (status != null && status.equals(this.status)) {
         this.status = status;
         zk.setData(ZkNodePaths.Root + "/" + groupName + "/" + serverId, UTF8.getBytes(status), -1, callback, status);
      }
   }

}

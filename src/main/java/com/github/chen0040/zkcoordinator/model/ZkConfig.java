package com.github.chen0040.zkcoordinator.model;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 5/12/16.
 */
@Getter
@Setter
public class ZkConfig {
   private String rootPathId = "zk-coordinator";
   private String workerGroupName = "workers";
   private String nodePathId = "nodes";
   private String masterGroupName = "masters";
   private String leaderPathId = "leader";
   private String taskGroupName = "tasks";
   private String taskAssignmentPathId = "assign";
   private String requestGroupName = "requests";
   private String corrPathId = "corr";

   private String workerSystemName = "WorkerActorSystem";
   private String masterSystemName = "MasterActorSystem";

   private String zkConnect = "";
   private int initialPort = 9000;

   private long reconnectDelayWhenSessionExpired = 180000;
   private int sessionTimeout = 15000;

   public String getRootPath(){
      return "/" + rootPathId;
   }

   public String getWorkerPath(){
      return "/" + rootPathId + "/" + workerGroupName;
   }

   public String getNodePath(){
      return "/" + rootPathId + "/" + nodePathId;
   }

   public String getRequestPath() {
      return "/" + rootPathId + "/" + requestGroupName;
   }

   public String getMasterPath() {
      return "/" + rootPathId + "/" + masterGroupName;
   }

   public String getLeaderPath(){
      return "/" + rootPathId + "/" + leaderPathId;
   }
   
   public List<String> getAllPaths(){
      List<String> result = new ArrayList<>();
      result.add(getRootPath());
      result.add(getNodePath());
      result.add(getMasterPath());
      result.add(getWorkerPath());
      result.add(getRequestPath());
      result.add(getTaskPath());
      result.add(getCorrPath());

      result.add(getTaskAssignmentPath());

      return result;
   }

   public String getCorrPath() {
      return "/" + rootPathId + "/" + corrPathId;
   }

   public String getTaskPath() {
      return "/" + rootPathId + "/" + taskGroupName;
   }

   public String getTaskAssignmentPath(){
      return "/" + rootPathId + "/" + taskAssignmentPathId;
   }
}

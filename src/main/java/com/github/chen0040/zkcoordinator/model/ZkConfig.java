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
   private String workerPathId = "workers";
   private String nodePathId = "nodes";
   private String masterPathId = "masters";
   private String leaderPathId = "leader";
   private String taskPathId = "tasks";
   private String taskAssignmentPathId = "assign";
   private String requestPathId = "requests";
   private String corrPathId = "corr";

   private String workerSystemName = "WorkerActorSystem";
   private String masterSystemName = "MasterActorSystem";


   private long reconnectDelayWhenSessionExpired = 180000;
   private int sessionTimeout = 15000;

   public String getRootPath(){
      return "/" + rootPathId;
   }

   public String getWorkerPath(){
      return "/" + rootPathId + "/" + workerPathId;
   }

   public String getNodePath(){
      return "/" + rootPathId + "/" + nodePathId;
   }

   public String getRequestPath() {
      return "/" + rootPathId + "/" + requestPathId;
   }

   public String getMasterPath() {
      return "/" + rootPathId + "/" + masterPathId;
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
      return "/" + rootPathId + "/" + taskPathId;
   }

   public String getTaskAssignmentPath(){
      return "/" + rootPathId + "/" + taskAssignmentPathId;
   }
}

package com.github.chen0040.zkcoordinator.model;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 5/12/16.
 */
public class ZkConfig {
   private static final String Root = "/zk-coordinator";
   private static final String Workers = "/zk-coordinator/workers";
   private static final String Nodes = "/zk-coordinator/nodes";
   private static final String Masters = "/zk-coordinator/masters";
   private static final String Leader = "/zk-coordinator/leader";
   private static final String Tasks = "/zk-coordinator/tasks";
   private static final String TaskAssignments = "/zk-coordinator/assign";
   private static final String Requests = "/zk-coordinator/requests";
   private static final String Corr = "/zk-coordinator/corr";

   @Getter
   @Setter
   private long reconnectDelayWhenSessionExpired = 180000;

   @Getter
   @Setter
   private int sessionTimeout = 15000;

   public String getRootPath(){
      return Root;
   }

   public String getWorkerPath(){
      return Workers;
   }

   public String getNodePath(){
      return Nodes;
   }

   public String getRequestPath() {
      return Requests;
   }

   public String getMasterPath() {
      return Masters;
   }

   public String getLeaderPath(){
      return Leader;
   }
   
   public List<String> getAllPaths(){
      List<String> result = new ArrayList<>();
      result.add(ZkConfig.Root);
      result.add(ZkConfig.Nodes);
      result.add(ZkConfig.Masters);
      result.add(ZkConfig.Workers);
      result.add(ZkConfig.Requests);
      result.add(ZkConfig.Tasks);
      result.add(ZkConfig.Corr);

      result.add(ZkConfig.TaskAssignments);

      return result;
   }

   public String getTaskPath() {
      return Tasks;
   }

   public String getTaskAssignmentPath(){
      return TaskAssignments;
   }
}

package com.github.chen0040.zkcoordinator.model;


/**
 * Created by xschen on 5/12/16.
 */
public class ZkNodePaths {
   public static final String Root = "/zk-coordinator";
   public static final String Workers = "/zk-coordinator/workers";
   public static final String Nodes = "/zk-coordinator/nodes";
   public static final String Masters = "/zk-coordinator/masters";
   public static final String Leader = "/zk-coordinator/leader";
   public static final String Tasks = "/zk-coordinator/tasks";
   public static final String TaskAssignments = "/zk-coordinator/assign";
   public static final String Producers = "/zk-coordinator/producers";
   public static final String Corr = "/zk-coordinator/corr";

   public static final String DevMasters = "/zk-coordinator/dev_masters";
   public static final String DevReaders = "/zk-coordinator/dev_readers";
   public static final String DevWorkers = "/zk-coordinator/dev_workers";
   public static final String DevLeader = "/zk-coordinator/dev_leader";
   public static final String DevTasks = "/zk-coordinator/dev_tasks";
   public static final String DevTaskAssignments = "/zk-coordinator/dev_assign";
}

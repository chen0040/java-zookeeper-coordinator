package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.data.utils.TupleTwo;
import com.github.chen0040.zkcoordinator.consts.TaskStates;
import com.github.chen0040.zkcoordinator.consts.UTF8;
import com.github.chen0040.zkcoordinator.models.*;
import com.github.chen0040.zkcoordinator.utils.TupleThree;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public class TaskAssignmentServiceImpl implements TaskAssignmentService {
   private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentServiceImpl.class);
   private static Random rand = new Random();
   private final List<String> workerList = new ArrayList<>();
   private final ZooKeeper zk;

   private final String zkTaskPath;
   private final String zkTaskAssignmentPath;
   private final String workerSystemName;

   private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   AsyncCallback.StringCallback createTaskCallback = (rc, path, context, name) -> {
      TupleTwo<String, Consumer<String>> tuple2 = (TupleTwo<String, Consumer<String>>) context;

      String taskId = tuple2._1();
      Consumer<String> callback = tuple2._2();

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            createTask(path, taskId, callback);
            break;
         case OK:
            callback.accept(taskId);
            break;
         case NODEEXISTS:
            callback.accept(taskId);
            break;
         default:
            logger.error("Something went wrong: ", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private ChildrenCache workersCache;
   private int zkDepth4Tasks = 4;
   AsyncCallback.DataCallback isTaskAssignedCallback = (rc, path, context, data, stat) -> {
      BiConsumer<String, Boolean> callback = (BiConsumer<String, Boolean>) context;
      String tsId = path.substring(path.lastIndexOf("/") + 1);

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            isTaskAssigned(tsId, callback);
            break;
         case OK:
            String state = new String(data);
            callback.accept(tsId, !state.equals(TaskStates.Idle));
            break;
         default:
            logger.error("Something went wrong", KeeperException.create(KeeperException.Code.get(rc)), path);
            break;
      }
   };
   AsyncCallback.DataCallback taskExistsCallback = (rc, path, context, data, stat) -> {
      BiConsumer<String, Boolean> callback = (BiConsumer<String, Boolean>) context;
      String tsId = path.substring(path.lastIndexOf("/") + 1);

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            taskExists(tsId, callback);
            break;
         case OK:
            callback.accept(tsId, true);
            break;
         case NONODE:
            callback.accept(tsId, false);
            break;
         default:
            logger.error("Something went wrong", KeeperException.create(KeeperException.Code.get(rc)), path);
            break;
      }
   };
   AsyncCallback.DataCallback getWorkerAssigned2TaskCallback = (rc, path, context, data, stat) -> {
      Consumer<NodeUri> callback = (Consumer<NodeUri>) context;
      String taskId = path.substring(path.lastIndexOf("/") + 1);
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            getWorkerAssigned2Task(taskId, callback);
            break;
         case OK:
            String workerId = new String(data);
            NodeUri worker = getUri(workerId);
            callback.accept(worker);
            break;
      }
   };
   private AsyncCallback.VoidCallback unregisterAbsentWorkerInAssignmentCallback = (rc, path, context) -> {
      String workerId = (String) context;
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            unregisterAbsentWorkerInAssignment(workerId);
            break;
         case OK:
            logger.info("Worker unregistered from task assignment: " + workerId);
            break;
         default:
            logger.error("Error when trying to unregisterAbsentWorkerInAssignment: " + workerId,
                    KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private AsyncCallback.DataCallback isWorkerRegisteredInAssignmentCallback = (rc, path, context, data, stat) -> {
      String workerId = path.substring(path.lastIndexOf("/") + 1);
      BiConsumer<String, Boolean> callback = (BiConsumer<String, Boolean>) context;
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            isWorkerRegisteredInAssignment(workerId, callback);
            break;
         case OK:
            callback.accept(workerId, true);
            break;
         case NONODE:
            callback.accept(workerId, false);
            break;
         default:
            logger.error("isWorkerRegisteredInAssignment failed.", KeeperException.create(KeeperException.Code.get(rc)), path);
      }
   };
   private AsyncCallback.StringCallback registerNewWorkerInAssignmentCallback = (rc, path, context, name) -> {
      String workerId = path.substring(path.lastIndexOf("/") + 1);
      Consumer<String> callback = (Consumer<String>) context;
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            registerNewWorkerInAssignment(workerId, callback);
            break;
         case OK:
            callback.accept(workerId);
            break;
         case NODEEXISTS:
            callback.accept(workerId);
            break;
         default:
            logger.error("Something went wrong.", KeeperException.create(KeeperException.Code.get(rc)), path);
            break;
      }

   };
   private AsyncCallback.StringCallback createAssignmentCallback = (rc, path, context, name) -> {
      TupleThree<String, String, BiConsumer<String, NodeUri>> tuple3 = (TupleThree<String, String, BiConsumer<String, NodeUri>>) context;
      String taskId = tuple3._1();
      String workerId = tuple3._2();
      BiConsumer<String, NodeUri> callback = tuple3._3();

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            createAssignment(path, taskId, workerId, callback);
            break;
         case OK:
            logger.info("Task assigned correctly: " + name);
            callback.accept(taskId, getUri(workerId));
            break;
         case NODEEXISTS:
            callback.accept(taskId, getUri(workerId));
            break;
         default:
            logger.error("createAssignment failed.", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private AsyncCallback.VoidCallback deleteTaskCallback = (rc, path, context) -> {
      String taskId = (String) context;
      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            deleteTask(taskId);
            break;
         case OK:
            logger.info("Task deleted: " + taskId);
            break;
         default:
            logger.error("Error when trying to delete task.", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private AsyncCallback.StatCallback updateTaskHandlerCallback = (rc, path, context, stat) -> {

      TupleTwo<String, BiConsumer<String, NodeUri>> tuple2 = (TupleTwo<String, BiConsumer<String, NodeUri>>) context;

      String workerId = tuple2._1();
      BiConsumer<String, NodeUri> callback = tuple2._2();

      String taskId = path.substring(path.lastIndexOf("/") + 1);

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            updateTaskHandler(path, workerId, callback);
            break;
         case OK:
            logger.info("Task handler updated: {} = {}", path, workerId);
            callback.accept(taskId, getUri(workerId));
            break;
         default:
            logger.error("Something went wrong: ", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private AsyncCallback.DataCallback assignTaskCallback = (rc, path, context, data, stat) -> {

      final String taskId = path.substring(path.lastIndexOf("/") + 1);
      final BiConsumer<String, NodeUri> callback = (BiConsumer<String, NodeUri>) context;

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            assignTask(taskId, callback);
            break;
         case OK:
            String designatedWorker = null;
            readWriteLock.readLock().lock();
            try {
               if (!workerList.isEmpty()) {
                  int worker = rand.nextInt(workerList.size());
                  designatedWorker = workerList.get(worker);
               }
            } finally {
               readWriteLock.readLock().unlock();
            }
            if(designatedWorker != null) {
               createAssignment(taskId, designatedWorker, callback);
            }
            else {
               logger.error("No worker to assign task: {}", taskId);
            }
            break;
         default:
            logger.error("assignTask failed.", KeeperException.create(KeeperException.Code.get(rc), path));
            break;
      }
   };
   private AsyncCallback.ChildrenCallback getAbsentWorkerTasksCallback = (rc, path, context, children) -> {
      String workerId = (String) context;

      switch (KeeperException.Code.get(rc)) {
         case CONNECTIONLOSS:
            getAbsentWorkerTasks(workerId);
            break;
         case OK:
            if (children != null) {
               //reassignTasks(children);
               unregisterAbsentWorkerInAssignment(workerId);
            }
            break;
         default:
            logger.error("getAbsentWorkerTasks failed.", KeeperException.create(KeeperException.Code.get(rc), path));
      }
   };

   public TaskAssignmentServiceImpl(ZooKeeper zk, ZkConfig zkConfig){
      this.zk = zk;
      this.zkTaskPath = zkConfig.getTaskPath();
      this.zkTaskAssignmentPath = zkConfig.getTaskAssignmentPath();
      this.workerSystemName = zkConfig.getWorkerSystemName();
   }


   public void reassignAndSet(List<String> children) {
      List<String> removedWorkers;

      readWriteLock.writeLock().lock();
      try {
         workerList.clear();
         workerList.addAll(children);
      } finally {
         readWriteLock.writeLock().unlock();
      }

      readWriteLock.readLock().lock();
      try {
         logger.info("Task assignment service gets a new set of workers: {}", children.size());
         int count = Math.min(20, workerList.size());
         for (int i = 0; i < count; ++i) {
            logger.info("worker: {}", workerList.get(i));
         }
      } finally {
         readWriteLock.readLock().unlock();
      }

      if (workersCache == null) {
         workersCache = new ChildrenCache(children);
         removedWorkers = null;
      }
      else {
         logger.info("Removing and setting workers");
         removedWorkers = workersCache.removeAndSet(children);
      }
      if (removedWorkers != null && !removedWorkers.isEmpty()) {
         removedWorkers.forEach(this::getAbsentWorkerTasks);
      }
   }



   private void unregisterAbsentWorkerInAssignment(String workerId) {
      try {
         ZKUtil.deleteRecursive(zk, zkTaskAssignmentPath + "/" + workerId, unregisterAbsentWorkerInAssignmentCallback, workerId);
      }
      catch (InterruptedException | KeeperException e) {
         logger.error("unregisterAbsentWorkerInAssignment failed.", e);
      }
   }


   private void getAbsentWorkerTasks(String workerId) {
      zk.getChildren(zkTaskAssignmentPath + "/" + workerId, false, getAbsentWorkerTasksCallback, workerId);
   }


   private void reassignTasks4LostWorker(List<String> tasks) {
      for (String task : tasks) {
         assignTask(task, (taskId, workerInfo) -> logger.info("Task reassigned: {}", taskId));
      }
   }


   private void createAssignment(String taskId, String designatedWorker, BiConsumer<String, NodeUri> callback) {
      final String path1 = zkTaskAssignmentPath + "/" + designatedWorker;

      createAssignment(path1, taskId, designatedWorker, (ww, ww1) -> {
         final String path2 = path1 + "/" + getPartition(taskId, 0, zkDepth4Tasks);
         createAssignment(path2, taskId, designatedWorker, (ss, ss2) -> {
            final String path3 = path2 + "/" + getPartition(taskId, 1, zkDepth4Tasks);
            createAssignment(path3, taskId, designatedWorker, (ss3, ss4) -> {
               final String path4 = path3 + "/" + getPartition(taskId, 2, zkDepth4Tasks);
               createAssignment(path4, taskId, designatedWorker, (ss5, ss6) -> {
                  final String path5 = path4 + "/" + getPartition(taskId, 3, zkDepth4Tasks);
                  createAssignment(path5, taskId, designatedWorker, (ss7, ss8) -> {
                     final String path6 = path5 + "/" + taskId;
                     createAssignment(path6, taskId, designatedWorker, (zz, zz2) -> updateTaskHandler(getPath4TaskId(taskId), designatedWorker, callback));
                  });

               });
            });
         });
      });
   }


   public void assignTask(String task, BiConsumer<String, NodeUri> callback) {
      zk.getData(getPath4TaskId(task), false, assignTaskCallback, callback);
   }


   private void isWorkerRegisteredInAssignment(String workerId, BiConsumer<String, Boolean> callback) {
      zk.getData(zkTaskAssignmentPath + "/" + workerId, false, isWorkerRegisteredInAssignmentCallback, callback);
   }


   private void registerNewWorkerInAssignment(String workerId, Consumer<String> callback) {
      byte[] data = new byte[0];
      zk.create(zkTaskAssignmentPath + "/" + workerId, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
              registerNewWorkerInAssignmentCallback, callback);
   }


   private void createAssignment(String assignmentPath, String taskId, String workerId, BiConsumer<String, NodeUri> callback) {
      byte[] data = new byte[0];
      zk.create(assignmentPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createAssignmentCallback,
              new TupleThree<>(taskId, workerId, callback));
   }


   private String getPath4TaskId(String taskId) {
      StringBuilder sb = new StringBuilder();
      sb.append(zkTaskPath);
      for (int i = 0; i < zkDepth4Tasks; ++i) {
         sb.append("/" + getPartition(taskId, i, zkDepth4Tasks));
      }

      sb.append("/" + taskId);

      return sb.toString();
   }


   private int getPartition(String taskId, int partIndex, int partCount) {
      int length = taskId.length();
      int partLength = length / partCount;

      int beginIndex = partIndex * partLength;
      int endIndex = Math.min(beginIndex + partLength, length);
      return hash(taskId.substring(beginIndex, endIndex));
   }


   private int hash(String text) {
      return Math.abs(text.hashCode() % 100);
   }


   private void deleteTask(String taskId) {
      zk.delete(getPath4TaskId(taskId), -1, deleteTaskCallback, taskId);
   }


   private void updateTaskHandler(String path, String workerId, BiConsumer<String, NodeUri> callback) {
      byte[] data = UTF8.getBytes(workerId);

      TupleTwo<String, BiConsumer<String, NodeUri>> tuple2 = new TupleTwo<>(workerId, callback);

      zk.setData(path, data, -1, updateTaskHandlerCallback, tuple2);
   }


   private void createTask(String path, String taskId, Consumer<String> callback) {
      zk.create(path, UTF8.getBytes(TaskStates.Idle), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createTaskCallback,
              new TupleTwo<>(taskId, callback));
   }


   public void isTaskAssigned(String taskId, BiConsumer<String, Boolean> callback) {
      zk.getData(getPath4TaskId(taskId), false, isTaskAssignedCallback, callback);
   }


   public void taskExists(String taskId, BiConsumer<String, Boolean> callback) {
      zk.getData(getPath4TaskId(taskId), false, taskExistsCallback, callback);
   }


   private NodeUri getUri(String workerId) {
      NodeUri worker = new NodeUri();
      String[] parts = workerId.split("_");
      worker.setHost(parts[0]);
      worker.setPort(Integer.parseInt(parts[1]));
      worker.setSystem(workerSystemName);
      return worker;
   }


   @Override public void getWorkerAssigned2Task(String taskId, Consumer<NodeUri> callback) {
      zk.getData(getPath4TaskId(taskId), false, getWorkerAssigned2TaskCallback, callback);
   }


   @Override public void createTask(String taskId, Consumer<String> callback) {
      final String path1 = zkTaskPath + "/" + getPartition(taskId, 0, zkDepth4Tasks);
      createTask(path1, taskId, (ss1) -> {
         final String path2 = path1 + "/" + getPartition(taskId, 1, zkDepth4Tasks);
         createTask(path2, taskId, (ss2) -> {
            final String path3 = path2 + "/" + getPartition(taskId, 2, zkDepth4Tasks);
            createTask(path3, taskId, (ss3) -> {
               final String path4 = path3 + "/" + getPartition(taskId, 3, zkDepth4Tasks);
               createTask(path4, taskId, ss4 -> {
                  final String path5 = path4 + "/" + taskId;
                  createTask(path5, taskId, callback);
               });
            });
         });
      });

   }

}

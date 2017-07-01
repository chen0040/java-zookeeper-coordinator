package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.NodeUri;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public interface TaskAssignmentService {
   void reassignAndSet(List<String> workers);

   void isTaskAssigned(String taskId, BiConsumer<String, Boolean> callback);

   void getWorkerAssigned2Task(String taskId, Consumer<NodeUri> callback);

   void createTask(String taskId, Consumer<String> callback);

   void taskExists(String taskId, BiConsumer<String, Boolean> callback);

   void assignTask(String taskId, BiConsumer<String, NodeUri> callback);
}

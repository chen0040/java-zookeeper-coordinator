package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.models.NodeUri;

import java.util.List;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public interface WorkerClusterService {
   void watchWorkers();

   boolean workerExists(String workerId);

   void addWorkerChangeListener(Consumer<List<String>> listener);

   List<NodeUri> workers();
}

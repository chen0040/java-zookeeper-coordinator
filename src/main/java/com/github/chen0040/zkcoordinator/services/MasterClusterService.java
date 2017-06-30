package com.github.chen0040.zkcoordinator.services;


import com.github.chen0040.zkcoordinator.model.AkkaNodeUri;

import java.util.List;
import java.util.function.Consumer;


/**
 * Created by xschen on 5/10/16.
 */
public interface MasterClusterService {
   void watchMasters();

   boolean masterExists(String masterId);

   void addMasterChangeListener(Consumer<List<AkkaNodeUri>> listener);

   List<AkkaNodeUri> masters();

   void addMasterAddedListener(Consumer<List<AkkaNodeUri>> listener);
}

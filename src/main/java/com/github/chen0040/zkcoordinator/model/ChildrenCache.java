package com.github.chen0040.zkcoordinator.model;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by xschen on 5/5/17.
 */
public class ChildrenCache {
   private Set<String> childrenSet = null;


   public ChildrenCache(List<String> children) {
      this.childrenSet = new HashSet<>(children);
   }


   public List<String> removeAndSet(List<String> children) {

      Set<String> newChildren = new HashSet<>(children);
      List<String> removed = new ArrayList<>();
      childrenSet.stream().forEach(child -> {
         if (!newChildren.contains(child)) {
            removed.add(child);
         }
      });

      this.childrenSet = newChildren;
      return removed;
   }
}

package com.github.chen0040.zkcoordinator.model;


import java.io.Serializable;


/**
 * Created by xschen on 8/26/17.
 */
public class RegistrationCompleted implements Serializable {
   private static final long serialVersionUID = -8603336312267035822L;

   private String serverId;
   private int port;

   public RegistrationCompleted(String serverId, int port) {

      this.serverId = serverId;
      this.port = port;
   }

   public RegistrationCompleted(){

   }


   public String getServerId() {
      return serverId;
   }


   public void setServerId(String serverId) {
      this.serverId = serverId;
   }


   public int getPort() {
      return port;
   }


   public void setPort(int port) {
      this.port = port;
   }
}

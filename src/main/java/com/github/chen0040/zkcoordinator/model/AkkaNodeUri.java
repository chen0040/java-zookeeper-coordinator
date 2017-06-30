package com.github.chen0040.zkcoordinator.model;


import java.io.Serializable;


/**
 * Created by xschen on 30/6/2017.
 */
public class AkkaNodeUri implements Serializable {

   private static final long serialVersionUID = -6536075494230297013L;
   private String host;
   private String protocol;
   private String system;
   private int port;
   private String message;


   public AkkaNodeUri() {
      host = "localhost";
      protocol = "akka.tcp";
      system = "";
      message = "";
   }




   public String getHost() {
      return host;
   }


   public void setHost(String host) {
      this.host = host;
   }


   public String getProtocol() {
      return protocol;
   }


   public void setProtocol(String protocol) {
      this.protocol = protocol;
   }


   public String getSystem() {
      return system;
   }


   public void setSystem(String system) {
      this.system = system;
   }


   public int getPort() {
      return port;
   }


   public void setPort(int port) {
      this.port = port;
   }




   public String getMessage() {
      return message;
   }


   public void setMessage(String message) {
      this.message = message;
   }


   @Override public boolean equals(Object obj) {
      if (obj instanceof AkkaNodeUri) {
         AkkaNodeUri rhs = (AkkaNodeUri) obj;
         if (!rhs.host.equals(host)) {
            return false;
         }
         if (!rhs.system.equals(system)) {
            return false;
         }
         if (!rhs.protocol.equals(protocol)) {
            return false;
         }

         return rhs.port == port;
      }
      return false;
   }


   @Override public int hashCode() {
      int hashCode = 0;
      hashCode = hashCode * 31 + host.hashCode();
      hashCode = hashCode * 31 + protocol.hashCode();
      hashCode = hashCode * 31 + system.hashCode();
      hashCode = hashCode * 31 + port;
      return hashCode;
   }


   @Override public String toString() {
      return protocol + "://" + system + "@" + host + ":" + port;
   }


   public String toAkkaPath() {
      return protocol + "://" + system + "@" + host + ":" + port;
   }
}

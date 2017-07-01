package com.github.chen0040.zkcoordinator.samples;


import com.github.chen0040.zkcoordinator.models.NodeUri;
import com.github.chen0040.zkcoordinator.models.ZkConfig;
import com.github.chen0040.zkcoordinator.nodes.ClientNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


/**
 * Created by xschen on 1/7/2017.
 */
public class ClientApplication{

   public static void main(String[] args) throws IOException, InterruptedException {
      ZkConfig config = new ZkConfig();
      config.setZkConnect("192.168.10.12:2181,192.168.10.13:2181,192.168.10.14:2181");

      final ClientNode application = new ClientNode(config);

      application.connect();

      List<NodeUri> masters = application.getMasters();
      List<NodeUri> producers = application.getProducers();

      application.disconnect();
   }

}

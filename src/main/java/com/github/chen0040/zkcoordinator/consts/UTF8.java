package com.github.chen0040.zkcoordinator.consts;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;


/**
 * Created by xschen on 10/26/16.
 */
public class UTF8 {
   private static final Logger logger = LoggerFactory.getLogger(UTF8.class);

   public static final String NAME = "UTF-8";
   public static byte[] getBytes(String text){
      byte[] result;
      try {
         result = text.getBytes(NAME);
      }
      catch (UnsupportedEncodingException e) {
         result = null;
         logger.info("Running into encoding not found for getBytes() on string " + text, e);
      }
      return result;
   }


   public static String getString(byte[] data) {
      try {
         return new String(data, NAME);
      }
      catch (UnsupportedEncodingException e) {
         logger.error("getString reports " + NAME + " encoding not supported", e);
      }
      return null;
   }
}

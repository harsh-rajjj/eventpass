package com.eventpass.common;

public class SoldOutException extends RuntimeException{

     public SoldOutException(String message) {
          super(message);
     }

}

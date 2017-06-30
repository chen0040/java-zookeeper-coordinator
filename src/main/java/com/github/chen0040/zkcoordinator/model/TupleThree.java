package com.github.chen0040.zkcoordinator.model;


import java.io.Serializable;


/**
 * Created by xschen on 30/6/2017.
 */
public class TupleThree<T1, T2, T3> implements Serializable {
   private T1 _1;
   private T2 _2;
   private T3 _3;

   public T1 _1(){
      return _1;
   }

   public T2 _2(){
      return _2;
   }

   public T3 _3(){
      return _3;
   }

   public TupleThree(T1 t1, T2 t2, T3 t3){
      _1 = t1;
      _2 = t2;
      _3 = t3;
   }


   @Override public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      TupleThree<?, ?, ?> that = (TupleThree<?, ?, ?>) o;

      if (_1 != null ? !_1.equals(that._1) : that._1 != null)
         return false;
      if (_2 != null ? !_2.equals(that._2) : that._2 != null)
         return false;
      return _3 != null ? _3.equals(that._3) : that._3 == null;
   }


   @Override public int hashCode() {
      int result = _1 != null ? _1.hashCode() : 0;
      result = 31 * result + (_2 != null ? _2.hashCode() : 0);
      result = 31 * result + (_3 != null ? _3.hashCode() : 0);
      return result;
   }
}

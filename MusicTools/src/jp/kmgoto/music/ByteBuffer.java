package jp.kmgoto.music;

import java.util.List;
import java.util.ArrayList;

public class ByteBuffer {
   private int capacity;
   private int head, tail, length;
//   private byte[] buffer = null;
   private List<Byte> buffer;

   public ByteBuffer(int capacity){
      buffer = new ArrayList<Byte>();
      this.capacity = capacity; 
      head = tail = 0;
      length = 0;
   }

   public void put(byte[] input, int size){
     synchronized(this){
       for (int i=0; i < size; i++) buffer.add(input[i]);
     }
   }

   public byte[] get(int len){
     if (buffer.size() < len) return null;
 
     byte[] retval = null;
     synchronized(this){
       retval = new byte[len]; 
       Byte[] tmp = buffer.subList(buffer.size()-len,len).toArray(new Byte[0]);
       for (int i=0; i < len; i++) retval[i] = tmp[i].byteValue(); 
       buffer.clear();
     }

     return retval;
   }

   public synchronized void dump(){
      System.out.println("dump");
   }

  public static void main(String[] args){
    ByteBuffer buffer = new ByteBuffer(16);
  }

}

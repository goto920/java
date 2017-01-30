package jp.kmgoto.music;

public class ByteRingBuffer {
   private int capacity;
   private int head, tail, length;
   private byte[] buffer = null;

   public ByteRingBuffer(int capacity){
      buffer = new byte[capacity];
      this.capacity = capacity; 
      head = tail = 0;
      length = 0;
   }

   public void put(byte[] input, int size){
     if (size > capacity) return;

     synchronized(this){
      for(int i = 0; i < size; i++)
        buffer[(tail + i) % capacity] = input[i];

      boolean full = false;
      if (length + size >= capacity) full = true;

      tail = (tail + size) % capacity;
      if (full) {
         head = tail; 
         length = capacity;
      } else 
        length = (capacity + tail - head) % capacity;
     }
     // System.out.println("length: " + length);
   }

   public byte[] get(int len){

     synchronized(this){
      // System.out.println("get length: " + length);
       if (len > length) return null;
       byte[] retval = new byte[len];
       for(int i = 0; i < len; i++)
          retval[i] = buffer[(head+i) % capacity];

       head = (head + len) % capacity;
       length -= len;
       return retval;
     }
   }

   public synchronized void dump(){
      System.out.println(
       "head/tail/length = " + head + "/" + tail + "/" + length);

      for(int i = 0; i < capacity; i++)
        System.out.print(buffer[i] + ",");
      System.out.println();
   }

  public static void main(String[] args){
    ByteRingBuffer buffer = new ByteRingBuffer(16);
    byte j = 0;
    for (int i=0; i < 16; i++){
     byte[] sample = new byte[4];
     sample[0] = j++; sample[1]=j++; sample[2]=j++; sample[3] = j++;
     buffer.put(sample, 4);
     buffer.dump();
    }

    for (int i=0; i < 16; i++){
     byte[] data = buffer.get(1);
     System.out.println(data[0]);
     buffer.dump();
    }


  }

}

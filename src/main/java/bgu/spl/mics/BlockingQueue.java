package bgu.spl.mics;


import java.util.LinkedList;

public class BlockingQueue<E>{

    private LinkedList<E> queue;

    public BlockingQueue(){
        queue = new LinkedList<>();
    }

    public int size(){
        return queue.size();
    }

    public synchronized void put(E element){
        queue.add(element);
        this.notifyAll();
    }

    public synchronized void putWithPriority(E element){
        queue.addFirst(element);
    }

    public synchronized E take() throws InterruptedException {
        while(size() == 0){
            try{
                this.wait();
            }catch (InterruptedException ignored){}
        }
        return queue.poll();
    }

    public synchronized E get(){
        return queue.pollFirst(); //returns null if the list is empty
    }
}

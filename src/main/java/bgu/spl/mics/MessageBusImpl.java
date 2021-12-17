package bgu.spl.mics;

import bgu.spl.mics.application.messages.TestModelEvent;
import bgu.spl.mics.application.messages.TickBroadcast;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

	private HashMap<MicroService, LinkedBlockingDeque<Message>> msQueues; //BlockingQueue is synchronized
	private ConcurrentHashMap<Class<? extends Message>, LinkedBlockingDeque<MicroService>> msgAndSubscribers; //the Lists are synchronized
	private ConcurrentHashMap<Event<?>,Future<?>> eventAndFuture; //synchronized
	private AtomicInteger numOfBroadcastsSent = new AtomicInteger(0);
	private AtomicInteger numOfEventsSent = new AtomicInteger(0);

	//private static MessageBusImpl single_instance=null;

	private MessageBusImpl(){
		msQueues = new HashMap<>();
		msgAndSubscribers = new ConcurrentHashMap<>();
		eventAndFuture = new ConcurrentHashMap<>();
	}

	private static class SingeltonHolder{
		private static MessageBusImpl instance = new MessageBusImpl();
	}

	public static MessageBusImpl getInstance(){
		return SingeltonHolder.instance;
	}


	@Override
	 synchronized public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		if(isMicroServiceRegistered(m)) {
			if (!msgAndSubscribers.containsKey(type)) {
				LinkedBlockingDeque<MicroService> list = new LinkedBlockingDeque<>();
				list.add(m);
				msgAndSubscribers.put(type, list);
			} else {
				msgAndSubscribers.get(type).add(m);
			}
		}
	}

	@Override
	 synchronized public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		if(!msgAndSubscribers.containsKey(type)){
			LinkedBlockingDeque<MicroService> list = new LinkedBlockingDeque<>();
			list.add(m);
			msgAndSubscribers.put(type,list);
		}else {
			msgAndSubscribers.get(type).add(m);
		}
	}

	@Override
	synchronized public <T> void complete(Event<T> e, T result) {
		Future<T> f = (Future<T>)eventAndFuture.get(e);
		f.resolve(result);
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		if(msgAndSubscribers.get(b.getClass()) != null) {
			synchronized (msgAndSubscribers.get(b.getClass())) {
				// Must be in synchronized block
				for (MicroService microService : msgAndSubscribers.get(b.getClass())) {
					msQueues.get(microService).add(b);
				}

			}

			//updates the numOfBroadcastsSent value
			int oldVal;
			do {
				oldVal = numOfBroadcastsSent.get();
			} while (!numOfBroadcastsSent.compareAndSet(oldVal, oldVal + 1));
		}
	}

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		Future<T> future = new Future<>();
		if (msgAndSubscribers.get(e.getClass()) != null) {
			synchronized (msgAndSubscribers) {
				MicroService ms = msgAndSubscribers.get(e.getClass()).poll();

				if (ms == null) {
					return null;
				} //returns null is there's no subscribers

				msQueues.get(ms).add(e);

				msgAndSubscribers.get(e.getClass()).add(ms); //round robbin
			}
			eventAndFuture.put(e, future); //synchronised map

			//updates the numOfEventsSent value
			int oldVal;
			do {
				oldVal = numOfEventsSent.get();
			} while (!numOfEventsSent.compareAndSet(oldVal, oldVal + 1));
		}
		return future;
	}

	@Override
	public void register(MicroService m) { //TODO synchronize?
		msQueues.put(m, new LinkedBlockingDeque<>());
	}

	@Override
	public void unregister(MicroService m) {
		msQueues.remove(m); //does not change if it doesn't contain m
		synchronized (msgAndSubscribers){
			for (LinkedBlockingDeque<MicroService> l : msgAndSubscribers.values()) { //TODO!!
				l.remove(m);
			}
		}
	}

	@Override
	 public Message awaitMessage(MicroService m) throws InterruptedException {
		if (!isMicroServiceRegistered(m)){
			throw new IllegalStateException();
		}
		return msQueues.get(m).take();
	}

	@Override
	synchronized public <T> boolean isMicroServiceSubscribedEvent(MicroService m, Class<? extends Event<T>> type) {
		if(msgAndSubscribers.get(type) != null){
			return msgAndSubscribers.get(type).contains(m);
		}
		return false;
	}

	@Override
	synchronized public boolean isMicroServiceSubscribedBroadcast(MicroService m, Class<? extends Broadcast> type) {
		if(msgAndSubscribers.get(type) != null){
			return msgAndSubscribers.get(type).contains(m);
		}
		return false;
	}

	@Override
	 public boolean isMicroServiceRegistered(MicroService m) {
		return msQueues.containsKey(m);
	}

	@Override
	public int numOfBroadcastsSent() {
		return numOfBroadcastsSent.get();
	}

	@Override
	public int numOfEventsSent() {
		return numOfEventsSent.get();
	}


}

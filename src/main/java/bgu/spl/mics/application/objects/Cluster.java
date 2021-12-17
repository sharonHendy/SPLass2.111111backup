package bgu.spl.mics.application.objects;


import bgu.spl.mics.BlockingQueue;
import bgu.spl.mics.MessageBusImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Passive object representing the cluster.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class Cluster {

	private Collection<CPU> CPUS;
	private Collection<GPU> GPUS;

	private LinkedBlockingDeque<DataBatch> unProcessed;
	private Map<DataBatch, GPU> dataBatchesAndGPUs;
	private Map<GPU, BlockingQueue<DataBatch>> GPUqueues;

	//statistics
	private ArrayList<String> modelsTrained;
	private int numOfDBProcessed;

	private int numOfTimeUnitsUsedCPU;
	private int numOfTimeUnitsUsedGPU;
	private Object lock_numOfTimeUnitsUsedCPU = new Object();
	private Object lock_numOfTimeUnitsUsedGPU = new Object();
	private Object lock_numOfDBProcessed = new Object();

	private Cluster(){
		unProcessed = new LinkedBlockingDeque<DataBatch>();
		dataBatchesAndGPUs = new HashMap<>(); //TODO ok?
		GPUqueues = new HashMap<>();
		modelsTrained = new ArrayList<>();
		numOfDBProcessed = 0;
		numOfTimeUnitsUsedCPU = 0;
		numOfTimeUnitsUsedGPU = 0;
	}

	/**
     * Retrieves the single instance of this class.
     */
	public static Cluster getInstance() {
		return Cluster.SingletonHolder.instance;
	}

	private static class SingletonHolder{
		private static Cluster instance = new Cluster();
	}

	/**
	 * sets CPUs and GPUs when parsing the input file.
	 * @param CPUS
	 */
	public void setCPUS(Collection<CPU> CPUS) {
		this.CPUS = CPUS;
	}

	public void setGPUS(Collection<GPU> GPUS) {
		this.GPUS = GPUS;
		for (GPU gpu : GPUS) {
			GPUqueues.put(gpu, new BlockingQueue<>());
		}
	}
	/**
	 * receives a processed data batch from the CPU and navigates it to the appropriate GPU queue.
	 * called from CPU.
	 * @param dataBatch the processed data batch
	 */
	void receiveDataBatchFromCPU(DataBatch dataBatch){
		GPU GPU = dataBatchesAndGPUs.get(dataBatch); //finds the GPU that sent the DB
		GPUqueues.get(GPU).put(dataBatch);
	}

	/**
	 * receives a data batch from the GPU.
	 * called from GPU.
	 * @param dataBatch the data batch for processing
	 */
	void receiveDataBatchFromGPU(DataBatch dataBatch , GPU GPU){
		unProcessed.add(dataBatch);
		dataBatchesAndGPUs.put(dataBatch,GPU);
//
//		boolean delivered = false;
//		if(!CPUStatus[indexOfCurrCPU]){ //if CPU is not available tries to find one that is
//			for(int i = 0; i < CPUqueues.size(); i++){
//				if(CPUStatus[i]){
//					CPU CPU = ((ArrayList<CPU>)CPUS).get(i);
//					CPUqueues.get(CPU).put(dataBatch);
//					CPUStatus[i] = false;
//					delivered = true;
//				}
//			}
//		}
//		if (!delivered){ //delivers the DB to the currCPU if it hasn't yet been delivered
//			CPU CPU = ((ArrayList<CPU>)CPUS).get(indexOfCurrCPU);
//			CPUqueues.get(CPU).put(dataBatch);
//			indexOfCurrCPU = indexOfCurrCPU + 1;
//		}
	}

	DataBatch getDBFromQueueCPU(){
//		DataBatch db = null;
//		try {
//			db = unProcessed.take(); //blocking
//		}catch (InterruptedException ignored){}
		return unProcessed.poll(); //returns null if queue is empty
	}

	DataBatch getDBFromQueueGPU(GPU GPU){
//		try{
//			db = GPUqueues.get(GPU).take(); //blocking
//		}catch (InterruptedException ignored){}
		return GPUqueues.get(GPU).get(); //returns null if queue is empty
	}

	public synchronized void setModelsTrained(String name) {
		this.modelsTrained.add(name);
	}

	public void addToNumOfDBProcessed() {
		synchronized (lock_numOfDBProcessed) {
			this.numOfDBProcessed = numOfDBProcessed + 1;
		}
	}

	public void addToNumOfTimeUnitsUsedCPU() {
		synchronized (lock_numOfTimeUnitsUsedCPU) {
			this.numOfTimeUnitsUsedCPU = numOfTimeUnitsUsedCPU + 1;
		}
	}

	public synchronized void addToNumOfTimeUnitsUsedGPU() {
		synchronized (lock_numOfTimeUnitsUsedGPU) {
			this.numOfTimeUnitsUsedGPU = numOfTimeUnitsUsedGPU + 1;
		}
	}

	public int getNumOfTimeUnitsUsedCPU() {
		return numOfTimeUnitsUsedCPU;
	}

	public int getNumOfTimeUnitsUsedGPU() {
		return numOfTimeUnitsUsedGPU;
	}

	public int getNumOfDBProcessed(){
		return numOfDBProcessed;
	}
}

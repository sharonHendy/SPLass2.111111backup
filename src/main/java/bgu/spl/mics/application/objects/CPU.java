package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.services.CPUService;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Passive object representing a single CPU.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class CPU {

    enum CPUStatus {Created, Started, Processing, Done, Waiting, NotStartedProcessing}

    /**
     * Processing - currently processing a batch
     * Waiting - waiting for a batch from the cluster
     */

    private int cores;
    private Collection<DataBatch> data;
    private Cluster cluster;

    private int currTick;
    private DataBatch currDataBatch;
    private int ticksUntilDone;
    private int timeUnitsUsed;
    private CPUStatus status;


    public CPU(int cores, Cluster cluster){
        this.cores = cores;
        data = new ArrayList<>();
        this.cluster = cluster;
        currTick = 0;
       // isProcessing = false;
        status = CPUStatus.Waiting;
    }

//    public void startCPU(){ //called from CPU service
//        status = CPUStatus.Waiting;
//        getDataBatches();
//    }
    /**
     *the CPUService will call this method when it receives a tickBroadcast from the messageBus.
     *@post: @currTick - @pre:currTick == 1
     */
    public void updateTick(){
        currTick = currTick + 1;

        if(status == CPUStatus.Processing){
            cluster.addToNumOfTimeUnitsUsedCPU();
            if(currTick >= ticksUntilDone){
                doneProcessing();
            }
        }else{
            getDataBatches();
        }
    }

    /**
     * gets dataBatches from the cluster. called when CPU is created and every time it finishes processing a batch.
     * @post: data.size() > @pre:data.size()
     */
    public void getDataBatches(){
        DataBatch db = cluster.getDBFromQueueCPU();
        if(db != null){
            data.add(db);
            if(status == CPUStatus.Waiting){
                startProcessing();
            }
        }
    }

    /**
     * sets the current dataBatch the CPU is working on, and the number of ticks it takes to process it.
     * @pre: data.size() != 0
     * @post: currDataBatch != null
     */
    void startProcessing(){
        currDataBatch = ((ArrayList<DataBatch>)data).get(0);

        Data.Type type = currDataBatch.getDataType();

        if(type == Data.Type.Images){
            ticksUntilDone = (32/cores) * 4;
        }else if(type == Data.Type.Text){
            ticksUntilDone = (32/cores)*2;
        }else if(type == Data.Type.Tabular){
            ticksUntilDone = (32/cores);
        }

        currTick = 0;
        status = CPUStatus.Processing;
    }

    /**
     * send the processed data batch to the cluster.
     * @pre: data.size > 0
     * @post: data.size == @pre: data.size - 1
     */
    void sendDataBatch(){
        cluster.receiveDataBatchFromCPU(currDataBatch);
        data.remove(currDataBatch);
    }

    /**
     * checks if the number of ticks it takes to process the data batch have passed,
     * if so starts the processing of another batch.
     * @post: if(ticksUntilDone == currTick) {data.size() == @pre:data.size() -1;}
     *
     */
    void doneProcessing(){
        cluster.addToNumOfDBProcessed();
        sendDataBatch();

        if(data.size() != 0){
            startProcessing();
        }else{
            status = CPUStatus.Waiting;
            getDataBatches();
        }
    }

    public int getCurrTick() {
        return currTick;
    }

    public int getTicksUntilDone() {
        return ticksUntilDone;
    }

    public int getCores() {
        return cores;
    }

    public Collection<DataBatch> getData() {
        return data;
    }

    public DataBatch getCurrDataBatch() {
        return currDataBatch;
    }

    public void setCurrTick(int currTick) {
        this.currTick = currTick;
    }
}

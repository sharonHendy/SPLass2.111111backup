package bgu.spl.mics.application.objects;

import com.sun.org.apache.xpath.internal.operations.Mod;
import javafx.print.Collation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static bgu.spl.mics.application.objects.GPU.Type.*;

/**
 * Passive object representing a single GPU.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class GPU {


    /**
     * Enum representing the type of the GPU.
     */
    public  enum Type {RTX3090, RTX2080, GTX1080}

    public enum GPUStatus {Started, Training, Waiting, Completed}
    /**
     * Created = just created, Started = started handling the data, Training= training a batch,
     *  Done= done training a batch, Waiting = waiting for a processed batch to train,
     *  NoteStartedTraining = started but didn't start to train yet.
     */


    private Type type;
    private Model model;
    private  Cluster cluster;
    private int MaxNumOfProcessedBatches; //max number of batches that can be stored

    //private boolean isTraining;
    private int ticksUntilDone; //number of ticks it takes to train the current batch
    private int currTick;
    private Collection<DataBatch> unprocessedDBs; //batches before processing
    private Collection<DataBatch> processedDBs; //batches after processing
    int numOfTrainedDBs; //number of data batches trained so far
    int numOfTotalDBs; //total number of data batches
    int totalTimeUnitsUsed; //for statistics /TODO!!
    private DataBatch currDBInTraining; //current DB the GPU trains
    //private boolean hasStarted; //true if the GPU started working
    private int numOfReceived; //number of processed DBs received

    private GPUStatus status;

    public GPU(Type type, Cluster cluster){
        this.type = type;
        this.cluster = cluster;
        unprocessedDBs = new ArrayList<>();
        processedDBs = new ArrayList<>();

        if (this.type == RTX3090){
            MaxNumOfProcessedBatches = 32;
            ticksUntilDone = 1;
        }else if(this.type == RTX2080){
            MaxNumOfProcessedBatches = 16;
            ticksUntilDone = 2;
        }else if(this.type == GTX1080){
            MaxNumOfProcessedBatches = 8;
            ticksUntilDone = 4;
        }

        currTick = 0;
        currDBInTraining = null;
        numOfTrainedDBs = 0;
        status = GPUStatus.Started;
    }

    /**
     * tests the model.
     * @pre: model.getResult() == None
     * @post: model.getResult() == Good | Bad
     */
    public void testModel(Model model){
        System.out.println("testinggggg");
        this.model = model;
        Random random = new Random();
        int r = random.nextInt(10);
        //sets the test result in the model
        if(model.getStudent().getStatus() == Student.Degree.MSc){
            if (r < 6){
                model.setResult(Model.Result.Good);
            }else{
                model.setResult(Model.Result.Bad);
            }
        }else {
            if (r < 8){
                model.setResult(Model.Result.Good);
            }else{
                model.setResult(Model.Result.Bad);
            }
        }
        model.setStatus(Model.Status.Tested);
    }

    /**
     * starts training a new model.
     * @param model
     */
    public void trainModel(Model model){
        System.out.println("trainingggggggggggggggggggggggggggg");
        this.model = model;
        reset();
        prepareDataBatches(); //prepares and sends
    }

    /**
     * resets the GPU fields, prepares for training a new model.
     */
    public void reset(){
        unprocessedDBs.clear();
        processedDBs.clear();
        currTick = 0;
        currDBInTraining = null;
        numOfTrainedDBs = 0;
        totalTimeUnitsUsed = 0;
        //numOfReceived = 0;
        status = GPUStatus.Started;
    }

    /**
     *the GPUService will call this method when it receives a tickBroadcast from the messageBus.
     * checks if the training is done, and trys to receive a batch from the cluster.
     *@post: @currTick - @pre:currTick == 1
     */
    public void updateTick(){
        currTick = currTick + 1;
        if(status == GPUStatus.Training){
            cluster.addToNumOfTimeUnitsUsedGPU();
            if(ticksUntilDone <= currTick) {
                doneTraining();
            }
        }

        if(status == GPUStatus.Started) { //means it started working
            receiveDataBatchFromCluster();
        }
    }

    /**
     * splits the data to dataBatches.
     * @post: numOfTotalDBs == model.getData().getSize()/1000
     */
    void prepareDataBatches(){
        numOfTotalDBs = model.getData().getSize()/1000;
        for (int i =0; i< numOfTotalDBs; i++){ //creates the DBs
            unprocessedDBs.add(new DataBatch(model.getData(),i));
        }
        for(int i = 0; i< MaxNumOfProcessedBatches; i++){ //sends all the DBs to the cluster
            sendDataBatchesToCluster();
        }
    }

    /**
     * sends unprocessed data batch to the cluster. will only send if it has room to store them.
     * @pre: GPU.getUnprocessedDBs().size() > 0
     * @post: GPU.getUnprocessedDBs().size() == @pre:GPU.getUnprocessedDBs().size() -1
     */
    void sendDataBatchesToCluster(){
        //sends one by one, called from doneTraining
        if(unprocessedDBs.size() != 0 && (processedDBs.size() < MaxNumOfProcessedBatches)) {
            cluster.receiveDataBatchFromGPU(((ArrayList<DataBatch>) unprocessedDBs).get(0), this);
            ((ArrayList<DataBatch>) unprocessedDBs).remove(0);
        }
    }

    /**
     * receives processed data batch from the cluster, only if there is room for it.
     *
     * ******called only if GPU has room - from done training and prepareDB************
     * @inv: GPU.getProcessedDBs().size() <= MaxNumOfProcessedBatches
     * @post: GPU.getProcessedDBs().size() == @pre:GPU.getProcessedDBs().size() + 1
     */
    void receiveDataBatchFromCluster(){

        if(processedDBs.size() < MaxNumOfProcessedBatches) {
            DataBatch dataBatch = cluster.getDBFromQueueGPU(this);
            if (dataBatch != null) {
                processedDBs.add(dataBatch);
                if (status == GPUStatus.Waiting) { //if it has been waiting for a batch, starts it again.
                    startTraining();
                } else if (status == GPUStatus.Started) { //happens once when the training starts
                    status = GPUStatus.Training;
                    model.setStatus(Model.Status.Training);
                    startTraining();
                }
            }
        }


//        Thread t = new Thread(()->{
//            while (numOfTotalDBs != numOfReceived){ //gets DBs until all have been received
//                synchronized (this){ //wait until it has room for more batches
//                    while (processedDBs.size() == MaxNumOfProcessedBatches){
//                        try{
//                            this.wait();
//                        }catch (InterruptedException ignored){}
//                    }
//                }
//                DataBatch dataBatch = cluster.getDBFromQueueGPU(this); //blocking method
//                processedDBs.add(dataBatch);
//
////                if(!model.getStatus().equals(Model.Status.Training)){ //if it hasn't started training yet (happens once)
////                    startTraining();
////                }
//                //TODO could be a problem if GPU got into Waiting just after this line :(
//                if(status == GPUStatus.Waiting){ //if it has been waiting for a batch, starts it again.
//                    startTraining();
//                }else if(status == GPUStatus.NotStartedTraining){ //happens once when the training starts
//                    status = GPUStatus.Training;
//                    model.setStatus(Model.Status.Training);
//                    startTraining();
//                }
//            }
//        });
//        t.start();
        //receives one DB from the cluster
//        DataBatch dataBatch = cluster.getDBFromQueueGPU(this); //blocking method
//        processedDBs.add(dataBatch);
//        if(!model.getStatus().equals(Model.Status.Training)){ //if it hasn't started training yet (happens once)
//            startTraining();
//        }
    }

    /**
     * starts to train a processed data batch.
     * @pre: processedDBs.size() > 0
     * @post: ticksUntilDone != 0
     * @post: currTick == 0
     * @post: currDBInTraining != null
     */
    void startTraining(){
        if(processedDBs.size() != 0){
            currDBInTraining = ((ArrayList<DataBatch>)processedDBs).get(0);
            currTick = 0;
            status = GPUStatus.Training;
           // isTraining = true; //ready for ticks
//            if(!model.getStatus().equals(Model.Status.Training)){
//                model.setStatus(Model.Status.Training);
//            }
        }
    }

    /**
     * starts to train a new batch if there is one, if not goes to Waiting mode.
     * @post: if(ticksUntilDone == currTick) {processedDBs.size() == @pre:processedDBs.size() -1;
     *                                        model.getData().getProcessed() == @pre:model.getData().getProcessed() + 1}
     */
    void doneTraining(){
            numOfTrainedDBs = numOfTrainedDBs + 1;
            model.getData().setProcessed(model.getData().getProcessed() + 1000);
            ((ArrayList<DataBatch>)processedDBs).remove(0);
            sendDataBatchesToCluster();
            if(numOfTrainedDBs == numOfTotalDBs){
                complete();
            }else{
                if(processedDBs.size() != 0){
                    startTraining();
                }else{
                    status = GPUStatus.Waiting;
                    receiveDataBatchFromCluster();
                }

//                while(processedDBs.size() == 0){
////                    try{
////                        this.wait();
////                    }catch (InterruptedException ignored){}
//                    receiveDataBatchFromCluster();
//                }

//                if(processedDBs.size() != 0){
//                    startTraining();
//                }else {
//                    status = GPUStatus.Waiting;
//                }
            }

    }

    /**
     * notifies the GPUService that it finished training the model.
     * @pre: model.status == "Training"
     * @pre: model.getData().getProcessed() == model.getData().getSize()
     * @pre: numOfTrainedDBs == numOfTotalDBs
     * @post: model.status == "Trained"
     */
    void complete(){
        model.setStatus(Model.Status.Trained);
        cluster.setModelsTrained(model.getName());
        status = GPUStatus.Completed;
    }

    public GPUStatus getStatus(){
        return status;
    }

    public Model getModel() {
        return model;
    }

    public Type getType() {
        return type;
    }

    public int getMaxNumOfProcessedBatches() {
        return MaxNumOfProcessedBatches;
    }

    public int getTicksUntilDone() {
        return ticksUntilDone;
    }

    public Collection<DataBatch> getUnprocessedDBs() {
        return unprocessedDBs;
    }

    public Collection<DataBatch> getProcessedDBs() {
        return processedDBs;
    }

    public int getNumOfTotalDBs() {
        return numOfTotalDBs;
    }

    public int getCurrTick() {
        return currTick;
    }

    public DataBatch getCurrDBInTraining() {
        return currDBInTraining;
    }

    public void setCurrTick(int currTick) {
        this.currTick = currTick;
    }

    public void setCurrDBInTraining(DataBatch currDBInTraining) {
        this.currDBInTraining = currDBInTraining;
    }

    public void setStatus(GPUStatus status) {
        this.status = status;
    }
}

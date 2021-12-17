package bgu.spl.mics.application.objects;

import org.junit.Before;
import org.junit.Test;

import static bgu.spl.mics.application.objects.GPU.Type.RTX2080;
import static bgu.spl.mics.application.objects.GPU.Type.RTX3090;
import static org.junit.Assert.*;

public class GPUTest {
    GPU GPU;
    @Before
    public void setUp(){
        GPU = new GPU(RTX2080,Cluster.getInstance());

    }

    @Test
    public void testModel(){
        GPU.testModel(new Model("",new Data(Data.Type.Text,1000),new Student("","", Student.Degree.PhD)));
        assertTrue(GPU.getModel().getResult() == Model.Result.Good ||
                GPU.getModel().getResult() == Model.Result.Bad);
        assertSame(GPU.getModel().getStatus(), Model.Status.Tested);
    }

    @Test
    public void updateTick() {
        int currTick = GPU.getCurrTick();
        GPU.updateTick();
        assertEquals(currTick, currTick + 1);
    }

    @Test
    public void prepareDataBatches() { //called from constructor
        int size = GPU.getModel().getData().getSize();
        assertEquals(GPU.getNumOfTotalDBs() ,size/1000);
        assertEquals(GPU.getUnprocessedDBs().size(),size/1000);
    }

    @Test
    public void sendDataBatchesToCluster(){
        int before = GPU.getUnprocessedDBs().size();
        GPU.sendDataBatchesToCluster();
        assertEquals(GPU.getUnprocessedDBs().size(), before - 1);
    }

    @Test
    public void receiveDataBatchFromCluster() {
        assertTrue(GPU.getProcessedDBs().size() < GPU.getMaxNumOfProcessedBatches());
        int before = GPU.getProcessedDBs().size();
        GPU.receiveDataBatchFromCluster();
        assertEquals(GPU.getProcessedDBs().size(), before + 1);
        assertTrue(GPU.getProcessedDBs().size() <= GPU.getMaxNumOfProcessedBatches());
    }

    @Test
    public void startTraining() {
        GPU.startTraining();
        assertTrue(GPU.getStatus().equals(bgu.spl.mics.application.objects.GPU.GPUStatus.Training));
        assertEquals(0, GPU.getCurrTick());
        assertEquals(2, GPU.getTicksUntilDone());
        assertNotNull(GPU.getCurrDBInTraining());
        assertEquals(GPU.getModel().getStatus(), Model.Status.Training);
    }

    @Test
    public void doneTraining() {
        DataBatch dataBatch = GPU.getUnprocessedDBs().iterator().next();
        GPU.getProcessedDBs().add(dataBatch);
        int sizeBefore = GPU.getProcessedDBs().size();
        int numOfProcessedInModelBefore = GPU.getModel().getData().getProcessed();
        GPU.setCurrDBInTraining(dataBatch);
        DataBatch curr = GPU.getCurrDBInTraining();
        GPU.startTraining();

        //checks that the GPU doesn't move to another batch until <TicksUntilDone> ticks have passed
        while ( GPU.getCurrTick() < GPU.getTicksUntilDone()){
            GPU.doneTraining();
            assertSame(curr, GPU.getCurrDBInTraining());
            GPU.setCurrTick(GPU.getCurrTick() +1);
        }
        GPU.doneTraining(); //here CurrTick == TicksUntilDone
        assertNotSame(curr, GPU.getCurrDBInTraining());
        assertEquals(GPU.getProcessedDBs().size(), sizeBefore - 1);
        assertEquals(GPU.getModel().getData().getProcessed(), numOfProcessedInModelBefore + 1);

    }

    @Test
    public void complete() {
        GPU.complete();
        assertEquals(GPU.getModel().getStatus(), Model.Status.Trained);
    }

}
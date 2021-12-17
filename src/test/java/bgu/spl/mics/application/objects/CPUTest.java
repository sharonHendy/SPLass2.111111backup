package bgu.spl.mics.application.objects;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

public class CPUTest extends TestCase {

    CPU CPU;
    @Before
    public void setUp(){
        CPU = new CPU(4,Cluster.getInstance());
        CPU.getData().add(new DataBatch(new Data(Data.Type.Images, 2000),0));
    }

    @Test
    public void updateTick() {
        int currTick = CPU.getCurrTick();
        CPU.updateTick();
        assertEquals(currTick, currTick + 1);
    }

    @Test
    public void getDataBatches() {
        int before = CPU.getData().size();
        CPU.getDataBatches();
        assertTrue(CPU.getData().size() > before);
    }

    @Test
    public void startProcessing() {
        CPU.startProcessing();

        assertNotNull(CPU.getCurrDataBatch());

        Data.Type type = CPU.getCurrDataBatch().getDataType();

        if(type == Data.Type.Images){
            assertEquals(CPU.getTicksUntilDone(),(32/CPU.getCores()) * 4);
        }else if(type == Data.Type.Text){
            assertEquals(CPU.getTicksUntilDone(),(32/CPU.getCores()) * 2);
        }else if(type == Data.Type.Tabular){
            assertEquals(CPU.getTicksUntilDone(),32/CPU.getCores());
        }

    }

    @Test
    public void sendDataBatch() {
        int size = CPU.getData().size();
        CPU.sendDataBatch();
        assertEquals(CPU.getData().size(), size - 1);
    }

    @Test
    public void doneProcessing() {
        CPU.getData().clear();
        CPU.getData().add(new DataBatch(new Data(Data.Type.Images, 2000),0));
        CPU.getData().add(new DataBatch(new Data(Data.Type.Images, 2000),0));

        //check that it did not move to a new batch for <ticksUntilDone> ticks have passed
        CPU.startProcessing();
        DataBatch curr = CPU.getCurrDataBatch();
        while(CPU.getCurrTick() < CPU.getTicksUntilDone()){
            CPU.doneProcessing();
            assertSame(CPU.getCurrDataBatch(), curr);
            CPU.setCurrTick(CPU.getCurrTick() +1);
        }
        CPU.doneProcessing();
        assertNotSame(CPU.getCurrDataBatch(), curr); //checks that it started processing a new batch
        assertEquals(1, CPU.getData().size()); //checks that it remove the already processed batch
    }
}
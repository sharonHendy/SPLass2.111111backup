package bgu.spl.mics.application.services;

import bgu.spl.mics.Callback;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminationBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.CPU;

/**
 * CPU service is responsible for handling the .
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class CPUService extends MicroService {

    private CPU CPU;

    public CPUService(String name, CPU CPU) {
        super(name);
        this.CPU = CPU;
    }

    @Override
    protected void initialize() { //called when the method run() is called
        CPU.getDataBatches();
        subscribeBroadcast(TerminationBroadcast.class, c -> terminate());
        subscribeBroadcast(TickBroadcast.class, c -> CPU.updateTick()); //subscribe for tickBroadcast-callback=updateTick()
    }
}

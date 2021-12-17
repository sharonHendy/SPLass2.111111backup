package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.PublishResultsEvent;
import bgu.spl.mics.application.messages.TerminationBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.ConfrenceInformation;
import bgu.spl.mics.application.messages.PublishConferenceBroadcast;

/**
 * Conference service is in charge of
 * aggregating good results and publishing them via the {@link PublishConferenceBroadcast},
 * after publishing results the conference will unregister from the system.
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class ConferenceService extends MicroService {

    ConfrenceInformation confrenceInformation;
    int ticks;
    int date;
    int tickTime; //duration of tick

    public ConferenceService(String name ,int tickTime, ConfrenceInformation conference) {
        super(name);
        confrenceInformation = conference;
        ticks = 0;
        this.date = conference.getDate();
        this.tickTime = tickTime;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TerminationBroadcast.class, c -> {
            terminate();
        });
        subscribeEvent(PublishResultsEvent.class, c -> {confrenceInformation.addResult(c.getModel());});
        subscribeBroadcast(TickBroadcast.class, c -> updateTick());
    }

    /**
     * called when the service receives a TickBroadcast. publishes the results and terminates if ticks==date.
     */
    void updateTick(){
        //when ticks = date : publish with PublishBroadcast
        ticks = ticks + 1;
        if (ticks * tickTime == date){
            sendBroadcast(new PublishConferenceBroadcast(confrenceInformation.getModelsWithResults()));
            terminate(); //unregisters
        }
    }
}

package bgu.spl.mics.application.services;

import bgu.spl.mics.Callback;
import bgu.spl.mics.Event;
import bgu.spl.mics.Message;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.GPU;
import bgu.spl.mics.application.objects.Model;
import com.sun.org.apache.xpath.internal.operations.Mod;
import  bgu.spl.mics.application.objects.GPU.GPUStatus;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * GPU service is responsible for handling the
 * {@link TrainModelEvent} and {@link TestModelEvent},
 *
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class GPUService extends MicroService {
    private GPU GPU;
    private Model currModel;
    private TrainModelEvent currTrainEvent;
    private LinkedList<Event<Model>> eventInLine;
    private boolean isBusy; //true if it is currently working on an event

    public GPUService(String name,GPU GPU) {
        super(name);
        this.GPU = GPU;
        eventInLine = new LinkedList<>();
        isBusy = false;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TerminationBroadcast.class, c -> {
            terminate();
        });

        subscribeBroadcast(TickBroadcast.class, c -> {
            GPU.updateTick();
            if(GPU.getStatus() == GPUStatus.Completed){
                GPU.setStatus(GPUStatus.Started);
                complete(currTrainEvent,currModel);
                sendEvent(new FinishedTrainingEvent(currModel));
                if(eventInLine.size() != 0){ //if there were event in line, starts processing one
                    startProcessingEvent();
                }else{
                    isBusy = false;
                }
            }
        });

        subscribeEvent(TrainModelEvent.class, c -> {
            if(!isBusy){
                isBusy = true;
                GPU.trainModel(c.getModel());
                currModel = c.getModel();
                currTrainEvent = c;
            }else {
                //TODO: sort by size of data
                int i = 0;
                int size = c.getModel().getData().getSize();
                for(Event<Model> m : eventInLine){
                    if(m.getClass() == TrainModelEvent.class){
                        if(((TrainModelEvent) m).getModel().getData().getSize() < size){
                            i++;
                        }
                    }
                }
                eventInLine.add(i, c); //saves the event for when it finishes training this model
            }

        });

        subscribeEvent(TestModelEvent.class, c -> {
            if(!isBusy) {
                GPU.testModel(c.getModel());
                complete(c, c.getModel()); //the model is modified with the result
                sendEvent(new FinishedTestingEvent(c.getModel()));
            }else {
                eventInLine.add(0,c); //saves the event for when it's not busy with another, gives priority to tests
            }
        });
    }

    /**
     * starts processing an event that was waiting in line.
     * @pre: eventInLine.size() != 0
     */
    private void startProcessingEvent(){
        isBusy = true;
        Event<Model> e = eventInLine.get(0);
        eventInLine.remove(0);
        if(e.getClass() == TrainModelEvent.class){
            currTrainEvent = (TrainModelEvent)e;
            currModel = currTrainEvent.getModel();
            GPU.trainModel(currModel);
        }else{
            GPU.testModel(((TestModelEvent)e).getModel());
            complete(e, ((TestModelEvent)e).getModel()); //the model is modified with the result
            sendEvent(new FinishedTestingEvent(((TestModelEvent)e).getModel()));
        }
    }


}

//            Thread t = new Thread(()-> { //waits until GPU finishes training, and places the result
//                while(c.getModel().getStatus() != Model.Status.Trained) {
//                    synchronized (GPU) {
//                        try {
//                            GPU.wait();
//                        } catch (InterruptedException ignored) {
//                        }
//                    }
//                }
//                complete(c, c.getModel());
//            });
//            t.start();
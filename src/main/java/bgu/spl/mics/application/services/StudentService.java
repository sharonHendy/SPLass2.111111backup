package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.Model;
import bgu.spl.mics.application.objects.Student;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Student is responsible for sending the {@link TrainModelEvent},
 * {@link TestModelEvent} and {@link PublishResultsEvent}.
 * In addition, it must sign up for the conference publication broadcasts.
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class StudentService extends MicroService {

    private Student student;
    private int index;

    public StudentService(String name, Student student) {
        super(name);
        this.student = student;
        index = 0;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TerminationBroadcast.class, c -> terminate());

        //subscribe to PublishConferenceBroadcast, the student handle the broadcast when received.
        subscribeBroadcast(PublishConferenceBroadcast.class, c -> {
            student.handlePublishConferenceBroadcast(c.getModelsAndResults());
        });


        //subscribes to FinishedTrainingEvent, creates TestModelEvent for the trained model from the message,
        subscribeEvent(FinishedTrainingEvent.class, c ->{
            sendEvent(new TestModelEvent(c.getModel()));
            if(index < student.getModels().size()) {
                sendEvent(new TrainModelEvent(student.getModels().get(index)));
                index++;
            }
        });

        //subscribes to FinishedTestingEvent, publishes the result
        subscribeEvent(FinishedTestingEvent.class, c -> {
            if(c.getModel().getResult()== Model.Result.Good){
                sendEvent(new PublishResultsEvent(c.getModel()));
            }

        });

        //sends the first model
        if (student.getModels().size() != 0){
            sendEvent(new TrainModelEvent(student.getModels().get(index)));
            index++;
        }

    }

}

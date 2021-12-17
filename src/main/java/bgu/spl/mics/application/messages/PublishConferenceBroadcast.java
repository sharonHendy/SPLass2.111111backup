package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.application.objects.Model;

import java.util.ArrayList;

public class PublishConferenceBroadcast implements Broadcast {

    private ArrayList<Model> modelsAndResults;

    public PublishConferenceBroadcast(ArrayList<Model> models){
        this.modelsAndResults = models;
    }

    public ArrayList<Model> getModelsAndResults() {
        return modelsAndResults;
    }
}

package bgu.spl.mics.application.objects;

import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Passive object representing information on a conference.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class ConfrenceInformation {
    private String name;

    private int date; //time of the conference
    private ArrayList<Model> modelsWithResults;

    public ConfrenceInformation(String name, int date){
        this.name = name;
        this.date = date;
        modelsWithResults = new ArrayList<>();
    }

    /**
     * adds tested model information,
     * the conference service will call this method when it gets publishResultEvent.
     * @param model
     */
    public void addResult(Model model){
        modelsWithResults.add(model);
    }

    public ArrayList<Model> getModelsWithResults() {
        return modelsWithResults;
    }

    public String getName() {
        return name;
    }

    public int getDate() {
        return date;
    }

    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("date",date);
        ArrayList<JSONObject> listOfModels = new ArrayList<>();
        for(Model m : modelsWithResults){
            listOfModels.add(m.toJson());
        }
        jsonObject.put("publications", listOfModels);
        return jsonObject;
    }
}

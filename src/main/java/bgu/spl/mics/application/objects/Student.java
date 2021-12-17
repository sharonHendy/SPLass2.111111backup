package bgu.spl.mics.application.objects;

import com.google.gson.JsonObject;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Passive object representing single student.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class Student {
    /**
     * Enum representing the Degree the student is studying for.
     */
    public  enum Degree {
        MSc, PhD
    }

    private String name;
    private String department;
    private Degree status;
    private int publications;
    private int papersRead;

    private ArrayList<Model> models;

    public Student(String name, String department, Degree status){
        this.name = name;
        this.department = department;
        this.status = status;
        publications = 0;
        papersRead = 0;
    }

    /**
     * gets the published models from the PublishConferenceBroadcast, increases publications if there
     * are models that are his, increases papers read for all the other models.
     * @param publishedModels
     */
    public void handlePublishConferenceBroadcast(ArrayList<Model> publishedModels){
        for(Model m : publishedModels){
            if(m.getStudent().equals(this)){
                publications = publications + 1;
            }else{
                papersRead = papersRead + 1;
            }
        }
    }


    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("department", department);
        jsonObject.put("status", status);
        jsonObject.put("publications", publications);
        jsonObject.put("papersRead", papersRead);
        ArrayList<Map<String,Object>> listOfModels = new ArrayList<>();
        for(Model m : models){
            if (m.getResult().equals(Model.Result.Good)){ //TODO???
                listOfModels.add(m.toJson());
            }
        }
        jsonObject.put("trainedModels", listOfModels);
        return jsonObject;
    }

    public void setModels(ArrayList<Model> models){
        this.models = models;
    }
    public ArrayList<Model> getModels() {
        return models;
    }
    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public Degree getStatus() {
        return status;
    }

    public int getPublications() {
        return publications;
    }

    public int getPapersRead() {
        return papersRead;
    }

}

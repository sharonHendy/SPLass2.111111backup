package bgu.spl.mics.application.objects;

import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Passive object representing a Deep Learning model.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class Model {

    public enum Status {PreTrained, Training, Trained, Tested}
    public enum Result {None, Good, Bad}


    private String name;
    private Data data;
    private Student student;
    private Status status;
    private Result result;


    public Model(String name, Data data, Student student){
        this.name = name;
        this.data = data;
        this.student = student;
        this.status = Status.PreTrained;
        this.result = Result.None;
    }

    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        JSONObject typeAndSize = new JSONObject();
        typeAndSize.put("type", data.getType()); //TODO: toString for Data.type
        typeAndSize.put("size", data.getSize());
        jsonObject.put("data", typeAndSize);
        jsonObject.put("status", status); //TODO
        jsonObject.put("result" , result); //TODO
        return jsonObject;
    }

    public void setResult(Result result){this.result = result;}

    public Data getData() {
        return data;
    }

    public Result getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public Student getStudent() {
        return student;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

}

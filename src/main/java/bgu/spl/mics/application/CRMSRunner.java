package bgu.spl.mics.application;

import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/** This is the Main class of Compute Resources Management System application. You should parse the input file,
 * create the different instances of the objects, and run the system.
 * In the end, you should output a text file.
 */
public class CRMSRunner {
    public static void main(String[] args) {
        ArrayList<Student> students = null;
        Cluster cluster = Cluster.getInstance();
        ArrayList<GPU> GPUs = null;
        ArrayList<CPU> CPUs = null;
        ArrayList<ConfrenceInformation> conferenceInformations = null;
        int tickTime = 0;
        int duration = 0;
        //MessageBusImpl messageBus = MessageBusImpl.getInstance();

        try {
            //JSONParser jsonParser = new JSONParser();
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(args[0]));
            Map<?,?> map = gson.fromJson(reader, Map.class);
            for(Map.Entry<?,?> entry : map.entrySet()){
                if(entry.getKey().equals("Students")){
                    ArrayList<?> arrOfStudents = (ArrayList<?>)entry.getValue();
                    students = buildStudents(arrOfStudents); //creates the students
                }else if(entry.getKey().equals("GPUS")){
                    ArrayList<String> arrOfGPU = (ArrayList<String>) entry.getValue();
                    GPUs = buildGPUs(arrOfGPU, cluster);
                }else if(entry.getKey().equals("CPUS")){
                    ArrayList<Double> arrOfCPU = (ArrayList<Double>) entry.getValue();
                    CPUs = buildCPUs(arrOfCPU, cluster);
                }else if(entry.getKey().equals("Conferences")){
                    ArrayList<LinkedTreeMap<?,?>> conferenceMap = (ArrayList<LinkedTreeMap<?,?>>) entry.getValue();
                    conferenceInformations = buildConferences(conferenceMap);
                }else if(entry.getKey().equals("TickTime")){
                    tickTime = ((Double)entry.getValue()).intValue();
                }else if(entry.getKey().equals("Duration")){
                    duration = ((Double)entry.getValue()).intValue();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        cluster.setGPUS(GPUs);
        cluster.setCPUS(CPUs);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for(ConfrenceInformation conference : conferenceInformations){
            String name = "conferenceService_" + conference.getName();
            executorService.execute(new ConferenceService(name, tickTime, conference));
        }
        for(CPU CPU : CPUs){
            String name = "CPUService_" + CPU.getCores() + "_";
            executorService.execute(new CPUService(name, CPU));
        }
        for(GPU GPU : GPUs){
            String name = "GPUService_" + GPU.getType() + "_";
            executorService.execute(new GPUService(name,GPU));
        }
        for( Student s : students) {
            String name = "studentService_"+ s.getName() + "_";
            executorService.execute(new StudentService(name , s));
        }
        executorService.execute(new TimeService(tickTime, duration));

        executorService.shutdown();
//        ArrayList<Thread> threads = new ArrayList<>();
//        cluster.setCPUS(CPUs);
//        cluster.setGPUS(GPUs);
//        int i = 1;
//        for(CPU CPU : CPUs){
//            String name = "CPUService_" + CPU.getCores() + "_" + i;
//            Thread CPUService = new Thread(new CPUService(name,CPU));
//            CPUService.start();
//            threads.add(CPUService);
//            i++;
//        }
//        i = 1;
//        for(GPU GPU : GPUs){
//            String name = "GPUService_" + GPU.getType() + "_" + i;
//            Thread GPUService = new Thread(new GPUService(name,GPU));
//            GPUService.start();
//            threads.add(GPUService);
//            i++;
//        }
//        i = 1;
//        for(ConfrenceInformation conference : conferenceInformations){
//            String name = "conferenceService_" + conference.getName();
//            Thread conferenceService = new Thread(new ConferenceService(name,conference.getDate(),tickTime));
//            conferenceService.start();
//            threads.add(conferenceService);
//        }
//
//        try {
//            sleep(1);
//        } catch (InterruptedException ignored) {}
//
//        for( Student s : students) {
//            String name = "studentService_"+ s.getName() + "_" + i;
//            Thread studentService = new Thread(new StudentService(name,s));
//            studentService.start();
//            threads.add(studentService);
//            i++;
//        }
//
//        Thread timeService = new Thread(new TimeService(tickTime,duration));
//        timeService.start();
//        threads.add(timeService);

//        for(Thread t : threads){
//            try {
//                t.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
        try {
            executorService.awaitTermination(Long.valueOf(duration + 5000),TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = buildJson(students,conferenceInformations, cluster, tickTime);
        try {
            FileWriter fileWriter = new FileWriter("./file.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(jsonObject.toJSONString());
            String prettyJsonStr = gson.toJson(je);
            fileWriter.write(prettyJsonStr);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static JSONObject buildJson(ArrayList<Student> students, ArrayList<ConfrenceInformation> conferences,
                                Cluster cluster, int tickTime){
        JSONObject jsonObject = new JSONObject();
        ArrayList<JSONObject> listOfStudents = new ArrayList<>();
        for(Student s : students){
            listOfStudents.add(s.toJson());
        }
        jsonObject.put("students", listOfStudents);
        ArrayList<JSONObject> listOfConferences = new ArrayList<>();
        for(ConfrenceInformation c : conferences){
            listOfConferences.add(c.toJson());
        }
        jsonObject.put("conferences", listOfConferences);
        jsonObject.put("cpuTimeUsed", cluster.getNumOfTimeUnitsUsedCPU() * tickTime);
        jsonObject.put("gpuTimeUsed", cluster.getNumOfTimeUnitsUsedGPU() * tickTime);
        jsonObject.put("batchesProcessed", cluster.getNumOfDBProcessed());
        return jsonObject;
    }
    static  ArrayList<ConfrenceInformation> buildConferences(ArrayList<LinkedTreeMap<?,?>> conferencesJson){
        ArrayList<ConfrenceInformation> conferences = new ArrayList<>();
        for(LinkedTreeMap<?,?> map : conferencesJson){
            conferences.add(new ConfrenceInformation((String)map.get("name"), ((Double)map.get("date")).intValue()));
        }
        return conferences;
    }
    static ArrayList<CPU> buildCPUs(ArrayList<Double> CPUsJson, Cluster cluster){
        ArrayList<CPU> CPUs = new ArrayList<>();
        for (Double cores : CPUsJson) {
            CPUs.add(new CPU(cores.intValue(), cluster));
        }
        return CPUs;

    }
    static ArrayList<GPU> buildGPUs(ArrayList<String> GPUsJson, Cluster cluster){
        ArrayList<GPU> GPUs = new ArrayList<>();
        for (String s : GPUsJson) {
            GPU.Type type;
            if (s.equals("RTX3090")) {
                type = GPU.Type.RTX3090;
            } else if (s.equals("RTX2080")) {
                type = GPU.Type.RTX2080;
            } else {
                type = GPU.Type.GTX1080;
            }
            GPUs.add(new GPU(type, cluster));
        }
        return GPUs;
    }
    //creates the students
    static ArrayList<Student> buildStudents(ArrayList<?> students){
        ArrayList<Student> studentsList = new ArrayList<>();
        for (Object o : students) {
            LinkedTreeMap<?, ?> studentDetails = (LinkedTreeMap<?, ?>) o;
            //gets details of students
            String name = "", department = "";
            Student.Degree status = Student.Degree.MSc;
            for (Map.Entry<?, ?> studentDetail : studentDetails.entrySet()) {
                if (studentDetail.getKey().equals("name")) {
                    name = studentDetail.getValue().toString();
                } else if (studentDetail.getKey().equals("department")) {
                    department = studentDetail.getValue().toString();
                } else if (studentDetail.getKey().equals("status")) {
                    if (studentDetail.getValue().toString().equals("MSc")) {
                        status = Student.Degree.MSc;
                    } else {
                        status = Student.Degree.PhD;
                    }
                } else if (studentDetail.getKey().toString().equals("models")) { //creates the models of the student
                    Student student = new Student(name, department, status);
                    studentsList.add(student);

                    ArrayList<?> models = (ArrayList<?>) studentDetail.getValue();
                    ArrayList<Model> modelOfStudent = new ArrayList<>();

                    for (LinkedTreeMap<?, ?> modelDetails : (ArrayList<LinkedTreeMap<?, ?>>) models) {
                        String nameOfModel = "";
                        Data.Type type = Data.Type.Text;
                        int size = 0;
                        for (Map.Entry<?, ?> modelDetail : modelDetails.entrySet()) {
                            if (modelDetail.getKey().equals("name")) {
                                nameOfModel = modelDetail.getValue().toString();
                            } else if (modelDetail.getKey().equals("type")) {
                                String typeStr = modelDetail.getValue().toString();
                                if (typeStr.equals("Images")) {
                                    type = Data.Type.Images;
                                } else if (typeStr.equals("Text")) {
                                    type = Data.Type.Text;
                                } else {
                                    type = Data.Type.Tabular;
                                }
                            } else if (modelDetail.getKey().equals("size")) {
                                size = ((Double)modelDetail.getValue()).intValue();
                            }
                        }
                        Model model = new Model(nameOfModel, new Data(type, size), student);
                        modelOfStudent.add(model);
                    }
                    student.setModels(modelOfStudent);
                }
            }
        }
        return studentsList;
    }
}

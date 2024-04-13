package replica2;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Appointment implements Serializable {
//    String appointmentID;
//    AppointmentTime time;
    private int capacity;
    private String ID;
    private ArrayList<String> users;
    private AppointmentType type;
    private Date date;

    public Appointment(String ID, AppointmentType type, int capacity) throws ParseException {
        this.ID = ID;
        this.capacity=capacity;
        users = new ArrayList();
        this.type=type;
        DateFormat df = new SimpleDateFormat("ddMMyy");
        df.setLenient(false);
        date = df.parse(ID.substring(4));
        /*DateFormat df = new SimpleDateFormat("ddMMyyyy");
        try {
            date = df.parse(ID.substring(4,8) + "20"+ID.substring(8));
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }*/
    }
    Appointment(int capacity,String ID, ArrayList<String> users, AppointmentType type, Date date){
        this.ID=ID;
        this.capacity=capacity;
        this.users = new ArrayList(users);
        this.type= type;
        this.date = date;
    }
    Appointment(String ID, String type, int capacity, String[] users) throws ParseException {
        this(ID,AppointmentType.valueOf(type),capacity);
        DateFormat df = new SimpleDateFormat("ddMMyy");
        df.setLenient(false);
        date = df.parse(ID.substring(4));
        this.users = new ArrayList<String>(Arrays.asList(users));
    }

    public Date getDate(){
        return date;
    }
    public int remainingCapacity(){
        return capacity - users.size();
    }
    public int getCapacity(){
        return capacity;
    }
    public ArrayList<String> getUsers(){
        return users;
    }
    public String getID(){
        return ID;
    }
    public String[] toStringArray(){
        ArrayList<String> list = new ArrayList();
        list.add(Integer.toString(capacity));
        list.add(ID);
        list.add(Integer.toString(users.size()));
        list.addAll(users);
        list.add(type.toString());
        list.add(Long.toString(date.getTime()));
        return list.toArray(new String[0]);
    }


    public AppointmentType getType(){
        return type;
    }
    public boolean book(String clientID){
        if(remainingCapacity()<=0){
            return false;
        }else{
            users.add(clientID);
            return true;
        }
    }
    public enum AppointmentType {
        Physician,
        Surgeon,
        Dental
    }
    public enum AppointmentTime {
        Morning,
        Afternoon,
        Evening
    }
}

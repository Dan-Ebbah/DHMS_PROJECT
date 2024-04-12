package com.webservice.hi;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebService(endpointInterface = "com.webservice.hi.AppointmentManager")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class AppointmentManagerImpl implements AppointmentManager {
    private ConcurrentHashMap<Appointment.AppointmentType, ConcurrentHashMap<String, Appointment>> appointments;
    private String code;
    private String logPath;
    private ConcurrentHashMap<String,ArrayList<Appointment>> userAppointments = new ConcurrentHashMap<>();

    public AppointmentManagerImpl(String hospitalCode) {
        super();
        code = hospitalCode;
        logPath = "logs/servers/"+ code+".txt";
        File logFile = new File(logPath);
        logFile.getParentFile().mkdirs();
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        appointments = new ConcurrentHashMap<>();
        appointments.put(Appointment.AppointmentType.Dental,new ConcurrentHashMap<>());
        appointments.put(Appointment.AppointmentType.Physician,new ConcurrentHashMap<>());
        appointments.put(Appointment.AppointmentType.Surgeon,new ConcurrentHashMap<>());
    }

    private boolean isAdmin(String patientID, boolean checkSameHospital){
        if(patientID.length() < 4){
            return false;
        }
        if(checkSameHospital && ! patientID.substring(0,3).equals(code)){
            return false;
        }
        return (patientID.charAt(3) == 'A');
    }
    @Override
    public boolean addAppointment1(String patientID, String appointmentID, String appointmentType, int capacity) {
        return addAppointment1(patientID,appointmentID, Appointment.AppointmentType.valueOf(appointmentType),capacity);
    }
    public boolean addAppointment1(String patientID, String appointmentID, Appointment.AppointmentType appointmentType, int capacity) {
        if(!isAdmin(patientID,true) || !patientID.substring(0,3).equals(appointmentID.substring(0,3)) || appointments.get(appointmentType).containsKey(appointmentID)){
            Logs.log(logPath,( "(addAppointment):" + patientID+" failed to add "+capacity + " " +appointmentType.toString()+" appointment(s) and returned false"));
            return false;
        }
        try{
            appointments.get(appointmentType).put(appointmentID,new Appointment(appointmentID,appointmentType,capacity));
        }catch(ParseException e){
            Logs.log(logPath,( "(addAppointment):" + patientID+" failed to add "+capacity + " " +appointmentType.toString()+" appointment(s) and returned false"));
            return false;
        }
        Logs.log(logPath,"(addAppointment):" + patientID+" successfully added "+capacity + " " +appointmentType.toString()+" appointment(s) and returned true");
        return true;
    }

    //returns false if there is no appointments at the specified time, else removes appointments and returns true

    @Override
    public boolean removeAppointment1(String patientID,String appointmentID, String appointmentType){
        return removeAppointment1(patientID,appointmentID, Appointment.AppointmentType.valueOf(appointmentType));
    }
    public boolean removeAppointment1(String patientID,String appointmentID, Appointment.AppointmentType appointmentType){
        if(!isAdmin(patientID,true) || !appointments.get(appointmentType).containsKey(appointmentID)){
            Logs.log(logPath,( "(removeAppointment):" + patientID+" failed to remove "+appointmentType.toString()+" appointment(s) "+ appointmentID +" and returned false"));
            return false;
        }
        Logs.log(logPath,( "(removeAppointment):" +patientID+" successfully removed "+appointmentType.toString()+" appointment(s) "+ appointmentID));

        //rebook old entries
        ArrayList<String> rebooks = appointments.get(appointmentType).remove(appointmentID).getUsers();
        for(String user:rebooks){
            for(int i=0;i<userAppointments.get(user).size();i++){
                if(userAppointments.get(user).get(i).getID().equals(appointmentID)){
                    userAppointments.get(user).remove(i);
                    break;
                }
            }
        }

        boolean success = true;
        for(String user:rebooks){
            if(!rescheduleAppointment(appointmentID,appointmentType, user)){
                success = false;
            }
        }
        if(success){
            Logs.log(logPath,( "(removeAppointment):" +patientID+" successfully rescheduled "+appointmentType+" appointment(s) "+ appointmentID +" and returned true"));
            return true;
        }
        Logs.log(logPath,( "(removeAppointment):" +patientID+" failed to reschedule all "+appointmentType+" appointment(s) "+ appointmentID +" and returned false"));
        return false;

    }

    //check if appointment id1 is before appointment id2
    private  boolean isBefore(String id1, String id2){
        if(Integer.parseInt(id1.substring(8)) > Integer.parseInt(id2.substring(8))) return false;
        if(Integer.parseInt(id1.substring(8)) < Integer.parseInt(id2.substring(8))) return true;
        if(Integer.parseInt(id1.substring(6,8)) > Integer.parseInt(id2.substring(6,8))) return false;
        if(Integer.parseInt(id1.substring(6,8)) < Integer.parseInt(id2.substring(6,8))) return true;
        if(Integer.parseInt(id1.substring(4,6)) > Integer.parseInt(id2.substring(4,6))) return false;
        if(Integer.parseInt(id1.substring(4,6)) < Integer.parseInt(id2.substring(4,6))) return true;
        if(id1.charAt(3) == 'M' && (id2.charAt(3)=='A' ||id2.charAt(3)=='E' ))return true;
        if(id1.charAt(3) == 'A' && (id2.charAt(3)=='E' ))return true;
        return false;
    }

    //returns next available appointment time
    private boolean rescheduleAppointment(String appointmentID, Appointment.AppointmentType type, String user){
        String nextAppointment = "ABCE999999";
        for(String id : appointments.get(type).keySet()){
            //check for next closest appointment
            if(appointments.get(type).get(id).remainingCapacity() <=0) continue;
            if(appointmentID.equals(id)) continue;
            if(isBefore(id,appointmentID)) continue;
            if(isBefore(nextAppointment,id)) continue;
            nextAppointment = id;
        }

        if(nextAppointment.equals("ABCE999999")){
            return false;
        }else{
            if(! bookAppointment1(user,nextAppointment,type)){
                return rescheduleAppointment(nextAppointment,type,user);
            }else{
                return true;
            }
        }
    }

    //returns arraylist of other hospitals
    private ArrayList<String> otherHospitalCodes(){
        ArrayList<String> codes = new ArrayList<String>();
        if(! code.equals("MTL")){
            codes.add("MTL");
        }
        if(! code.equals("QUE")){
            codes.add("QUE");
        }
        if(! code.equals("SHE")){
            codes.add("SHE");
        }
        return codes;
    }

    public byte[] listAppointmentAvailability1(String patientID, String appointmentType){
        return listAppointmentAvailability1(patientID, Appointment.AppointmentType.valueOf(appointmentType));
    }

    public byte[] listAppointmentAvailability1(String patientID, Appointment.AppointmentType appointmentType){
        if(! isAdmin(patientID, false)){
            Logs.log(logPath,( patientID+" failed to list all "+appointmentType.toString()+" appointments and returned null"));
            return null;
        }
        ArrayList<Appointment> appointmentList = new ArrayList<Appointment>();
        for(Appointment a:appointments.get(appointmentType).values()){
            appointmentList.add(a);
        }
        if(patientID.substring(0,3).equals(code)){
            ArrayList<String> hospitalCodes = otherHospitalCodes();

            for(String hCode : hospitalCodes){
                AppointmentManager stub = getStub(hCode);
                try {
                    appointmentList.addAll(Converter.ByteArrayToArrayList(stub.listAppointmentAvailability1(patientID, appointmentType.toString())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Logs.log(logPath,( patientID+" successfully listed all "+appointmentType.toString()+" appointments and returned the list of appointments"));
            for (Appointment a :appointmentList){
                System.out.println(a.getID());
            }
        }
        try {
            return Converter.ArrayListToByteArray(appointmentList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
/*
    private Appointment[] ArrayListToArray(ArrayList<Appointment> list){
        Appointment[] arr = new Appointment[list.size()];
        for(int i=0;i<arr.length;i++){
            arr[i]=list.get(i);
        }
        return arr;
    }*/

    @Override
    public boolean bookAppointment1(String patientID, String type, String appointmentID){
        return bookAppointment1(patientID,appointmentID, Appointment.AppointmentType.valueOf(type));
    }
    public boolean bookAppointment1(String patientID, String appointmentID, Appointment.AppointmentType type) {
        if(! appointmentID.substring(0,3).equals(code)){
            String name = appointmentID.substring(0,3);
            AppointmentManager stub = getStub(name);
            return stub.bookAppointment1(patientID,type.toString(),appointmentID);
        }
        if(userAppointments.get(patientID)==null){
            userAppointments.put(patientID,new ArrayList<>());
        }
        if(!patientID.substring(0,3).equals(code)){
            int count=0;
            DateFormat df = new SimpleDateFormat("ddMMyy");
            df.setLenient(false);
            Date date = null;
            try {
                date = df.parse(appointmentID.substring(4));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            for (Appointment a : userAppointments.get(patientID)){
                if(a.getDate().getYear()!=date.getYear()){
                    continue;
                }
                if(a.getDate().getMonth() == date.getMonth()){
                    count++;
                }
            }
            if(count>2){
                return false;
            }
        }
        boolean alreadyBooked = false;
        for( Appointment a :userAppointments.get(patientID)){
            if(a.getID().equals(appointmentID)){
                alreadyBooked = true;
                break;
            }
        }
        if(appointments.get(type).get(appointmentID) == null
                || alreadyBooked
                || (appointments.get(type).get(appointmentID.substring(0,3) + 'A' + appointmentID.substring(4))!= null
                    && appointments.get(type).get(appointmentID.substring(0,3) + 'A' + appointmentID.substring(4)).getUsers().contains(patientID))
                || (appointments.get(type).get(appointmentID.substring(0,3) + 'E' + appointmentID.substring(4))!= null
                    && appointments.get(type).get(appointmentID.substring(0,3) + 'E' + appointmentID.substring(4)).getUsers().contains(patientID))
                || (appointments.get(type).get(appointmentID.substring(0,3) + 'M' + appointmentID.substring(4)) != null
                    && appointments.get(type).get(appointmentID.substring(0,3) + 'M' + appointmentID.substring(4)).getUsers().contains(patientID))
                || appointments.get(type).get(appointmentID).remainingCapacity()<=0
        ){
            Logs.log(logPath,( patientID+" failed to book "+type.toString()+" appointment and returned false"));
            return false;
        }
        appointments.get(type).get(appointmentID).book(patientID);
        try{
            userAppointments.get(patientID).add(new Appointment(appointmentID,type,-1));
        }catch(Exception e){
            Logs.log(logPath,( patientID+" failed to book "+type.toString()+" appointment and returned false"));
            return false;
        }

        Logs.log(logPath,( patientID+" successfully booked "+type.toString()+" appointment and returned true"));
        return true;
    }


    @Override
    public byte[] getAppointmentSchedule1(String patientID){
        if(! patientID.substring(0,3).equals(code)){
            if(userAppointments.get(patientID) == null) return new byte[0];
            try {
                return Converter.ArrayListToByteArray(userAppointments.get(patientID));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ArrayList<String> hospitalCode = otherHospitalCodes();
        ArrayList<Appointment> appoints = new ArrayList<>();
        for(String hCode:hospitalCode){
            AppointmentManager stub = getStub(hCode);
            ArrayList<Appointment> aArray = new ArrayList<>();
            try {
                aArray = Converter.ByteArrayToArrayList(stub.getAppointmentSchedule1(patientID));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for(Appointment a : aArray){
                appoints.add(a);
            }
        }
        if(userAppointments.get(patientID) != null){
            appoints.addAll(userAppointments.get(patientID));
        }
        Logs.log(logPath,( patientID+" successfully returned appointment list"));
        try {
            return Converter.ArrayListToByteArray(appoints);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean cancelAppointment1(String patientID, String appointmentID) {
        if(! appointmentID.substring(0,3).equals(code)){
            try {
                String name = appointmentID.substring(0,3);
                AppointmentManager stub = getStub(name);
                return stub.cancelAppointment1(patientID,appointmentID);
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
        if(userAppointments.get(patientID)==null){
            Logs.log(logPath,( patientID+" failed to cancel appointment"+appointmentID ));
            return false;
        }
        int i;
        for(i=0;i<userAppointments.get(patientID).size();i++){
            if(userAppointments.get(patientID).get(i).getID().equals(appointmentID)){
                Appointment app = userAppointments.get(patientID).remove(i);
                appointments.get(app.getType()).get(appointmentID).getUsers().remove(patientID);
                i=-1;
                break;
            }
        }
        if(i!=-1){
            Logs.log(logPath,( patientID+" failed to cancel appointment"+appointmentID ));
            return false;
        }
        Logs.log(logPath,( patientID+" cancel appointment successfully "+appointmentID ));
        return true;
    }

    @Override
    public boolean swapAppointment1(String patientID, String oldID, String oldType, String newID, String newType){
        boolean found = false;
        Logs.num=1;
        try {
            for(Appointment a : Converter.ByteArrayToArrayList(getAppointmentSchedule1(patientID))){
                if(a.getID().equals(oldID)){
                    found = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(!found){
            Logs.log(logPath,patientID+" failed to swap appointments. old appointment did not exist");
            return false;
        }
        Logs.num=1;
        if (! bookAppointment1(patientID,newID, Appointment.AppointmentType.valueOf(newType))){
            Logs.log(logPath,patientID+" failed to swap appointments. new appointment can't be booked");
            return false;
        }
        return cancelAppointment1(patientID,oldID);//should be true since we checked
    }
    public boolean addAppointment1(String appointmentID, String appointmentType, int capacity){
        return addAppointment1(code + "A1234",appointmentID, Appointment.AppointmentType.valueOf(appointmentType),capacity);
    }
    public boolean removeAppointment1(String appointmentID, String appointmentType){
        return removeAppointment1(code + "A1234",appointmentID, Appointment.AppointmentType.valueOf(appointmentType));
    }

    public byte[] listAppointmentAvailability1(String appointmentType){
        return listAppointmentAvailability1(code+"A1234", Appointment.AppointmentType.valueOf(appointmentType));
    }
    private AppointmentManager getStub(String name){
        try {
            URL url = new URL("http://localhost:8080/"+name+"?wsdl");
            QName qname = new QName("http://hi.webservice.com/","AppointmentManagerImplService");
            Service service = Service.create(url,qname);
            return service.getPort(AppointmentManager.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String boolToMessage(boolean b){
        if(b){
            return "Successful";
        }else{
            return "Failure";
        }
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {
        return boolToMessage(addAppointment1(appointmentID,appointmentType,capacity));
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {
        return boolToMessage(removeAppointment1(appointmentID,appointmentType));
    }

    @Override
    public String bookAppointment(String patientID, String appointmentType, String appointmentID) {
        return boolToMessage(bookAppointment1(patientID,appointmentType,appointmentID));
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        return boolToMessage(cancelAppointment1(patientID,appointmentID));
    }

    String arraylistToMessage(ArrayList<Appointment> appointments,boolean isList){
        String str = "";
        for(Appointment a : appointments){
            String param;
            if(isList){
                param=a.getCapacity()+"";
            }else{
                param = a.getType().toString();
            }
            if(!str.equals("")){
                str = str.concat(","+a.getID()+"("+param+")");
            }else{
                str=a.getID();
            }
        }
        return str;
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        try {
            return arraylistToMessage(Converter.ByteArrayToArrayList(getAppointmentSchedule1(patientID)),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String listAppointmentAvailability(String appointmentType) {
        try {
            return arraylistToMessage(Converter.ByteArrayToArrayList(listAppointmentAvailability1(appointmentType)),true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentType, String oldAppointmentID, String newAppointmentType, String newAppointmentID) {
        return boolToMessage(swapAppointment1(patientID,oldAppointmentType,oldAppointmentID,newAppointmentType,newAppointmentID));
    }

    public String getInfo(){//returns info of only this hospital
        appointments.get(Appointment.AppointmentType.Dental);
        String info;
        ConcurrentHashMap<String, Appointment> a = new ConcurrentHashMap<>();
        a.putAll(appointments.get(Appointment.AppointmentType.Dental));
        a.putAll(appointments.get(Appointment.AppointmentType.Physician));
        a.putAll(appointments.get(Appointment.AppointmentType.Surgeon));
        info=hashMapToString(a);
        if(info.equals("")){
            return info;
        }
        return info.substring(1);
    }
    private String hashMapToString(ConcurrentHashMap<String,Appointment> hashMap){
        String str = "";
        for(String s: hashMap.keySet()){
            str = str.concat(";"+hashMap.get(s).getID() +":"                //appointmentID
                        + hashMap.get(s).getType().toString() +":"    //appointment type
                        + hashMap.get(s).getCapacity() +":"           //appointment capacity
                        + usersListToInfo(hashMap.get(s).getUsers())  //users
            );
        }
        return str;
    }
    private String usersListToInfo(List<String> users){
        String str = "";
        for(String s : users){
            if(!str.equals("")){
                str = str.concat(","+s);
            }else{
                str = str.concat(s);
            }
        }
        return str;
    }

    public void setInfo(String info){
        appointments = new ConcurrentHashMap<>();
        appointments.put(Appointment.AppointmentType.Dental,new ConcurrentHashMap<>());
        appointments.put(Appointment.AppointmentType.Physician,new ConcurrentHashMap<>());
        appointments.put(Appointment.AppointmentType.Surgeon,new ConcurrentHashMap<>());
        userAppointments = new ConcurrentHashMap<>();
        if(info.equals("")){
            return;
        }
        String[] appointments1 = info.split(";");
        for(String appointment: appointments1){
            String[] appointmentInfo = appointment.split(":");
            String ID =appointmentInfo[0];
            String type = appointmentInfo[1];
            int capacity = Integer.parseInt(appointmentInfo[2]);
            String[] users = appointmentInfo[3].split(",");
            Appointment a = null;
            try {
                for(String s :users){
                    if(userAppointments.get(s)==null){
                        userAppointments.put(s,new ArrayList<>());
                        userAppointments.get(s).add(new Appointment(ID, Appointment.AppointmentType.valueOf(type),-1));
                    }
                }
                a = new Appointment(ID,type,capacity,users);
                appointments.get(a.getType()).put(a.getID(),a);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

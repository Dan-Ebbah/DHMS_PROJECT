package com.webservice.hi;

import com.sun.net.httpserver.Authenticator;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class TestClient {
    static String logPath;
    static String clientID;
    static clientToFE stub;
    static FrontEndInterface fe;

    private static void setClient(String newClientID){
        clientID = newClientID;
        logPath = "logs/Client/"+clientID+".txt";
        File logFile = new File(logPath);
        logFile.getParentFile().mkdirs();
    }

    private static void setFE(String ip){
        try {
            URL url = new URL("http://"+ip+":8080/frontend?wsdl");
            QName qName = new QName("http://frontend/", "FrontEndImplService");
            QName qName2 = new QName("http://frontend/", "FrontEndImplPort");
            Service service = Service.create(url, qName);
            fe = service.getPort(qName2,FrontEndInterface.class);
            stub = new clientToFE(fe);
        } catch (MalformedURLException e) {
            fe=null;
            return;
            //throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
//        setFE("172.30.80.208");
        setFE("192.168.43.7");
        if(fe==null){
            System.out.println("Cannot connect to front end");
            return;
        }

        loginTest();

        addAndRemoveAppointmentTest();
        listTest();
        bookAppointmentAndCancelAppointmentTest();
        getScheduleTest();
        addingAndRemovingWithBookedAppointmentsTest();
        swapAppointmentTest();
//        if(!addAndRemoveAppointmentTest()){return;}
//        if(!listTest()) {return;}
//        if(!bookAppointmentAndCancelAppointmentTest()){return;}
//        if(!getScheduleTest()){return;}
//        if(!addingAndRemovingWithBookedAppointmentsTest()){return;}
//        if(!swapAppointmentTest()){return;}
    }
    private static void loginTest(){
        int counter=0;
        System.out.println("Login Test: \n");

        try{
            setClient("MTLA1234");
            counter+= new TestMessage("Login","Success", "Success").getResult();
        }catch(Exception e){
            counter+= new TestMessage("Login","Success", "Fail").getResult();
        }

        System.out.println("\n ("+counter + "/1) tests passed\n\n");
    }
    private static boolean addAndRemoveAppointmentTest(){//addAppointment(String ClientID, String AppointmentID, String appointmentType, int capacity);
        int counter=0;
        boolean broken=false;
        TestMessage t;
        System.out.println("addAppointment and removeAppointment tests: \n");
        do{
            setClient("MTLP1234");
            t = new TestMessage("Non-admin client ID adding appointment", "false",
                    stub.addAppointment(clientID, "MTLA121212", "Dental", 5));
            counter += t.getResult();

            setClient("MTLA1234");
            t = new TestMessage("Adding invalid appointment ID", "false",
                    stub.addAppointment(clientID, "MTLA131313", "Dental", 5));
            counter += t.getResult();

            t = new TestMessage("Admin client ID adding appointment", "true",
                    stub.addAppointment(clientID, "MTLA121212", "Dental", 5));
            counter += t.getResult();
            if(t.getResult()==0){
                broken=true;
                break;
            }

            t = new TestMessage("Admin client adding appointment to another server", "false",
                    stub.addAppointment(clientID, "MTLA121212", "Dental", 5));
            counter += t.getResult();

            t = new TestMessage("Admin client ID adding duplicate appointment", "false",
                    stub.addAppointment(clientID, "MTLA121212", "Dental", 5));
            counter += t.getResult();

            setClient("MTLP1234");
            t = new TestMessage("non-Admin client ID removing appointment", "false",
                    stub.addAppointment(clientID, "MTLA121212", "Dental", 5));
            counter += t.getResult();

            setClient("MTLA1234");
            t = new TestMessage("Admin client ID removing nonexistent appointment", "false",
                    stub.removeAppointment(clientID, "MTLA111111", "Dental"));
            counter += t.getResult();

            t = new TestMessage("Admin client ID removing appointment from other servers", "false",
                    stub.removeAppointment(clientID, "SHEA121212", "Dental"));
            counter += t.getResult();

            t = new TestMessage("Admin client ID removing existing appointment", "true",
                    stub.removeAppointment(clientID, "MTLA121212", "Dental"));
            counter += t.getResult();
            if(t.getResult()==0){
                broken=true;
                break;
            }
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("\n ("+counter + "/9) tests passed\n\n");
        return !broken;
    }
    private static boolean listTest() throws IOException {
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println("List Appointments tests: \n");
        do{
            setClient("MTLA1234");
            t = new TestMessage("Test with no appointments added",new String[0],
                    stub.listAppointmentAvailability(clientID,"Dental"));
            counter += t.getResult();

            stub.addAppointment(clientID,"MTLA010101","Dental",5);
            stub.addAppointment(clientID,"MTLA020202","Dental",5);
            t = new TestMessage("Test with multiple appointments added on the admin's server",new String[]{"MTLA010101", "MTLA020202"},
                    stub.listAppointmentAvailability(clientID,"Dental"));
            counter += t.getResult();

            setClient("SHEA1234");
            stub.addAppointment(clientID,"SHEA020202","Dental",5);
            stub.addAppointment(clientID,"SHEA010101","Dental",5);
            t = new TestMessage("Test with multiple appointments added on multiple servers",new String[]{"MTLA010101", "MTLA020202","SHEA020202","SHEA010101"},
                    stub.listAppointmentAvailability(clientID,"Dental"));
            counter += t.getResult();
            stub.removeAppointment(clientID,"SHEA020202","Dental");
            stub.removeAppointment(clientID,"SHEA010101","Dental");
            setClient("MTLA1234");
            stub.removeAppointment(clientID,"MTLA010101","Dental");
            stub.removeAppointment(clientID,"MTLA020202","Dental");
        }while(false);
        if(counter<3){
            broken=true;
        }
        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/3) tests passed\n\n");
        return !broken;
    }
    private static boolean bookAppointmentAndCancelAppointmentTest() throws IOException {
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println("Book appointment and cancel appointment tests: \n");
        do{
            setClient("MTLA1234");
            t = new TestMessage("Book appointment that doesn't exist","false",
                    stub.bookAppointment(clientID,"Dental","MTLA121212"));
            counter += t.getResult();

            stub.addAppointment(clientID,"MTLA121212","Dental",1);
            t = new TestMessage("Book valid appointment","true",
                    stub.bookAppointment(clientID,"Dental","MTLA121212"));
            counter += t.getResult();
            if(t.getResult()==0){
                broken=true;
                break;
            }

            setClient("SHEA1234");
            stub.addAppointment(clientID,"SHEA121212","Dental",5);
            setClient("MTLA1234");
            t = new TestMessage("Book valid appointment at another hospital","true",
                    stub.bookAppointment(clientID,"Dental","SHEA121212"));
            counter += t.getResult();
            if(t.getResult()==0){
                broken=true;
                break;
            }

            setClient("SHEA1234");
            stub.addAppointment(clientID,"SHEA111212","Dental",5);
            stub.addAppointment(clientID,"SHEA101212","Dental",5);
            stub.addAppointment(clientID,"SHEA131212","Dental",5);
            setClient("MTLA1234");
            stub.bookAppointment(clientID,"Dental","SHEA101212");
            stub.bookAppointment(clientID,"Dental","SHEA121212");
            t = new TestMessage("Book more than 3 appointment in a week at another hospital","true",
                    stub.bookAppointment(clientID,"Dental","SHEA131212"));
            counter += t.getResult();

            t = new TestMessage("Book appointment at same time","false",
                    stub.bookAppointment(clientID,"Dental","MTLA121212"));
            counter += t.getResult();

            t = new TestMessage("Book appointment at the same time for another type","false",
                    stub.bookAppointment(clientID,"Surgeon","MTLA121212"));
            counter += t.getResult();

            t = new TestMessage("Book appointment at the same time for another hospital","false",
                    stub.bookAppointment(clientID,"Dental","SHEA121212"));
            counter += t.getResult();

            setClient("MTLA4321");
            t = new TestMessage("Book appointment that is at max capacity","false",
                    stub.bookAppointment(clientID,"Dental","MTLA121212"));
            counter += t.getResult();

            setClient("MTLA1234");
            t = new TestMessage("cancel non-existing booking","false",
                    stub.bookAppointment(clientID,"Dental","MTLA11212"));
            counter += t.getResult();

            t = new TestMessage("cancel existing booking","false",
                    stub.bookAppointment(clientID,"Dental","MTLA121212"));
            counter += t.getResult();

            stub.removeAppointment(clientID,"MTLA121212","Dental");
            setClient("SHEA1234");
            stub.removeAppointment(clientID,"SHEA111212","Dental");
            stub.removeAppointment(clientID,"SHEA101212","Dental");
            stub.removeAppointment(clientID,"SHEA131212","Dental");
            stub.removeAppointment(clientID,"SHEA121212","Dental");
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/10) tests passed\n\n");
        return !broken;
    }

    private static boolean getScheduleTest() throws IOException {
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println("Get appointment schedule tests: \n");
        do{
            setClient("MTLP4321");
            t = new TestMessage("Get appointment schedule of client that has never booked an appointment",new String[] {"some fail"},
                    stub.getAppointmentSchedule(clientID));
            counter += t.getResult();

            setClient("MTLA1234");
            stub.addAppointment(clientID,"MTLA121212","Dental",5);
            setClient("SHEA1234");
            stub.addAppointment(clientID,"SHEA010101","Dental",5);
            setClient("QUEA1234");
            stub.addAppointment(clientID,"QUEA020202","Dental",5);
            stub.bookAppointment(clientID,"Dental","MTLA121212");
            stub.bookAppointment(clientID,"Dental","SHEA010101");
            stub.bookAppointment(clientID,"Dental","QUEA020202");
            t = new TestMessage("Get appointment schedule of client that bookings in multiple hospitals",new String[] {"MTLA121212", "SHEA010101", "QUEA020202"},
                    stub.getAppointmentSchedule(clientID));
            counter += t.getResult();
            setClient("MTLA1234");
            stub.removeAppointment(clientID,"MTLA121212","Dental");
            setClient("SHEA1234");
            stub.removeAppointment(clientID,"SHEA010101","Dental");
            setClient("QUEA1234");
            stub.removeAppointment(clientID,"QUEA020202","Dental");
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/2) tests passed\n\n");
        return !broken;
    }
    private static boolean addingAndRemovingWithBookedAppointmentsTest(){
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println("Adding and removing appointments with bookings tests: \n");
        do{
            setClient("MTLA1234");
            stub.addAppointment(clientID,"MTLA121212","Dental",5);
            stub.bookAppointment(clientID,"Dental","MTLA121212");
            t = new TestMessage("Removing appointment without appointments to reschedule bookings to","false",
                    stub.removeAppointment(clientID,"MTLA121212","Dental"));
            counter += t.getResult();

            stub.addAppointment(clientID,"MTLA121212","Dental",5);
            stub.addAppointment(clientID,"MTLA111111","Dental",5);
            stub.bookAppointment(clientID,"Dental","MTLA111111");
            t = new TestMessage("Removing appointment with appointments to reschedule bookings to","true",
                    stub.removeAppointment(clientID,"MTLA121212","Dental"));
            counter += t.getResult();
            stub.removeAppointment(clientID,"MTLA111111","Dental");
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/2) tests passed\n\n");
        return !broken;
    }
    private static boolean swapAppointmentTest(){
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println("Swap appointment tests: \n");
        do{
            setClient("MTLA1234");
            stub.addAppointment(clientID,"MTLA111111","Dental",5);
            stub.addAppointment(clientID,"MTLA121212","Dental",5);
            stub.addAppointment(clientID,"MTLA010101","Dental",0);

            t = new TestMessage("Old appointment doesn't exist","false",
                    stub.swapAppointment(clientID,"MTLA111110","Dental","MTLA121212","Dental"));
            counter += t.getResult();

            t = new TestMessage("Old appointment not booked","false",
                    stub.swapAppointment(clientID,"MTLA111111","Dental","MTLA121212","Dental"));
            counter += t.getResult();

            stub.bookAppointment(clientID,"Dental","MTLA111111");
            t = new TestMessage("New appointment doesn't exist","false",
                    stub.swapAppointment(clientID,"MTLA111111","Dental","MTLA121210","Dental"));
            counter += t.getResult();

            t = new TestMessage("New appointment not bookable","false",
                    stub.swapAppointment(clientID,"MTLA111111","Dental","MTLA010101","Dental"));
            counter += t.getResult();

            t = new TestMessage("Valid swap","true",
                    stub.swapAppointment(clientID,"MTLA111111","Dental","MTLA121212","Dental"));
            counter += t.getResult();

            stub.removeAppointment(clientID,"MTLA111111","Dental");
            stub.removeAppointment(clientID,"MTLA121212","Dental");
            stub.removeAppointment(clientID,"MTLA010101","Dental");
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/5) tests passed\n\n");
        return !broken;
    }
    /*

     private static boolean Test(){
        int counter=0;
        TestMessage t;
        boolean broken=false;
        System.out.println(" tests: \n");
        do{
            t = new TestMessage();
            counter += t.getResult();
        }while(false);

        if(broken){
            System.out.println("Cannot continue tests");
        }
        System.out.println("("+counter + "/???) tests passed\n\n");
        return !broken;
    }
    */
}
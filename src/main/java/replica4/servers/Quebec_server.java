package replica4.servers;

import replica4.Details;
import replica4.Interface;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@WebService(endpointInterface = "com.example.webservice.Interface")
@SOAPBinding(style= SOAPBinding.Style.RPC)
public class  Quebec_server implements Interface {


    ConcurrentHashMap<String, ConcurrentHashMap<String, Details>> app;

    private static final int POOL_SIZE2 = 3;
    private static final int POOL_SIZE = 3;

    private static final int UDP_PORT = 22222;
    private static final int MTL_PORT = 55555;
    private static final int SHE_PORT = 44444;


    private static final int UDP_PORT2 = 20022;
    private static final int MTL_PORT2 = 50055;
    private static final int SHE_PORT2 = 40044;

    private static final int LEN2= 220;
    private static final int LEN = 200;

    private static DatagramSocket UDPSocket;
    private static DatagramSocket UDPSocket2;
    static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy-hh-mm-ss");
    static Date D = new Date();
    static String strDate = sdf.format(D);
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> Map = new ConcurrentHashMap<>();

    public Quebec_server() throws SocketException {

        ConcurrentHashMap<String,ConcurrentHashMap<String,Details>> app = new ConcurrentHashMap<>();
        app.put("dental",new ConcurrentHashMap<>());
        app.put("surgeon",new ConcurrentHashMap<>());
        app.put("Physician",new ConcurrentHashMap<>());
        this.app= app;
        UDPSocket =new DatagramSocket(UDP_PORT);
        new Thread(this::receive_requests).start();
        UDPSocket2 =new DatagramSocket(UDP_PORT2);
        new Thread(this::receive_requests2).start();
    }






    /// Admin operations

    public String addAppointment(String appointment_Id, String appointment_type, int capacity) {

        if (app.containsKey(appointment_type)) {

            ConcurrentHashMap<String, Details> appointmentMap = app.get(appointment_type);

            if (appointmentMap.containsKey(appointment_Id)) {

                String status = "successful";
                String add = "Appointment_Already_Exists";
                admin_log1(add, status, appointment_Id);
                server_log1(add, status, appointment_Id);
                return status;

            } else {

                appointmentMap.put(appointment_Id, new Details(new ArrayList<>(), capacity));
                String status = "successful";
                String add = "Appointment_Added";
                admin_log1(add, status, appointment_Id);
                server_log1(add, status, appointment_Id);
                return status;
            }

        } else if (!app.containsKey(appointment_type)) {

            ConcurrentHashMap<String, Details> newAppointmentType = new ConcurrentHashMap<>();

            newAppointmentType.put(appointment_Id, new Details(new ArrayList<>(), capacity));
            app.put(appointment_type, newAppointmentType);

            String status = "successful";
            String add = "New_Appointment_Added";
            admin_log1(add, status, appointment_Id);
            server_log1(add, status, appointment_Id);
            return status;

        } else {
            String add = "Appointment_Not_Added";
            String status = "fail";
            admin_log1(add, status, appointment_Id);
            server_log1(add, status, appointment_Id);
            return status;

        }


    }



    public String removeAppointment(String appointment_Id, String appointment_type) {
        if (!(null == app.get(appointment_type).get(appointment_Id))) {

            app.get(appointment_type).remove(appointment_Id);
            String status = "successful";
            String add = "Appointment_remove";
            admin_log1(add, status, appointment_Id);
            server_log1(add, status, appointment_Id);
            return status;

        } else {

            String status = "successful";
            String add = "Appointment_Not_Exists";
            admin_log1(add, status, appointment_Id);
            server_log1(add, status, appointment_Id);
            return status;
        }
    }


    public String listAppointmentAvailability(String appointment_type) {
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        executor.submit(() -> Availability_In_MTL(appointment_type));
        executor.submit(() -> Availability_In_QUE(appointment_type));
        executor.submit(() -> Availability_In_SHE(appointment_type));

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Convert the ConcurrentHashMap to the desired string format
        StringBuilder resultBuilder = new StringBuilder();
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> entry : Map.entrySet()) {
            for (Map.Entry<String, Integer> innerEntry : entry.getValue().entrySet()) {
                resultBuilder.append(innerEntry.getKey()).append(":").append(innerEntry.getValue()).append(",");
            }
        }

        // Remove the trailing comma and space
        if (resultBuilder.length() > 0) {
            resultBuilder.setLength(resultBuilder.length() - 2);
        }

        return resultBuilder.toString();
    }


    private void Availability_In_QUE(String t) {
        app.forEach((appointment_type, appointment_details_Map) -> {
            if (appointment_type.equals(t) && !appointment_details_Map.isEmpty()) {

                Map.put("QUE", new ConcurrentHashMap<>());
                appointment_details_Map.forEach((appointmentID, appointmentDetails) ->
                        Map.get("QUE").put(appointmentID, appointmentDetails.get_capacity()));

            }
        });
    }

    private void Availability_In_MTL(String t) {

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket d =
                    new DatagramPacket(t.getBytes(StandardCharsets.UTF_8), t.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("localhost"), MTL_PORT);
            socket.send(d);
            while (true) {
                byte[] appointment_id_buffer = new byte[LEN];
                DatagramPacket appointment_id_packet = new DatagramPacket(appointment_id_buffer,LEN);
                socket.receive(appointment_id_packet);
                String appointment_id = new String(appointment_id_buffer).substring(0, appointment_id_packet.getLength());

                if (appointment_id.equals("completed")) {
                    break;
                }

                byte[] capacityBuffer = new byte[LEN];
                DatagramPacket capacityPacket = new DatagramPacket(capacityBuffer, LEN);
                socket.receive(capacityPacket);
                int capacity = Integer.parseInt(new String(capacityBuffer).substring(0, capacityPacket.getLength()));

                synchronized (Map) {
                    if (!Map.containsKey("MTL")) {
                        Map.put("MTL", new ConcurrentHashMap<>());
                    }
                    Map.get("MTL").put(appointment_id, capacity);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void Availability_In_SHE(String t) {
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket d =
                    new DatagramPacket(t.getBytes(StandardCharsets.UTF_8), t.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("localhost"), SHE_PORT);
            socket.send(d);
            while (true) {
                byte[] appointment_id_buffer = new byte[LEN];
                DatagramPacket appointment_id_Packet = new DatagramPacket(appointment_id_buffer, LEN);
                socket.receive(appointment_id_Packet);
                String appointment_id = new String(appointment_id_buffer).substring(0, appointment_id_Packet.getLength());

                if (appointment_id.equals("completed")) {
                    break;
                }

                byte[] capacityBuffer = new byte[LEN];
                DatagramPacket capacity = new DatagramPacket(capacityBuffer, LEN);
                socket.receive(capacity);
                int capacity1 = Integer.parseInt(new String(capacityBuffer).substring(0, capacity.getLength()));

                synchronized (Map) {
                    if (!Map.containsKey("SHE")) {
                        Map.put("SHE", new ConcurrentHashMap<>());
                    }
                    Map.get("SHE").put(appointment_id, capacity1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    private void receive_requests() {
        while (true) {
            try {
                byte[] buffer = new byte[LEN];
                DatagramPacket d = new DatagramPacket(buffer,LEN);
                UDPSocket.receive(d);
                InetAddress sender = d.getAddress();
                int senderPort = d.getPort();
                String type = new String(buffer).substring(0, d.getLength());
                app.forEach((appointmentType, appointment_details_map) -> {
                    if (appointmentType.equals(type)) {
                        appointment_details_map.forEach((appointmentID, appointmentDetails) -> {
                            DatagramPacket appointment_id_packet = new DatagramPacket(appointmentID.getBytes(StandardCharsets.UTF_8), appointmentID.getBytes(StandardCharsets.UTF_8).length, sender, senderPort);
                            DatagramPacket capacity = new DatagramPacket(String.valueOf(appointmentDetails.get_capacity()).getBytes(StandardCharsets.UTF_8), String.valueOf(appointmentDetails.get_capacity()).getBytes(StandardCharsets.UTF_8).length, sender, senderPort);
                            try (DatagramSocket socket = new DatagramSocket()) {
                                socket.send(appointment_id_packet);
                                socket.send(capacity);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });

                try (DatagramSocket socket = new DatagramSocket()) {
                    String completed = "completed";
                    DatagramPacket donePacket = new DatagramPacket(completed.getBytes(StandardCharsets.UTF_8), completed.getBytes(StandardCharsets.UTF_8).length, sender, senderPort);
                    socket.send(donePacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    ///patient operations

    public String bookAppointment(String patient_Id, String appointment_type, String appointment_Id)throws NullPointerException
    {
        String time = appointment_Id.substring(4);
        ConcurrentHashMap<String, Details> appointmentMap = app.get(appointment_type);
        if (app == null || appointment_type == null || appointment_Id == null || patient_Id == null) {
            // Handle null data structure or parameters
            return "fail";
        }
        else if (appointmentMap == null) {

            return "fail";
        }
        else if (app.get(appointment_type).get(appointment_Id).getIDList().contains(patient_Id)){
            return "fail";
        }
        else if (app.containsKey(appointment_type)) {
            final boolean[] containsSubstring = new boolean[1];

            app.get(appointment_type).forEach((appId, subMap) -> {
                if (appId.substring(4).equals(time)) {
                    if (subMap.getIDList().contains(patient_Id)) {
                        containsSubstring[0] = true;
                    }
                }
            });

            if (containsSubstring[0]) {
                return "fail";
            }
        }
        else if (app.get(appointment_type).get(appointment_Id).get_capacity()==0)
        {
            return "fail";


        }

        app.get(appointment_type).get(appointment_Id).addPatientToTheList(patient_Id);
        return "successful";


    }

    public String cancelAppointment(String patient_Id, String appointment_Id)   {


        Iterator it = app.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue(); // Retrieve the value as Object

            if (value instanceof ConcurrentHashMap) { // Check if the value is a HashMap
                ConcurrentHashMap<String, Details> appointmentDetails = (ConcurrentHashMap<String, Details>) value;

                if (appointmentDetails.containsKey(appointment_Id) &&
                        appointmentDetails.get(appointment_Id).getIDList().contains(patient_Id)) {
                    appointmentDetails.get(appointment_Id).removeID(patient_Id);
                    System.out.println("Appointment is Canceled");
                    return "successful";

                }
                else {
                    return "fail";
                }
            }
        }

        return null;

    }

    public String getAppointmentSchedule(String patient_Id) {
        ConcurrentHashMap<String, String> appointmentSchedule = new ConcurrentHashMap<>();
        Iterator it1 = app.entrySet().iterator();

        while (it1.hasNext()) {
            Map.Entry entry = (Map.Entry) it1.next();
            ConcurrentHashMap<String, Details> value = (ConcurrentHashMap<String, Details>) entry.getValue();
            Iterator it2 = value.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry entry1 = (Map.Entry) it2.next();

                Details value1 = (Details) entry1.getValue();
                if (value1.getIDList().contains(patient_Id)) {
                    appointmentSchedule.put(entry.getKey().toString(), entry1.getKey().toString());
                }
            }
        }

        // Convert HashMap to JSON-like string
        StringBuilder jsonBuilder = new StringBuilder("(");
        for (Map.Entry<String, String> entry : appointmentSchedule.entrySet()) {
            jsonBuilder.append(entry.getValue()).append("(").append(entry.getKey()).append("),");
        }
        // Remove the trailing comma if there's at least one entry
        if (!appointmentSchedule.isEmpty()) {
            jsonBuilder.setLength(jsonBuilder.length() - 1);
        }
        jsonBuilder.append(")");

        return jsonBuilder.toString();
    }




    public static void admin_log1(String add, String status,String ID) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("admin_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---ID--->").append(ID).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();
        }



    }


    public static void server_log1(String add, String status,String ID) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("D:/java_intellji/DHMS_PROJECT/src/main/java/replica4/server_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---ID--->").append(ID).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();
        }
    }



    public String swapAppointment(String patientID, String oldAppointmentType, String oldAppointmentID, String newAppointmentType, String newAppointmentID) throws NullPointerException{

        if (!app.containsKey(oldAppointmentType) || !app.get(oldAppointmentType).containsKey(oldAppointmentID) || !app.get(oldAppointmentType).get(oldAppointmentID).getIDList().contains(patientID)) {
            return "fail";
        }
        // Check availability of the new appointment at the new city branch server
        String availabilityCheckResult = checkAppointmentAvailability(patientID,oldAppointmentType,oldAppointmentID, newAppointmentType,newAppointmentID);
        if (!availabilityCheckResult.equals("successful")) {
            return "fail" ;
        }

        // Book the patient for the new appointment and cancel the old appointment atomically
        String bookingResult = bookAndCancelAppointment(patientID, oldAppointmentID, oldAppointmentType, newAppointmentID, newAppointmentType);
        if (!bookingResult.equals("successful")) {
            return "fail";
        }

        return "successful";
    }

    @Override
    public String getInfo(){
        String info;
        info = hashMapToString(app);
        if(info.equals("")){
            return info;
        }
        return info.substring(1);
    }

    private String hashMapToString(ConcurrentHashMap<String, ConcurrentHashMap<String, Details>> appointments){
        String str = "";
        appointments.forEach((type, idDetails) -> {
            idDetails.forEach((id, details) ->  {
                str.concat(";"+ id +":"                //appointmentID
                        + type +":"    //appointment type
                        + details.get_capacity() +":"           //appointment capacity
                        + usersListToInfo(details.getIDList()));
            });
        });
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

    @Override
    public void setInfo(String info){
        app.clear();
        app.put("Physician", new ConcurrentHashMap<>());
        app.put("Surgeon", new ConcurrentHashMap<>());
        app.put("Dental",new ConcurrentHashMap<>());
        if(info.equals("")){
            return;
        }
        String[] appointments1 = info.split(";");
        for(String appointment: appointments1){
            String[] appointmentInfo = appointment.split(":");
            String id =appointmentInfo[0];
            String type = appointmentInfo[1];
            int capacity = Integer.parseInt(appointmentInfo[2]);
            String[] users = appointmentInfo[3].split(",");
            app.get(type).put(id, new Details(Arrays.asList(users), capacity));
        }
    }


    private String bookAndCancelAppointment(String patientID, String oldappointmentId, String oldappointmenttype, String newAppointmentID, String newAppointmentType) {

        if ((Cancel_Appointment( patientID,oldappointmenttype,oldappointmentId).equals("successful")) && (!BookAppointment(patientID, newAppointmentType, newAppointmentID).equals("fail"))) {
            return "successful";
        } else {
            return "fail";
        }

    }

    public String checkAppointmentAvailability(String patientID,String oldappointmenttype, String oldappointmentId, String newAppointmentType, String newAppointmentId) {
        String city=newAppointmentId.substring(0,3);
        String result="";
        switch (city) {
            case "MTL":
                result= Availability_In_MTL2(patientID, oldappointmenttype, oldappointmentId, newAppointmentType, newAppointmentId);
                break;
            case "QUE":
                result= Availability_In_QUE2(patientID, oldappointmenttype, oldappointmentId, newAppointmentType, newAppointmentId);
                break;
            case "SHE":
                result= Availability_In_SHE2(patientID, oldappointmenttype, oldappointmentId, newAppointmentType, newAppointmentId);
                break;
        }


        return result;
    }


    private String Availability_In_QUE2(String patientID, String oldappointmenttype, String oldappointmentId, String newAppointmentType, String newAppointmentId ) {
        if ((app.get(newAppointmentType).containsKey(newAppointmentId)) && (app.get(newAppointmentType).get(newAppointmentId).get_capacity()!=0)){
            // Assuming patientID check logic

            return "successful";
        } else {
            return "fail";
        }
    }

    private String Availability_In_MTL2(String patientID, String oldappointmenttype, String oldappointmentId, String newAppointmentType, String newAppointmentId ) {
        // Send query to MTL server and receive response
        return queryCityServer(patientID, oldappointmenttype, oldappointmentId,  newAppointmentType, newAppointmentId ,MTL_PORT2);
    }

    private String Availability_In_SHE2(String patientID, String oldappointmenttype, String oldappointmentId, String newAppointmentType, String newAppointmentId ) {
        // Send query to SHE server and receive response
        return queryCityServer(patientID, oldappointmenttype, oldappointmentId,  newAppointmentType, newAppointmentId ,SHE_PORT2);
    }

    private String queryCityServer(String patientID, String oldappointmenttype, String oldappointmentId, String newAppointmentType, String newAppointmentId ,int port)throws NullPointerException {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Construct query message
            String queryMessage = "SWAP_APPOINTMENT" + ":"  + patientID + ":" + oldappointmenttype + ":" + oldappointmentId + ":" + newAppointmentType + ":" +  newAppointmentId;
            DatagramPacket queryPacket = new DatagramPacket(queryMessage.getBytes(StandardCharsets.UTF_8), queryMessage.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("localhost"), port);
            socket.send(queryPacket);
                // Receive response from the server
                byte[] responseBuffer = new byte[LEN2];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, LEN2);
                socket.receive(responsePacket);
                String response = new  String(responseBuffer).substring(0, responsePacket.getLength());
                return response;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String BookAppointment(String patientID,String newAppointmentType, String newAppointmentId) {
        String city=newAppointmentId.substring(0,3);
        String result="";
        switch (city) {
            case "MTL":
                result= Book_In_MTL2(patientID, newAppointmentType, newAppointmentId);
                break;
            case "QUE":
                result= Book_In_QUE2(patientID, newAppointmentType, newAppointmentId);
                break;
            case "SHE":
                result= Book_In_SHE2(patientID, newAppointmentType, newAppointmentId);
                break;
        }
        return result;
    }
    private String Book_In_QUE2(String patientID,  String newAppointmentType, String newAppointmentId ) {
        if (!bookAppointment(patientID,newAppointmentType, newAppointmentId).equals("fail")) {
            // Assuming patientID check logic
            return "successful";
        } else {
            return "fail";
        }
    }

    private String Book_In_MTL2(String patientID,String newAppointmentType, String newAppointmentId) {
        // Send query to QUE server and receive response
        return Booking(patientID,newAppointmentType, newAppointmentId, MTL_PORT2);
    }


    private String Book_In_SHE2(String patientID, String newAppointmentType, String newAppointmentId ){
        // Send query to SHE server and receive response
        return Booking(patientID,  newAppointmentType, newAppointmentId ,SHE_PORT2);
    }

    private String Booking(String patientID, String newAppointmenttype, String newAppointmentId ,int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Construct query message
            String queryMessage = "BOOKING_APPOINTMENT" + ":"  + patientID +":" + newAppointmenttype+ ":" +  newAppointmentId;
            DatagramPacket queryPacket = new DatagramPacket(queryMessage.getBytes(StandardCharsets.UTF_8), queryMessage.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("localhost"), port);
            socket.send(queryPacket);
            byte[] responseBuffer = new byte[LEN2];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, LEN2);
            socket.receive(responsePacket);
            String response = new  String(responseBuffer).substring(0, responsePacket.getLength());
            return response;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String Cancel_Appointment(String patientID,String oldappointmenttype, String oldappointmentId) {
        String city=oldappointmentId.substring(0,3);
        String result="";
        switch (city) {
            case "MTL":
                result= cancel_In_MTL2(patientID, oldappointmenttype, oldappointmentId);
                break;
            case "QUE":
                result= cancel_In_QUE2(patientID, oldappointmenttype, oldappointmentId);
                break;
            case "SHE":
                result= cancel_In_SHE2(patientID, oldappointmenttype, oldappointmentId);
                break;
        }
        return result;
    }


    private String cancel_In_QUE2(String patientID,  String  oldappointmenttype, String oldappointmentId ) {
        if (cancelAppointment1(patientID,oldappointmenttype, oldappointmentId).equals("successful")) {
            // Assuming patientID check logic
            return "successful";
        } else {
            return "fail";
        }
    }

    private String cancel_In_MTL2(String patientID,  String  oldappointmenttype, String oldappointmentId ) {
        // Send query to QUE server and receive response
        return canceling(patientID, oldappointmenttype, oldappointmentId , MTL_PORT2);
    }


    private String cancel_In_SHE2(String patientID,  String  oldappointmenttype, String oldappointmentId ){
        // Send query to SHE server and receive response
        return canceling(patientID, oldappointmenttype, oldappointmentId ,SHE_PORT2);
    }



    private String canceling(String patientID, String oldappointmenttype, String oldappointmentId,int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Construct query message
            String queryMessage = "CANCELING_APPOINTMENT" + ":"  + patientID +":" + oldappointmenttype + ":" + oldappointmentId;
            DatagramPacket queryPacket = new DatagramPacket(queryMessage.getBytes(StandardCharsets.UTF_8), queryMessage.getBytes(StandardCharsets.UTF_8).length, InetAddress.getByName("localhost"), port);
            socket.send(queryPacket);
            byte[] responseBuffer = new byte[LEN2];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, LEN2);
            socket.receive(responsePacket);
            String response = new  String(responseBuffer).substring(0, responsePacket.getLength());
            return response;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String cancelAppointment1(String patientID, String oldAppointmentType, String oldAppointmentID) {
        if (!app.containsKey(oldAppointmentType)) {
            return "fail";
        }

        ConcurrentHashMap<String, Details> appointmentDetails = app.get(oldAppointmentType);

        if (appointmentDetails.containsKey(oldAppointmentID) &&
                appointmentDetails.get(oldAppointmentID).getIDList().contains(patientID)) {
            appointmentDetails.get(oldAppointmentID).removeID(patientID);
            return "successful";
        } else {
            return "fail";
        }
    }

    private void receive_requests2() {
        while (true) {
            try {

                DatagramPacket requestPacket = receivePacket();
                String request = extractRequest(requestPacket);
                InetAddress sender = requestPacket.getAddress();
                int senderPort = requestPacket.getPort();

                String[] parts = request.split(":");
                String operation = parts[0];
                String result = "";

                if (operation.equals("SWAP_APPOINTMENT")) {
                    result = Availabilty(parts);
                }
                else if(operation.equals("BOOKING_APPOINTMENT"))
                {
                    result = Book(parts);
                }
                else if(operation.equals("CANCELING_APPOINTMENT"))
                {
                    result = cancel(parts);
                }
                sendResponse(sender, senderPort, result);



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[LEN2];
        DatagramPacket packet = new DatagramPacket(buffer, LEN2);
        UDPSocket2.receive(packet);
        return packet;
    }

    private String extractRequest(DatagramPacket packet) {
        return new String(packet.getData()).substring(0, packet.getLength());
    }

    private String Availabilty(String[] parts) {
        String patientID = parts[1];
        String oldAppointmentID = parts[2];
        String oldAppointmentType = parts[3];
        String newAppointmentID = parts[4];
        String newAppointmentType = parts[5];
        return Availability_In_QUE2(patientID, oldAppointmentID, oldAppointmentType, newAppointmentID, newAppointmentType);
    }


    private String Book(String[] parts)
    {
        String patientID = parts[1];
        String newAppointmentType = parts[2];
        String newAppointmentID = parts[3];

        return Book_In_QUE2(patientID,newAppointmentType, newAppointmentID);
    }

    private String cancel(String[] parts)
    {
        String patientID = parts[1];
        String oldAppointmentType = parts[2];
        String oldAppointmentID = parts[3];
        return cancel_In_QUE2(patientID,oldAppointmentType, oldAppointmentID);
    }

    private void sendResponse(InetAddress sender, int senderPort, String response) throws IOException {
        byte[] responseBuffer = response.getBytes(StandardCharsets.UTF_8);
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, sender, senderPort);
        UDPSocket2.send(responsePacket);
    }


}






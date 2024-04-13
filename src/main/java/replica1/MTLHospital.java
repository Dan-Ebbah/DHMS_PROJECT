package replica1;

import static java.lang.Thread.sleep;
import static replica1.utils.DateUtil.isSameDate;
import static replica1.utils.DateUtil.isSameWeek;

import replica1.pojo.AppointmentDetails;
import replica1.pojo.LogRecord;
import replica1.utils.HospitalUtil;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@WebService(endpointInterface="replica1.Hospital")
@SOAPBinding(style= SOAPBinding.Style.RPC)
public class MTLHospital implements Hospital {

    private static final int MY_UDP_PORT_1 = 11001;
    private static final int MY_UDP_PORT_2 = 11004;
    private static final int QUE_UDP_PORT_2 = 11005;
    private static final int SHE_UDP_PORT_2 = 11006;
    private static final int MAX_LEN = 100;

    private final HospitalUtil hospitalUtil;
    private final DatagramSocket myUDPSocket1;
    private final DatagramSocket myUDPSocket2;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments;
    private final ConcurrentHashMap<String, String> bookAppointmentsOnHold;

    public MTLHospital() throws SocketException {
        ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments = new ConcurrentHashMap<>();
        appointments.put("Physician", new ConcurrentHashMap<>());
        appointments.put("Surgeon", new ConcurrentHashMap<>());
        appointments.put("Dental", new ConcurrentHashMap<>());
        this.appointments = appointments;
        this.bookAppointmentsOnHold = new ConcurrentHashMap<>();
        myUDPSocket1 = new DatagramSocket(MY_UDP_PORT_1);
        myUDPSocket2 = new DatagramSocket(MY_UDP_PORT_2);
        hospitalUtil = new HospitalUtil();
        new Thread(() -> hospitalUtil.handleAvailabilityRequests(myUDPSocket1, appointments)).start();
        new Thread(this::handleAppointmentSwaps).start();
    }

    public void handleAppointmentSwaps() {
        while (true) {
            try {
                byte[] buffer = new byte[MAX_LEN];
                DatagramPacket datagram = new DatagramPacket(buffer, MAX_LEN);
                myUDPSocket2.receive(datagram);
                InetAddress senderHost = datagram.getAddress();
                int senderPort = datagram.getPort();
                String[] request = new String(buffer).substring(0, datagram.getLength()).split(" ");
                AtomicReference<String> result = new AtomicReference<>();

                if ("bookAppointment".equals(request[0])) {
                    String patientID = request[1];
                    String appointmentID = request[2];
                    String appointmentType = request[3];
                    result.set(bookAppointment(patientID, appointmentType, appointmentID));
                    bookAppointmentsOnHold.remove(appointmentType + "," + appointmentID);
                } else if ("cancelAppointment".equals(request[0])) {
                    String patientID = request[1];
                    String appointmentID = request[2];
                    result.set(cancelAppointment(patientID, appointmentID));
                } else {
                    String id = request[0];
                    String type = request[1];
                    String patientID = request[2];
                    result.set("false");
                    appointments.get(type).forEach((appointmentID, appointmentDetails) -> {
                        if (appointmentID.equals(id) && appointmentDetails.getCapacity() > 0) {
                            bookAppointmentsOnHold.put(type + "," + id, patientID);
                            result.set("true");
                        }
                    });
                }

                try (DatagramSocket socket = new DatagramSocket()) {
                    DatagramPacket resultPacket = new DatagramPacket(result.get().getBytes(StandardCharsets.UTF_8), result.get().getBytes(StandardCharsets.UTF_8).length, senderHost, senderPort);
                    socket.send(resultPacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String bookAppointment(String patientID, String appointmentType, String appointmentID) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "Book Appointment";
        String logId = patientID + " (patient ID) - " + appointmentID + " (appointment ID)";
        String logStatus = "Successful";
        String logResponse;

        Hospital queHospital = hospitalUtil.getHospital("QUE");
        Hospital sheHospital = hospitalUtil.getHospital("SHE");

        if (queHospital == null || sheHospital == null) {
            logStatus = "Failure";
            logResponse = "Something went wrong when processing the request. Contact hospital support team.";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        if (appointmentID.startsWith("QUE")) {
            return queHospital.bookAppointment(patientID, appointmentType, appointmentID);
        }

        if (appointmentID.startsWith("SHE")) {
            return sheHospital.bookAppointment(patientID, appointmentType, appointmentID);
        }

        // this will block all other users from booking the same appointment for which swap is going on
        int time = 0;
        while(bookAppointmentsOnHold.containsKey(appointmentType + "," + appointmentID)
                && (!bookAppointmentsOnHold.get(appointmentType + "," + appointmentID).equals(patientID))) {

            try {
                sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            time += 100;
            if (time >= 5000) {
                break;
            }
        }

        if (!patientID.startsWith("MTL")) {

            int appointmentCountInOtherHospitals;

            appointmentCountInOtherHospitals = countOfAppointmentsInSameWeek(appointmentID, patientID);
            if (patientID.startsWith("QUE")) {
                appointmentCountInOtherHospitals += sheHospital.countOfAppointmentsInSameWeek(appointmentID, patientID);
            } else if (patientID.startsWith("SHE")) {
                appointmentCountInOtherHospitals += queHospital.countOfAppointmentsInSameWeek(appointmentID, patientID);
            }

            if (appointmentCountInOtherHospitals >= 3) {
                logResponse = "Cannot book more than 3 appointments from other cities overall in a week";
                logStatus = "Failure";
                new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
                return logStatus;
            }
        }

        if (appointments.get(appointmentType).get(appointmentID) == null) {
            logResponse = "Appointment ID doesn't exist. So cannot be booked.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        if (appointments.get(appointmentType).get(appointmentID).getPatientIDList().contains(patientID)) {
            logResponse = "You have an existing appointment with same type and ID. So cannot be booked again.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        if (hospitalUtil.appointmentWithSameIDExists(appointmentID, patientID, appointments)) {
            logResponse = "You have an appointment with same ID and different type. So cannot book another in the same time slot.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        if (appointmentWithSameTypeOnSameDayExists(appointmentType, appointmentID, patientID)
                || queHospital.appointmentWithSameTypeOnSameDayExists(appointmentType, appointmentID, patientID)
                || sheHospital.appointmentWithSameTypeOnSameDayExists(appointmentType, appointmentID, patientID)) {
            logResponse = "You have an existing appointment with same type on the same day. So cannot be booked.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        if (appointments.get(appointmentType).get(appointmentID).getCapacity() == 0) {
            logResponse = "The appointment ID is full and doesn't have more capacity. So cannot be booked.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        appointments.get(appointmentType).get(appointmentID).addPatientID(patientID);
        logResponse = "Booked Appointment ID, " + appointmentID + " for the patient, " + patientID;
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
        return logStatus;
    }

    @Override
    public String getAppointmentSchedule(String patientID) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "Get Appointment Schedule";
        String logId = patientID;
        String logStatus = "Successful";
        String logResponse;

        Hospital queHospital = hospitalUtil.getHospital("QUE");
        Hospital sheHospital = hospitalUtil.getHospital("SHE");

        if (queHospital == null || sheHospital == null) {
            logStatus = "Failure";
            logResponse = "Something went wrong when processing the request. Contact hospital support team.";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logResponse;
        }

        StringBuffer appointmentSchedule = new StringBuffer(
                this.getAppointmentsOfPatient(patientID)
                + queHospital.getAppointmentsOfPatient(patientID)
                + sheHospital.getAppointmentsOfPatient(patientID));

        if (appointmentSchedule.length() != 0) {
            appointmentSchedule.replace(appointmentSchedule.length() - 1, appointmentSchedule.length(), "");
        }

        logResponse = appointmentSchedule.toString();
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();

        return logResponse;
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "Cancel Appointment";
        String logId = patientID + " (patient ID) - " + appointmentID + " (appointment ID)";;
        String logStatus = "Successful";
        String logResponse;

        if (appointmentID.startsWith("QUE")) {
            Hospital queHospital = hospitalUtil.getHospital("QUE");
            if (queHospital == null) {
                logStatus = "Failure";
                logResponse = "Something went wrong when processing the request. Contact hospital support team.";
                new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
                return logStatus;
            }
            return queHospital.cancelAppointment(patientID, appointmentID);
        } else if (appointmentID.startsWith("SHE")) {
            Hospital sheHospital = hospitalUtil.getHospital("SHE");
            if (sheHospital == null) {
                logStatus = "Failure";
                logResponse = "Something went wrong when processing the request. Contact hospital support team.";
                new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
                return logStatus;
            }
            return sheHospital.cancelAppointment(patientID, appointmentID);
        }

        AtomicBoolean validPairFlag = new AtomicBoolean(false);
        appointments.forEach((appointmentType, stringAppointmentDetailsHashMap) -> {
            if (stringAppointmentDetailsHashMap.containsKey(appointmentID)
                    && stringAppointmentDetailsHashMap.get(appointmentID).getPatientIDList().contains(patientID)) {

                validPairFlag.set(true);
                stringAppointmentDetailsHashMap.get(appointmentID).removePatientID(patientID);
            }
        });

        if (!validPairFlag.get()) {
            logResponse = "Patient ID, " + patientID + ", has no appointment for the appointment ID, " + appointmentID;
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        logResponse = "Patient ID, " + patientID + ", has cancelled the appointment for the appointment ID, " + appointmentID;
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
        return logStatus;
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "Add Appointment";
        String logId = appointmentID + " (" + appointmentType + ")";
        String logStatus = "Successful";
        String logResponse;

        if (appointments.get(appointmentType).get(appointmentID) != null) {
            logResponse = "Appointment ID already exists. You cannot add the same again.";
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        appointments.get(appointmentType).put(appointmentID, new AppointmentDetails(new Vector<>(), capacity));
        logResponse = "New appointment added";
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
        return logStatus;
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "Remove Appointment";
        String logId = appointmentID + " (" + appointmentType + ")";
        String logStatus = "Successful";
        String logResponse;

        if (appointments.get(appointmentType).get(appointmentID) == null) {
            logResponse = "Appointment ID, " + appointmentID + ", does not exist for the appointment type, " + appointmentType;
            logStatus = "Failure";
            new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
            return logStatus;
        }

        // this will block all other users from removing the appointment when a swap is going on
        int time = 0;
        while(bookAppointmentsOnHold.containsKey(appointmentType + "," + appointmentID)) {

            try {
                sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            time += 100;
            if (time >= 5000) {
                break;
            }
        }

        List<String> patientIDs = appointments.get(appointmentType).get(appointmentID).getPatientIDList();

        String additionalMessage = "";
        if (!patientIDs.isEmpty()) {
            String nextAppointmentID = hospitalUtil.getNextAvailableAppointment(appointmentID, appointmentType, patientIDs.size(), appointments);
            if (nextAppointmentID == null) {
                additionalMessage = "No next available appointment for patients associated with this Id. So cancelled all their appointments.";
            } else {
                patientIDs.forEach(patientID -> appointments.get(appointmentType).get(nextAppointmentID).addPatientID(patientID));
                additionalMessage = "Patients associated with this Id have been added to next available appointment.";
            }
        }
        appointments.get(appointmentType).remove(appointmentID);
        logResponse = "Appointment removed. " + additionalMessage;
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();
        return logStatus;
    }

    @Override
    public String listAppointmentAvailability(String type) {

        LocalDateTime logRequestTime = LocalDateTime.now();
        String logAction = "List Appointment Availability";
        String logId = null;
        String logStatus = "Successful";
        String logResponse;

        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> availabilityList = new ConcurrentHashMap<>();

        Thread t1 = new Thread(() -> hospitalUtil.findAvailabilityInThisCity("MTL", availabilityList, type, appointments));
        Thread t2 = new Thread(() -> hospitalUtil.findAvailabilityIn("QUE", availabilityList, type));
        Thread t3 = new Thread(() -> hospitalUtil.findAvailabilityIn("SHE", availabilityList, type));

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logResponse = hospitalUtil.convertAvailablityListToString(availabilityList);
        new LogRecord(logRequestTime, logAction, logId, logStatus, logResponse).addToLogsFile();

        return logResponse;
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {

        String logAction = "Swap Appointment";
        String logId = patientID + " (patient ID)";
        String logStatus = "Successful";
        String logResponse;

        // get new and old cities and set host and port attributes for new and old cities
        String newCity = newAppointmentID.substring(0, 3);
        String oldCity = oldAppointmentID.substring(0, 3);

        InetAddress newHost;
        InetAddress oldHost;
        int newPort;
        int oldPort;

        try {
            newHost = InetAddress.getByName("localhost");
            newPort = MY_UDP_PORT_2;
            switch (newCity) {
                case "MTL":
                    break;
                case "QUE":
                    newPort = QUE_UDP_PORT_2;
                    break;
                case "SHE":
                    newPort = SHE_UDP_PORT_2;
                    break;
            }

            oldHost = InetAddress.getByName("localhost");
            oldPort = MY_UDP_PORT_2;
            switch (oldCity) {
                case "MTL":
                    break;
                case "QUE":
                    oldPort = QUE_UDP_PORT_2;
                    break;
                case "SHE":
                    oldPort = SHE_UDP_PORT_2;
                    break;
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            // check if new appointment ID exists and has capacity for the new patient
            byte[] request = (newAppointmentID + " " + newAppointmentType + " " + patientID).getBytes(StandardCharsets.UTF_8);
            DatagramPacket datagram = new DatagramPacket(request, request.length, newHost, newPort);
            socket.send(datagram);

            byte[] response = new byte[MAX_LEN];
            DatagramPacket responsePacket = new DatagramPacket(response, MAX_LEN);
            socket.receive(responsePacket);

            if ("false".equals(new String(response).substring(0, responsePacket.getLength()))) {
                logResponse = "New appointment, " + newAppointmentID + " (type: " + newAppointmentType + ")," + " not available for booking";
                logStatus = "Failure";
                new LogRecord(LocalDateTime.now(), logAction, logId, logStatus, logResponse).addToLogsFile();
                return logStatus;
            }

            // send request to the server to cancel the old appointment
            request = ("cancelAppointment " + patientID + " " + oldAppointmentID + " " + oldAppointmentType).getBytes(StandardCharsets.UTF_8);
            datagram = new DatagramPacket(request, request.length, oldHost, oldPort);
            socket.send(datagram);

            response = new byte[MAX_LEN];
            responsePacket = new DatagramPacket(response, MAX_LEN);
            socket.receive(responsePacket);

            if (!new String(response).contains("Successful")) {
                logResponse = "Some issue with cancelling old appointment, " + oldAppointmentID + "(type: " + oldAppointmentType + ")";
                logStatus = "Failure";
                new LogRecord(LocalDateTime.now(), logAction, logId, logStatus, logResponse).addToLogsFile();
                return logStatus;
            }

            // send request to the server to book the new appointment
            request = ("bookAppointment " + patientID + " " + newAppointmentID + " " + newAppointmentType).getBytes(StandardCharsets.UTF_8);
            datagram = new DatagramPacket(request, request.length, newHost, newPort);
            socket.send(datagram);

            response = new byte[MAX_LEN];
            responsePacket = new DatagramPacket(response, MAX_LEN);
            socket.receive(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logResponse = "Swapped appointment, " + oldAppointmentID + " (type: " + oldAppointmentType + "), with, " + newAppointmentID + " (type: " + newAppointmentType + ")";
        new LogRecord(LocalDateTime.now(), logAction, logId, logStatus, logResponse).addToLogsFile();
        return logStatus;
    }

    @Override
    public int countOfAppointmentsInSameWeek(String appointmentID, String patientID) {

        AtomicInteger count = new AtomicInteger();
        appointments.forEach((type, appointmentIDMap) -> {
            appointmentIDMap.forEach((id, details) -> {
                if (isSameWeek(id, appointmentID) && details.getPatientIDList().contains(patientID)) {
                    count.getAndIncrement();
                }
            });
        });

        return count.get();
    }

    @Override
    public boolean appointmentWithSameTypeOnSameDayExists(String appointmentType, String appointmentID, String patientID) {

        AtomicBoolean appointmentExists = new AtomicBoolean(false);
        appointments.get(appointmentType).forEach((id, details) -> {
            if (isSameDate(id, appointmentID) && details.getPatientIDList().contains(patientID)) {
                appointmentExists.set(true);
            }
        });

        return appointmentExists.get();
    }

    @Override
    public String getAppointmentsOfPatient(String patientID) {

        HashMap<String, HashMap<String, List<String>>> appointmentIdsOfPatient = new HashMap<>();

        appointments.forEach((appointmentType, stringAppointmentDetailsHashMap) ->
                stringAppointmentDetailsHashMap.forEach((appointmentID, appointmentDetails) -> {
            if (appointmentDetails.getPatientIDList().contains(patientID)) {
                appointmentIdsOfPatient.computeIfAbsent("MTL", k -> new HashMap<>());
                appointmentIdsOfPatient.get("MTL").computeIfAbsent(appointmentType, k -> new ArrayList<>());
                appointmentIdsOfPatient.get("MTL").get(appointmentType).add(appointmentID);
            }
        }));

        StringBuffer stringBuffer = new StringBuffer();

        if (!appointmentIdsOfPatient.isEmpty()) {
            appointmentIdsOfPatient.get("MTL").forEach((type, idList) -> {
                idList.forEach(id -> stringBuffer.append(id).append("(").append(type).append("),"));
            });
        }

        return stringBuffer.toString();
    }

    @Override
    public String getInfo(){
        String info;
        info = hashMapToString(appointments);
        if(info.equals("")){
            return info;
        }
        return info.substring(1);
    }

    private String hashMapToString(ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments){
        StringBuffer stringBuffer = new StringBuffer();
        appointments.forEach((type, idDetails) -> {
            idDetails.forEach((id, details) ->  {
                stringBuffer.append(";").append(id).append(":"                //appointmentID
                ).append(type).append(":"    //appointment type
                ).append(details.getCapacity()).append(":"           //appointment capacity
                ).append(usersListToInfo(details.getPatientIDList()));
            });
        });
        return stringBuffer.toString();
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
        appointments.clear();
        appointments.put("Physician", new ConcurrentHashMap<>());
        appointments.put("Surgeon", new ConcurrentHashMap<>());
        appointments.put("Dental",new ConcurrentHashMap<>());
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
            appointments.get(type).put(id, new AppointmentDetails(Arrays.asList(users), capacity));
        }
    }
}

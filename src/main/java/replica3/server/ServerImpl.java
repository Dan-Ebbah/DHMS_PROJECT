package replica3.server;


import replica3.database.HashMapImpl;
import replica3.model.Appointment;
import replica3.model.AppointmentType;
import replica3.model.HospitalType;
import replica3.model.UDPServerInfo;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@WebService(targetNamespace = "http://localhost:8082/server")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ServerImpl implements ServerInterface {
    private HashMapImpl _database;
    private String successful = "successful";
    private String failure = "failure";
    private Logger logger;

    public ServerImpl(HashMapImpl database, int portNum, Logger logger) throws SocketException {
        _database = database;
        this.logger = logger;
//        createAndStartThread(portNum, getServerName());
    }

    public ServerImpl(HashMapImpl database, Logger logger) {
        _database = database;
        this.logger = logger;
    }

    public ServerImpl() {
    }

    public String getServerName() {
        return null;
    }

    private void createAndStartThread(int portNum, String serverName) throws SocketException {
        new Thread(new UDPServerThread(portNum, _database, serverName)).start();
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {
        System.out.println("Hello from the other side");
        if (isAppointmentPresent(appointmentID)) {
            String msg = "Could not add appointment, because appointment seems to already exist";
            logger.info(String.format(msg.concat(": Appointment Type = %s, Appointment ID = %s, Appointment Capacity = %d"), appointmentID, appointmentType, capacity));
            return failure;
        }

        Appointment appointment = new Appointment(appointmentID, AppointmentType.valueOf(appointmentType), capacity);
        _database.insert(appointment);
        String msg = "Successfully added the new appointment";
        logger.info(String.format(msg.concat(": Appointment Type = %s, Appointment ID = %s, Appointment Capacity = %d"), appointmentID, appointmentType, capacity));
        return successful;
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {
        if (isAppointmentPresent(appointmentID)) {
            String removeMsg = _database.remove(new Appointment(appointmentID, AppointmentType.valueOf(appointmentType)));
            logger.info(String.format(removeMsg.concat(": Appointment Type = %s, Appointment ID = %s"), appointmentID, appointmentType));
            return successful;
        }
        String s = "Appointment does not exist";
        logger.info(String.format(s.concat(": Appointment Type = %s, Appointment ID = %s"), appointmentID, appointmentType));
        return failure;
    }

    @Override
    public String bookAppointment(String patientID, String appointmentType, String appointmentID) {
        Appointment appointment = new Appointment(appointmentID, AppointmentType.valueOf(appointmentType));
        if (isAppointmentPresent(appointmentID)) {
            String msg = _database.book(patientID, appointment);
            logger.info(String.format(msg.concat(": Appointment Type = %s, Appointment ID = %s, Patient ID = %s"), appointmentID, appointmentType, patientID));
            return successful;
        }
        String msg = "Couldn't book appointment because Appointment ID is wrong";
        logger.info(String.format(msg.concat(": Appointment Type = %s, Appointment ID = %s, Patient ID = %s"), appointmentID, appointmentType, patientID));
        return failure;
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        if (isAppointmentPresent(appointmentID)) {
            String cancelMsg = _database.cancel(patientID, appointmentID);
            logger.info(String.format(cancelMsg.concat(": Appointment ID = %s, Patient ID = %s"), appointmentID, patientID));
            return successful;
        }
        String s = "Patient does not have the appointment booked";
        logger.info(String.format(s.concat(": Appointment ID = %s, Patient ID = %s"), appointmentID, patientID));
        return failure;
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        List<Appointment> appointmentList = _database.getByPatientId(patientID);
        StringBuilder msg = new StringBuilder();
        for (Appointment appointment : appointmentList) {
            msg.append(appointment.getAppointmentID());
            msg.append("(");
            msg.append(appointment.getAppointmentType().toString());
            msg.append("),");
        }
        logger.info((msg.append(": Patient ID = "+ patientID)).toString());
        return msg.toString();
    }

    @Override
    public String listAppointmentAvailability(String appointmentType) {
        AppointmentType convertedAppointmentType = AppointmentType.valueOf(appointmentType);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(_database.getAvailability(convertedAppointmentType));
        stringBuilder.append(" ");
        stringBuilder.append(getOthersAvailability(appointmentType));
        stringBuilder.append(" ");
        String msg = stringBuilder.toString();
        logger.info(msg);
        return msg;
    }

    public Boolean isBookableAndBooked(UDPServerInfo serverInfo, String newAppointmentID, String patientId) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress byAddress = InetAddress.getByName("localhost");
            String toSend = String.format("%d,%s/%s", 2, newAppointmentID, patientId);
            byte[] sendData = toSend.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, byAddress, serverInfo.getPort());

            socket.send(datagramPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket datagramPacket1 = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(datagramPacket1);

            String s = new String(datagramPacket1.getData(), 0, datagramPacket1.getLength());
            return Boolean.valueOf(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getOthersAvailability(String appointmentType) {
        StringBuilder stringBuilder = new StringBuilder();
        for(UDPServerInfo serverInfo : getOtherServersInfo()) {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress byAddress = InetAddress.getByName("localhost");
                String toSend = String.format("%d,%s", 1, appointmentType);
                byte[] sendData = toSend.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, byAddress, serverInfo.getPort());

                socket.send(datagramPacket);

                byte[] receiveData = new byte[1024];
                DatagramPacket datagramPacket1 = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(datagramPacket1);

                String s = new String(datagramPacket1.getData(), 0, datagramPacket1.getLength());
                stringBuilder.append(" ");
                stringBuilder.append(s);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return stringBuilder.toString();
    }

    protected UDPServerInfo[] getOtherServersInfo() {
        UDPServerInfo sherbrookeServerAddress = new UDPServerInfo("SherbrookeServerAddress", 5053);
        UDPServerInfo quebecServerAddress = new UDPServerInfo("QuebecServerAddress", 5051);

        return new UDPServerInfo[]{sherbrookeServerAddress, quebecServerAddress};
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentType, String oldAppointmentID, String newAppointmentType, String newAppointmentID) {
        String msg = "";
        if (!isAppointmentPresent(oldAppointmentID)) {
            msg = "Old Appointment does not seem to exist";
            logger.info(msg);
            return msg;
        }

        if (!isAppointmentPresent(newAppointmentID)) {
            msg = "New Appointment does not seem to exist";
            logger.info(msg);
            return msg;
        }

        Appointment oldAppointment = _database.findByAppointmentID(oldAppointmentID);

        if (!oldAppointment.getPatientID().equalsIgnoreCase(patientID)) {
            msg = String.format("You dont seem to have this appointment %s booked already", oldAppointmentID);
            logger.info(msg);
            return msg;
        }


        HospitalType hospitalTypeInfo = extractHospitalInfo(newAppointmentID);
        if (!isBookableAndBooked(hospitalTypeInfo.getHospitalServerAddress(), newAppointmentID, patientID)) {
            msg = "Could not swap appointment";
            logger.info(msg);
            return msg;
        }

        _database.cancel(patientID, oldAppointmentID);
        msg = "Appointments were successfully swapped ";
        logger.info(msg);
        return msg;
    }

    @Override
    public String getInfo() {//returns info of only this hospital
        String info;
        ConcurrentHashMap<String, Appointment> a = new ConcurrentHashMap<>();
        a.putAll(_database.getByAppointmentType(AppointmentType.Dental));
        a.putAll(_database.getByAppointmentType(AppointmentType.Physician));
        a.putAll(_database.getByAppointmentType(AppointmentType.Surgeon));
        info=hashMapToString(a).substring(1);
        return info;
    }
    private String hashMapToString(ConcurrentHashMap<String,Appointment> hashMap){
        String str = "";
        for(String s: hashMap.keySet()){
            str = str.concat(";"+hashMap.get(s).getAppointmentID() +":"                //appointmentID
                    + hashMap.get(s).getAppointmentType().toString() +":"    //appointment type
                    + hashMap.get(s).getCapacity() +":"           //appointment capacity
                    + usersListToInfo(hashMap.get(s).getPatientIDs())  //users
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

    @Override
    public void setInfo(String info) {
        //reset hashmaps here probably
        if(info.equals("")){
            return;
        }
        ConcurrentHashMap<AppointmentType, HashMap<String, Appointment>> newDB = null;
        String[] appointments = info.split(";");
        for(String appointment: appointments){
            String[] appointmentInfo = appointment.split(":");
            String apptId = appointmentInfo[0];
            AppointmentType appointmentType = AppointmentType.valueOf(appointmentInfo[1]);
            int appCapacity = Integer.parseInt(appointmentInfo[2]);
//            appointmentInfo[3].split(","); //TODO: Change

            HashMap<String, Appointment> apptDetails = new HashMap<>();
            apptDetails.put(apptId, new Appointment(apptId, appointmentType, appCapacity));
            newDB.put(appointmentType, apptDetails);
        }
    }

    private HospitalType extractHospitalInfo(String newAppointmentID) {
        String substring = newAppointmentID.substring(0, 3).toUpperCase();
        return HospitalType.findHospital(substring);
    }

    private boolean isAppointmentPresent(String appointmentID) {
        return _database.findByAppointmentID(appointmentID) != null;
    }

}

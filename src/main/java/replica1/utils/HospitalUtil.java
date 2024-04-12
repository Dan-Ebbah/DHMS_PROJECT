package replica1.utils;

import static replica1.utils.DateUtil.isLater;

import replica1.Hospital;
import replica1.pojo.AppointmentDetails;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HospitalUtil {

    private static final int MTL_UDP_PORT_1 = 11001;
    private static final int QUE_UDP_PORT_1 = 11002;
    private static final int SHE_UDP_PORT_1 = 11003;
    private static final int MAX_LEN = 100;

    public HospitalUtil() {}

    public void handleAvailabilityRequests(final DatagramSocket myUDPSocket1, final ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments) {
        while (true) {
            try {
                byte[] buffer = new byte[MAX_LEN];
                DatagramPacket datagram = new DatagramPacket(buffer, MAX_LEN);
                myUDPSocket1.receive(datagram);
                InetAddress senderHost = datagram.getAddress();
                int senderPort = datagram.getPort();
                String type = new String(buffer).substring(0, datagram.getLength());
                appointments.forEach((appointmentType, stringAppointmentDetailsHashMap) -> {
                    if (appointmentType.equals(type)) {
                        stringAppointmentDetailsHashMap.forEach((appointmentID, appointmentDetails) -> {
                            DatagramPacket appointmentIDPacket = new DatagramPacket(appointmentID.getBytes(StandardCharsets.UTF_8), appointmentID.getBytes(StandardCharsets.UTF_8).length, senderHost, senderPort);
                            DatagramPacket capacityPacket = new DatagramPacket(String.valueOf(appointmentDetails.getCapacity()).getBytes(StandardCharsets.UTF_8), String.valueOf(appointmentDetails.getCapacity()).getBytes(StandardCharsets.UTF_8).length, senderHost, senderPort);
                            try (DatagramSocket socket = new DatagramSocket()) {
                                socket.send(appointmentIDPacket);
                                socket.send(capacityPacket);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });

                try (DatagramSocket socket = new DatagramSocket()) {
                    String done = "done";
                    DatagramPacket donePacket = new DatagramPacket(done.getBytes(StandardCharsets.UTF_8), done.getBytes(StandardCharsets.UTF_8).length, senderHost, senderPort);
                    socket.send(donePacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void findAvailabilityInThisCity(String city, final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> availabilityList, final String type, final ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments) {

        appointments.forEach((appointmentType, stringAppointmentDetailsHashMap) -> {
            if (appointmentType.equals(type) && !stringAppointmentDetailsHashMap.isEmpty()) {
                availabilityList.put(city, new ConcurrentHashMap<>());
                stringAppointmentDetailsHashMap.forEach((appointmentID, appointmentDetails) ->
                        availabilityList.get(city).put(appointmentID, appointmentDetails.getCapacity()));
            }
        });
    }

    public void findAvailabilityIn(final String city, final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> availabilityList, final String type) {

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress host = InetAddress.getByName("localhost");
            int port = 0;
            switch (city) {
                case "MTL":
                    port = MTL_UDP_PORT_1;
                    break;
                case "QUE":
                    port = QUE_UDP_PORT_1;
                    break;
                case "SHE":
                    port = SHE_UDP_PORT_1;
                    break;
            }

            DatagramPacket datagram =
                    new DatagramPacket(type.getBytes(StandardCharsets.UTF_8), type.getBytes(StandardCharsets.UTF_8).length, host, port);
            socket.send(datagram);
            while (true) {
                byte[] appointmentIDBuffer = new byte[MAX_LEN];
                DatagramPacket appointmentIDPacket = new DatagramPacket(appointmentIDBuffer, MAX_LEN);
                socket.receive(appointmentIDPacket);
                String appointmentID = new String(appointmentIDBuffer).substring(0, appointmentIDPacket.getLength());

                if (appointmentID.equals("done")) {
                    break;
                }

                byte[] capacityBuffer = new byte[MAX_LEN];
                DatagramPacket capacityPacket = new DatagramPacket(capacityBuffer, MAX_LEN);
                socket.receive(capacityPacket);
                int capacity = Integer.parseInt(new String(capacityBuffer).substring(0, capacityPacket.getLength()));

                availabilityList.computeIfAbsent(city, k -> new ConcurrentHashMap<>());
                availabilityList.get(city).put(appointmentID, capacity);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Hospital getHospital(final String city) {

        try {
            switch (city) {
                case "MTL":
                    URL mtlHospitalURL = new URL("http://localhost:8080/mtlHospital?wsdl");
                    QName mtlQName = new QName("http://server.hospital.com/", "MTLHospitalService");
                    Service mtlService = Service.create(mtlHospitalURL, mtlQName);
                    return mtlService.getPort(Hospital.class);
                case "QUE":
                    URL queHospitalURL = new URL("http://localhost:8080/queHospital?wsdl");
                    QName queQName = new QName("http://server.hospital.com/", "QUEHospitalService");
                    Service queService = Service.create(queHospitalURL, queQName);
                    return queService.getPort(Hospital.class);
                case "SHE":
                    URL sheHospitalURL = new URL("http://localhost:8080/sheHospital?wsdl");
                    QName sheQName = new QName("http://server.hospital.com/", "SHEHospitalService");
                    Service sheService = Service.create(sheHospitalURL, sheQName);
                    return sheService.getPort(Hospital.class);
                default:
                    return null;
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNextAvailableAppointment(final String appointmentID, final String appointmentType, final int capacityNeeded, final ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments) {

        final String[] nextAppointment = {null};
        appointments.get(appointmentType).forEach((currentID, details) -> {
            if (isLater(currentID, appointmentID) && appointments.get(appointmentType).get(currentID).getCapacity() >= capacityNeeded) {
                if (nextAppointment[0] == null) {
                    nextAppointment[0] = currentID;
                } else {
                    if (isLater(nextAppointment[0], currentID)) {
                        nextAppointment[0] = currentID;
                    }
                }
            }
        });

        return nextAppointment[0];
    }

    public boolean appointmentWithSameIDExists(final String appointmentID, final String patientID, final ConcurrentHashMap<String, ConcurrentHashMap<String, AppointmentDetails>> appointments) {

        AtomicBoolean appointmentExists = new AtomicBoolean(false);
        appointments.forEach((type, idDetailsMap) -> {
            idDetailsMap.forEach((id, details) -> {
                if (id.equals(appointmentID) && details.getPatientIDList().contains(patientID)) {
                    appointmentExists.set(true);
                }
            });
        });

        return appointmentExists.get();
    }

    public String convertAvailablityListToString(final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> availabilityList) {

        final StringBuilder resultString = new StringBuilder();

        availabilityList.forEach((city, idCapacityMap) -> {
            idCapacityMap.forEach((id, capacity) -> resultString.append(id).append(":").append(capacity).append(","));
        });

        if (resultString.length() != 0) {
            resultString.replace(resultString.length() - 1, resultString.length(), "");
        }

        return resultString.toString();
    }
}

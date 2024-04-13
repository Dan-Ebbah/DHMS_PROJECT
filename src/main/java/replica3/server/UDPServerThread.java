package replica3.server;

import replica3.database.HashMapImpl;
import replica3.model.Appointment;
import replica3.model.AppointmentType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class UDPServerThread implements Runnable{
    private boolean running = true;
    private int portNumber;
    private DatagramSocket socket;
    private final HashMapImpl database;
    private byte[] buffer = new byte[1024];
    private String city;

    public UDPServerThread(int portNumber, HashMapImpl database, String city) throws SocketException {
        this.portNumber = portNumber;
        socket = new DatagramSocket(portNumber);
        this.database = database;
        this.city = city;
        System.out.printf("Server Started. Listening for %s Clients on port %d ....%n", city, portNumber);
    }

    @Override
    public void run() {
        System.out.println("Hello There I am waiting.....");
        running = true;
        while (running) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(datagramPacket);

                String s = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                System.out.println("I am tasked to search for appointments for " + s.toUpperCase());
                byte[] response = processCommand(s);

                DatagramPacket sendPacket = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
                socket.send(sendPacket);
            } catch (IOException e) {
                running = false;
                throw new RuntimeException(e);
            }
        }
        socket.close();
    }

    private byte[] processCommand(String s) {
        String trimmed = s.trim();
        String[] split = trimmed.split(",");
        if(split[0].equalsIgnoreCase("1")) {
          return listCurrentAvailability(split[1]).getBytes();
        } else {
            String[] bookingInfo = split[1].split("/");
            return isPresentAndBookable(bookingInfo[0], bookingInfo[1]).getBytes();
        }
    }

    private String listCurrentAvailability(String appointmentTypeString) {
        AppointmentType appointmentType = AppointmentType.valueOf(appointmentTypeString);
        String s = appointmentType == null ? "" : database.getAvailability(appointmentType);
        System.out.printf("I searched through [%s] for [%s] database and got back : ->  %s", city, appointmentTypeString ,s);
        System.out.println();
        return s;
    }

    private String isPresentAndBookable(String appointmentID, String patientID) {
        Appointment byAppointmentID = database.findByAppointmentID(appointmentID);
        if(byAppointmentID == null) {
            return "false";
        }

        if (byAppointmentID.getCapacity() <= 0) {
            return "false";
        }
        database.book(patientID, byAppointmentID);
        return "true";
    }

}

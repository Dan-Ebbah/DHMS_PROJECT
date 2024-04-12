package replica3;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaManager {
    private static final String sequencer_IP = "127.0.0.1";
    private static final int sequencer_Port = 2222;
    private static final int BUFFER_SIZE = 1024;
    public static final int CRASH_PORT = 3333;

    private static Process process;
    private static Replica replica;

    private static ReplicaManager [] otherRMs;
    //should start the servers too

    public static void main(String[] args) {
        replica = new Replica();

        new Thread(ReplicaManager::startNewThreadForOtherRMs).start();

        //should start and be ready to receive messages
        try {
            DatagramSocket rmSocket = getDatagramSocket(sequencer_Port);
            System.out.println("Replica Manager started...");

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(rmSocket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());
                acknowledgeReceipt(receivePacket, rmSocket);
                String[] splitMessage = messageReceived.split(":");
                String responseCommand = splitMessage[0];
                switch (responseCommand.trim().toLowerCase()) {
                    case "crash":
                        if (isCrashed()) {
                            List<String> dataList = getOtherRMsData();
                            setDataAndStartReplica(dataList);
                        }

                    case "byzantine":
                        // do something: start a new replica with the Db from other RMs
                        List<String> dataList = getOtherRMsData();
                        setDataAndStartReplica(dataList);
                    default:
                        forwardToReplica(splitMessage[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setDataAndStartReplica(List<String> dataList) {
        String concurrentHashMap;
        if (dataList.get(0).equalsIgnoreCase(dataList.get(1)) && dataList.get(0).equalsIgnoreCase(dataList.get(2))) {
            concurrentHashMap = (dataList.get(0));
        } else if (dataList.get(2).equalsIgnoreCase(dataList.get(0)) && dataList.get(2).equalsIgnoreCase(dataList.get(1))) {
            concurrentHashMap = (dataList.get(2));
        } else {
            concurrentHashMap = (dataList.get(1));
        }

        for (String city : replica.getServerWebDetails().keySet()){
            ReplicaInterface replicaInterface = connectToCityServerObject(city);
            replicaInterface.setInfo(concurrentHashMap);
        }
        startReplica();
    }

    private static List<String> getOtherRMsData() {
        String [] rmArray = {"", "", ""};
        List<String> result = new ArrayList<>(3);
        try {
            for (String ipAddress : rmArray) {
                InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, CRASH_PORT);
                DatagramSocket sendSocket = getDatagramSocket(socketAddress);
                sendCrashMessage(sendSocket, socketAddress);
                result.add(receiveDataBack(sendSocket));//TODO: might have to update instead of waiting

            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static String receiveDataBack(DatagramSocket socketAddress) {
        byte[] buffer = new byte[BUFFER_SIZE];
        String s;
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
        try {
            socketAddress.receive(ackPacket);
            s = new String(ackPacket.getData(), 0, ackPacket.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    private static void sendCrashMessage(DatagramSocket sendSocket, InetSocketAddress inetSocketAddress) {
        try{
            String toSend = "Crash Happened";
            byte[] sendData = toSend.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, inetSocketAddress.getAddress(), CRASH_PORT);
            sendSocket.send(datagramPacket);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(DatagramSocket sendSocket, InetAddress inetAddress, String message) {
        try{
            byte[] sendData = message.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, inetAddress, CRASH_PORT);
            sendSocket.send(datagramPacket);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCrashed() {
        String result = forwardToReplica("montreal addAppointment MTLA1111 DENTAL 1");
        return result.equalsIgnoreCase("SUCCESSFUL");
    }

    private static void startNewThreadForOtherRMs() {
        try {
            DatagramSocket socket = getDatagramSocket(CRASH_PORT);
            System.out.println("Started Listening for Crashes on port "+ CRASH_PORT);

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(socket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());

                String replicaData = getReplicaData();
                sendMessage(socket, receivePacket.getAddress(), replicaData);
            }
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }
    }

    private static ConcurrentHashMap convertBackToHashMap(String replicaData) {
        return null; //Provide the Appointment type here
    }

    private static DatagramPacket getDatagramPacket(DatagramSocket socket) throws IOException {
        byte[] receiveData = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    private static DatagramSocket getDatagramSocket(int crashPort) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(sequencer_IP, crashPort));
        return socket;
    }

    private static DatagramSocket getDatagramSocket(String address, int crashPort) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(address, crashPort));
        return socket;
    }

    private static DatagramSocket getDatagramSocket(InetSocketAddress address) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(address);
        return socket;
    }

    private static String getReplicaData() {
        StringBuilder hospitalsData = new StringBuilder();
        for (String city : replica.getServerWebDetails().keySet()) {
            ReplicaInterface replicaInterface = connectToCityServerObject(city);
            hospitalsData.append( replicaInterface.getInfo());
            hospitalsData.append("\\");
        }
        return hospitalsData.toString();
    }

    private static void acknowledgeReceipt(DatagramPacket recPacket, DatagramSocket socket) throws IOException {
        String receiptMessage = "Request Received";
        byte[] ackBytes = receiptMessage.getBytes();
        InetAddress clientAddress = recPacket.getAddress();
        int clientPort = recPacket.getPort();

        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, clientAddress, clientPort);
        socket.send(ackPacket);
    }

    private static String forwardToReplica(String request) {
        String[] splitRequestMessage = request.split(" ");
        String city = splitRequestMessage[0];
        String operation = splitRequestMessage[1];

        ReplicaInterface replicaInterface = connectToCityServerObject(city);
        if (replicaInterface == null) {
            System.out.println("ERROR as I could not connect to server object of " + city);
        }

        String result = callOperation(replicaInterface, operation, Arrays.copyOfRange(splitRequestMessage, 2, splitRequestMessage.length));
        return result;


    }

    private static String callOperation(ReplicaInterface replicaInterface, String operation, String [] parameters) {
        switch (operation) {
            case "addAppointment":
                return replicaInterface.addAppointment(parameters[0], parameters[1], Integer.parseInt(parameters[2]));
            case "removeAppointment":
                return replicaInterface.removeAppointment(parameters[0], parameters[1]);
            case "listAppointmentAvailability":
                return replicaInterface.listAppointmentAvailability(parameters[0]);
            case "bookAppointment":
                return replicaInterface.bookAppointment(parameters[0], parameters[1], parameters[2]);
            case "getAppointmentSchedule":
                return replicaInterface.getAppointmentSchedule(parameters[0]);
            case "cancelAppointment":
                return replicaInterface.cancelAppointment(parameters[0], parameters[1]);
            case "swapAppointment":
                return replicaInterface.swapAppointment(parameters[0], parameters[1], parameters[2], parameters[3], parameters[4]);
        }
        return null;
    }

    private static ReplicaInterface connectToCityServerObject(String city){
        try {
            Map<String, QName> serverWebDetails = replica.getServerWebDetails();
            String url = "http://localhost:8082/server/" + city.toLowerCase() + "?wsdl";
            URL urlLink = new URL(url);
            System.out.println("trying to connect to " + url);
            String s = getServerObjectService(serverWebDetails, city);
            QName qName = serverWebDetails.get(city);
//            QName qName2 = new QName("http://server/", "MontrealServerObjectImplPort");
            Service service = Service.create(urlLink, qName);
            return service.getPort(ReplicaInterface.class);
        } catch (Exception e) {
            System.out.println("Error e:" + e.getMessage());
        }

        return null;
    }

    private static String getServerObjectService(Map<String, QName> serverWebDetails, String city) {
        switch (city) {
            case "montreal":
                return "MontrealServerObjectImplService";
            case "sherbrooke":
                return "SherbrookeServerObjectImplService";
            case "quebec":
                return "QuebecServerObjectImplService";
        }
        return null;
    }

    public static void forwardToFrontEnd(DatagramPacket recPacket, DatagramSocket socket)
    {
        String sentence = new String(recPacket.getData(), 0, recPacket.getLength());
        String[] parts = sentence.split(" ");
        try {

            String finalMessage = parts[3];//requestId;
            byte[] data = finalMessage.getBytes();
            InetAddress FrontAddress = InetAddress.getByName(parts[1]);
            int FrontEndPort = Integer.parseInt(parts[2]);
            DatagramPacket packet = new DatagramPacket(data, data.length, FrontAddress,FrontEndPort);
            socket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startReplica() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", "replica1-shanmukha.jar", "replica1.HospitalServer");
            processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources\\"));
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopReplica() {
        process.destroyForcibly();
    }
}

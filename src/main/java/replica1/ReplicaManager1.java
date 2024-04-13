package replica1;

import replicaManager.ReplicaInterface;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplicaManager1 {
    private static final int sequencer_Port = 4444;
    private static final int BUFFER_SIZE = 1024;
    public static final int CRASH_PORT = 3333;

    private static Process process;


    public static void main(String[] args) {
        shutDownGracefullyAtTheTimeOfTermination();

        startReplica("replica1");
        new Thread(ReplicaManager1::startNewThreadForOtherRMs).start();

        //should start and be ready to receive messages
        try {
            DatagramSocket rmSocket = getDatagramSocket(sequencer_Port);
            System.out.println("Replica Manager started...");

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(rmSocket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());
                acknowledgeReceipt(receivePacket, rmSocket);
                String[] splitMessage = messageReceived.split(" : ");
                String responseCommand = splitMessage[0];
                switch (responseCommand.trim().toLowerCase()) {
                    case "crash":
                        if (isCrashed()) {
                            List<String> dataList = getOtherRMsData();
                            startReplica("replica1");
                            setData(dataList);
                        }
                    case "byzantine":
                        // do something: start a new replica with the Db from other RMs
                        List<String> dataList = getOtherRMsData();
                        stopReplica();
                        startReplica("replica1");
                        setData(dataList);
                    default:
                        String responseFromReplica = forwardToReplica(splitMessage[1]);
                        forwardToFrontEnd(responseFromReplica, responseCommand);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setData(List<String> dataList) {
        String concurrentHashMap;
        if (dataList.get(0).equalsIgnoreCase(dataList.get(1)) && dataList.get(0).equalsIgnoreCase(dataList.get(2))) {
            concurrentHashMap = (dataList.get(0));
        } else if (dataList.get(2).equalsIgnoreCase(dataList.get(0)) && dataList.get(2).equalsIgnoreCase(dataList.get(1))) {
            concurrentHashMap = (dataList.get(2));
        } else {
            concurrentHashMap = (dataList.get(1));
        }

        String[] concurrentHashMapCities =  concurrentHashMap.split("/");

        int i = 0;
        for (String city : new String[] {"MTL", "QUE", "SHE"}) {
            replicaManager.ReplicaInterface replicaInterface = connectToCityServerObject(city);
            replicaInterface.setInfo(concurrentHashMapCities[i]);
            i ++;
        }
    }

    private static List<String> getOtherRMsData() {
        String [] rmArray = {"192.168.43.254", "192.168.43.251", "192.168.43.159"};
        List<String> result = new ArrayList<>(3);
        try {
            for (String ipAddress : rmArray) {
                InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, CRASH_PORT);
                DatagramSocket sendSocket = getDatagramSocket(socketAddress);
                sendCrashMessage(sendSocket, socketAddress);
                result.add(receiveDataBack(sendSocket));

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
        String result = forwardToReplica("MTL listAppointmentAvailability Dental");
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

    private static DatagramPacket getDatagramPacket(DatagramSocket socket) throws IOException {
        byte[] receiveData = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    private static DatagramSocket getDatagramSocket(int crashPort) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(crashPort));
        return socket;
    }

    private static DatagramSocket getDatagramSocket(InetSocketAddress address) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(address);
        return socket;
    }

    private static String getReplicaData() {
        StringBuilder hospitalsData = new StringBuilder();
        for (String city : new String[]{"MTL", "QUE", "SHE"}) {
            replicaManager.ReplicaInterface replicaInterface = connectToCityServerObject(city);
            hospitalsData.append(replicaInterface.getInfo());
            hospitalsData.append("/");
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
        System.out.println(request);
        String[] splitRequestMessage = request.split(" ");
        String city = splitRequestMessage[0];
        String operation = splitRequestMessage[1];

        replicaManager.ReplicaInterface replicaInterface = connectToCityServerObject(city);
        if (replicaInterface == null) {
            System.out.println("ERROR as I could not connect to server object of " + city);
        }

        String result = callOperation(replicaInterface, operation, Arrays.copyOfRange(splitRequestMessage, 2, splitRequestMessage.length));
        System.out.println(result);
        return result;
    }

    private static String callOperation(replicaManager.ReplicaInterface replicaInterface, String operation, String [] parameters) {
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

    private static replicaManager.ReplicaInterface connectToCityServerObject(String city){
        try {
            String url = "http://localhost:8080/" + city.toLowerCase() + "Hospital" + "?wsdl";
            URL urlLink = new URL(url);
            QName qname = new QName("http://replica1/",city.toUpperCase() + "HospitalService");
            QName qName2 = new QName("http://replica1/", city.toUpperCase() + "HospitalPort");
            Service service = Service.create(urlLink,qname);
            return service.getPort(qName2, ReplicaInterface.class);
        } catch (Exception e) {
            System.out.println("Error e:" + e.getMessage());
        }
        return null;
    }

    public static void forwardToFrontEnd(String message, String request)
    {
        try {
            String[] requestArgs = request.split(" ");//requestId;
            InetAddress FrontAddress = InetAddress.getByName(requestArgs[1]);
            int FrontEndPort = Integer.parseInt(requestArgs[2]);
            String updatedMessage = "1 " + requestArgs[3] + " " + message;
            System.out.println(updatedMessage);
            DatagramPacket packet = new DatagramPacket(updatedMessage.getBytes(StandardCharsets.UTF_8), updatedMessage.getBytes(StandardCharsets.UTF_8).length, FrontAddress,FrontEndPort);
            new DatagramSocket().send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startReplica(String replica) {
        ProcessBuilder processBuilder;
        try {
            switch (replica) {
                case "replica1":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica1-shanmukha.jar", "replica1.HospitalServer");
                    processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
                case "replica2":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica2-alain.jar", "replica2.HospitalServer");
                    processBuilder.directory(new File("C:\\Users\\potat\\Desktop\\assignments\\comp6231\\project\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
                case "replica3":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica3-daniel.jar", "replica3.HospitalServer");
                    processBuilder.directory(new File("/Users/danieldan-ebbah/Documents/School/MACS/COMP_6231/Assignments/code/DHMS_PROJECT/src/main/resources/"));
                    process = processBuilder.start();
                    break;
                case "replica4":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica4-naveen.jar", "replica4.HospitalServer");
                    processBuilder.directory(new File("D:\\java_intellji\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopReplica() {
        System.out.println("Stopping replica...");
        process.destroyForcibly();
    }

    public static void shutDownGracefullyAtTheTimeOfTermination() {
        Runtime.getRuntime().addShutdownHook(new Thread(ReplicaManager1::stopReplica));
    }
}
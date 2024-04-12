package replicaManager;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicaManager {
    private static final String sequencer_IP = "127.0.0.1";
    private static final int sequencer_Port = 2222;
    private static final int BUFFER_SIZE = 1024;
    public static final int CRASH_PORT = 3333;

    private static Process process;

    private static Replica replica;
    //should start the servers too

    public static void main(String[] args) {
        replica = new Replica();
        new Thread(ReplicaManager::startNewThreadForOtherRMs);

        //should start and be ready to receive messages
        try {
            DatagramSocket rmSocket = getDatagramSocket(sequencer_Port);
            System.out.println("Replica Manager started...");

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(rmSocket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());
                acknowledgeReceipt(receivePacket, rmSocket);
                String[] splitMessage = messageReceived.split(":");

                forwardToReplica(splitMessage[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startNewThreadForOtherRMs() {
        try {
            DatagramSocket socket = getDatagramSocket(CRASH_PORT);
            System.out.println("Started Listening for Crashes on port "+ CRASH_PORT);

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(socket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // if needed to get data from RM
                ConcurrentHashMap concurrentHashMap = convertBackToHashMap(getReplicaData());
                replica = new Replica(concurrentHashMap);

                //if restartReplica is needed then
                startReplica("replica1");
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

    private static void acknowledgeReceipt(DatagramPacket recPacket, DatagramSocket socket) throws IOException {
        String receiptMessage = "Request Received";
        byte[] ackBytes = receiptMessage.getBytes();
        InetAddress clientAddress = recPacket.getAddress();
        int clientPort = recPacket.getPort();

        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, clientAddress, clientPort);
        socket.send(ackPacket);
    }

    private static void forwardToReplica(String request) {
        String[] splitRequestMessage = request.split(" ");
        String city = splitRequestMessage[0];

        connectToCityServerObject(city);



    }

    private static void connectToCityServerObject(String city){
        try {
            String url = "http://localhost:8082/server/" + city.toLowerCase() + "?wsdl";
            URL urlLink = new URL(url);
            System.out.println("trying to connect to " + url);
            QName qName = new QName("http://server/", "MontrealServerObjectImplService");
            QName qName2 = new QName("http://server/", "MontrealServerObjectImplPort");
            Service service = Service.create(urlLink, qName);
            ReplicaInterface replicaInterface = service.getPort(qName2, ReplicaInterface.class);

            String ss = replicaInterface.addAppointment(" ", "Dental", 1);
            System.out.println("Result gotten: " + ss);
            System.out.println();
        } catch (Exception e) {
            System.out.println("Error e:" + e.getMessage());
        }

    }

    private static String getReplicaData() {
        return replica.getAllDataFromReplica();
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
                    processBuilder = new ProcessBuilder("java", "-cp", "replica2-alain.jar", "replica2.Servers");
                    processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
                case "replica3":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica3-daniel.jar", "replica3.<Class>");
                    processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
                case "replica4":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica4-naveen.jar", "replica4.<Class>");
                    processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources\\"));
                    process = processBuilder.start();
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopReplica() {
        process.destroyForcibly();
    }
}

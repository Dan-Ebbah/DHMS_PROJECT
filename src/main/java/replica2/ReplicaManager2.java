package replica2;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplicaManager2 {
    private static final int RM_RECEIVE_PORT_FOR_SEQUENCER = 4444;
    public static final int LISTEN_PORT_FOR_DATA_REQUESTS = 3333;
    private static final int BUFFER_SIZE = 1024;
    private static final String[] RM_HOSTS;
    private static final HashMap<Integer, String> RM_NUMBER_HOST_MAP;

    private static Process process;
    private static AtomicBoolean handlingCrash = new AtomicBoolean(false);
    private static AtomicBoolean handlingByzantine = new AtomicBoolean(false);
    private static int byzantineCount = 0;
    private static AtomicInteger lastExecutedSeqNumber = new AtomicInteger(0);
    private static String runningReplica = "";
    private static String messageWhoseExecStoppedInMiddle = "";

    static {
        RM_HOSTS = new String[] {"192.168.2.37", "192.168.2.1", "192.168.2.2"};
        RM_NUMBER_HOST_MAP = new HashMap<>();
        RM_NUMBER_HOST_MAP.put(1, RM_HOSTS[0]);
        RM_NUMBER_HOST_MAP.put(3, RM_HOSTS[1]);
        RM_NUMBER_HOST_MAP.put(4, RM_HOSTS[2]);
    }

    public static void main(String[] args) {
        shutDownGracefullyAtTheTimeOfTermination();

        startReplica("replica2");
        new Thread(ReplicaManager2::startNewThreadForOtherRMs).start();

        //should start and be ready to receive messages
        try {
            DatagramSocket rmSocket = getDatagramSocket(RM_RECEIVE_PORT_FOR_SEQUENCER);
            System.out.println("Replica Manager started...");

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(rmSocket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("messageReceived: " + messageReceived);
                if (handlingCrash.get() || handlingByzantine.get()) {
                    System.out.println("Handling crash or byzantine");
                    continue;
                }
                if (!(messageReceived.startsWith("crash") || messageReceived.startsWith("byzantine"))) {
                    acknowledgeReceipt(receivePacket, rmSocket);
                }
                if (!isSequenceNumberOkayToExecute(messageReceived.split(" ")[0])) {
                    System.out.println("Sequence number bad. message seq num: "+ messageReceived.split(" ")[0] +";   rm seq num: " + lastExecutedSeqNumber);
                    continue;
                }

                String[] splitMessage = messageReceived.split(" : ");
                String metaData = splitMessage[0];
                switch (metaData) {
                    case "crash 2":
                        if (isCrashed()) {
                            System.out.println("Detected an actual crash");
                            handlingCrash.set(true);
                            List<String> dataList = getOtherRMsData();
                            startReplica("replica2");
                            setData(dataList);
                            reExecutePreviousMessageIfNeeded();
                            handlingCrash.set(false);
                        }
                        break;
                    case "crash 1":
                    case "crash 3":
                    case "crash 4":
                        break;
                    case "byzantine":
                        // do something: start a new replica with the Db from other RMs
                        byzantineCount ++;
                        if (byzantineCount < 3) {
                            continue;
                        }
                        handlingByzantine.set(true);
                        stopReplica();
                        List<String> dataList = getOtherRMsData();
                        startReplica("replica1");
                        setData(dataList);
                        byzantineCount = 0;
                        handlingByzantine.set(false);
                        break;
                    default:
                        String responseFromReplica = forwardToReplica(splitMessage[1]);
                        if ("error".equals(responseFromReplica)) {
                            messageWhoseExecStoppedInMiddle = messageReceived;
                        }
                        else{
                            forwardToFrontEnd(responseFromReplica, metaData);
                            lastExecutedSeqNumber.getAndIncrement();
                        }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isSequenceNumberOkayToExecute(String value) {
        if ("crash".equals(value) || "byzantine".equals(value)) {
            return true;
        }

        if (!"".equals(messageWhoseExecStoppedInMiddle)) {
            return false;
        }

        int seqNumber = Integer.parseInt(value);
        if (seqNumber == lastExecutedSeqNumber.get()+1) {
            return true;
        }

        return false;
    }

    private static void reExecutePreviousMessageIfNeeded() {
        String responseFromReplica = forwardToReplica(messageWhoseExecStoppedInMiddle.split(" : ")[1]);
        if ("error".equals(responseFromReplica)) {
            return;
        }

        forwardToFrontEnd(responseFromReplica, messageWhoseExecStoppedInMiddle.split(" : ")[0]);
        lastExecutedSeqNumber.getAndIncrement();
        messageWhoseExecStoppedInMiddle = "";
    }

    private static void setData(List<String> dataList) {
        String correctData;
        System.out.println("dataList:" + dataList);
        if (dataList.get(0).equalsIgnoreCase(dataList.get(1)) || dataList.get(0).equalsIgnoreCase(dataList.get(2))) {
            correctData = (dataList.get(0));
        } else if (dataList.get(1).equalsIgnoreCase(dataList.get(2))) {
            correctData = (dataList.get(1));
        } else {
            correctData = (dataList.get(0));
        }
        System.out.println("correctData:" + correctData);
        String[] correctDataForHospitals =  correctData.split("/",-1);

        int i = 0;
        for (String city : new String[] {"MTL", "QUE", "SHE"}) {
            ReplicaInterface replicaInterface = connectToCityServerObject(city);
            replicaInterface.setInfo(correctDataForHospitals[i]);
            i ++;
        }
    }

    private static List<String> getOtherRMsData() {
        List<String> result = new ArrayList<>(3);
        try {
            for (String ipAddress : RM_HOSTS) {
                DatagramSocket socket = new DatagramSocket();
                String data = sendDataRequestMessage(socket, ipAddress);
                result.add(data);
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static String sendDataRequestMessage(DatagramSocket socket, String ipAddress) {
        try {
            String toSend = "Need data";
            byte[] sendData = toSend.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ipAddress), LISTEN_PORT_FOR_DATA_REQUESTS);
            socket.send(datagramPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            socket.setSoTimeout(3000);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        String data;
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(dataPacket);
            data = new String(dataPacket.getData(), 0, dataPacket.getLength());
        } catch (IOException e) {
            data = "";
        }

        // retrying once more
        if (data.isEmpty()) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(dataPacket);
                data = new String(dataPacket.getData(), 0, dataPacket.getLength());
            } catch (IOException ignored) {
            }
        }

        return data;
    }

    private static void sendMessage(DatagramSocket sendSocket, InetAddress inetAddress, int port, String message) {
        try {
            byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
            sendSocket.send(datagramPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCrashed() {
        if (!process.isAlive()) {
            return true;
        }

        try {
            URL url = new URL("http://localhost:8080/mtlHospital?wsdl");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            int responseCode = huc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                handlingCrash.set(false);
                return false;
            }
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    private static void startNewThreadForOtherRMs() {
        try {
            DatagramSocket socket = getDatagramSocket(LISTEN_PORT_FOR_DATA_REQUESTS);
            System.out.println("Started Listening for data requests on port " + LISTEN_PORT_FOR_DATA_REQUESTS);

            while (true) {
                DatagramPacket receivePacket = getDatagramPacket(socket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());

                String replicaData = getReplicaData();
                sendMessage(socket, receivePacket.getAddress(), receivePacket.getPort(), replicaData);
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

    private static DatagramSocket getDatagramSocket(int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(port));
        return socket;
    }

    private static String getReplicaData() {
        StringBuilder hospitalsData = new StringBuilder();
        for (String city : new String[]{"MTL", "QUE", "SHE"}) {
            ReplicaInterface replicaInterface = connectToCityServerObject(city);
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
        System.out.println("Sending the request to replica: " + request);
        String[] splitRequestMessage = request.split(" ");
        String city = splitRequestMessage[0];
        String operation = splitRequestMessage[1];

        ReplicaInterface replicaInterface = connectToCityServerObject(city);
        if (replicaInterface == null) {
            return "error";
        }

        String result = callOperation(replicaInterface, operation, Arrays.copyOfRange(splitRequestMessage, 2, splitRequestMessage.length));
        System.out.println(result);
        return result;
    }

    private static String callOperation(ReplicaInterface replicaInterface, String operation, String [] parameters) {
        try {
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
        } catch (Exception e) {
            System.out.println("CALLOPERATION FAILED: ");
            for(String s: parameters){
                System.out.println(s);
            }
            return "error";

        }
        return "error";
    }

    private static ReplicaInterface connectToCityServerObject(String city){
        try {
            String url = "http://localhost:8080/" + city.toLowerCase() + "Hospital" + "?wsdl";
            URL urlLink = new URL(url);
            QName qname = new QName("http://" + runningReplica + "/",city.toUpperCase() + "HospitalService");
            QName qName2 = new QName("http://" + runningReplica + "/", city.toUpperCase() + "HospitalPort");
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
            String[] requestArgs = request.split(" ");
            InetAddress frontEndAddress = InetAddress.getByName(requestArgs[1]);
            int frontEndPort = Integer.parseInt(requestArgs[2]);
            String updatedMessage = "2 " + requestArgs[3] + " " + message;
            System.out.println(updatedMessage);
            DatagramPacket packet = new DatagramPacket(updatedMessage.getBytes(StandardCharsets.UTF_8), updatedMessage.getBytes(StandardCharsets.UTF_8).length, frontEndAddress, frontEndPort);
            new DatagramSocket().send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startReplica(String replica) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            switch (replica) {
                case "replica1":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica1-shanmukha.jar", "replica1.HospitalServer");
                    break;
                case "replica2":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica2-alain.jar", "replica2.HospitalServer");
                    break;
                case "replica3":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica3-daniel.jar", "replica3.HospitalServer");
                    break;
                case "replica4":
                    processBuilder = new ProcessBuilder("java", "-cp", "replica4-naveen.jar", "replica4.HospitalServer");
                    break;
            }
            processBuilder.directory(new File("C:\\Users\\shanm\\IdeaProjects\\DHMS_PROJECT\\src\\main\\resources"));
            process = processBuilder.start();
            runningReplica = replica;
            System.out.println("Started " + replica + "...");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopReplica() {
        System.out.println("Stopping replica...");
        process.destroyForcibly();
    }

    public static void shutDownGracefullyAtTheTimeOfTermination() {
        Runtime.getRuntime().addShutdownHook(new Thread(ReplicaManager2::stopReplica));
    }
}

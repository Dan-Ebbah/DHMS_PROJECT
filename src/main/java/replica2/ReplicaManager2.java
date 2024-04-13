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
import java.util.List;

public class ReplicaManager2 {
    private static final int RM_RECEIVE_PORT_FOR_SEQUENCER = 4444;
    public static final int LISTEN_PORT_FOR_DATA_REQUESTS = 3333;
    private static final int BUFFER_SIZE = 1024;
    private static final String[] RM_HOSTS = {"192.168.43.7", "192.168.43.251", "192.168.43.159"};

    private static Process process;
    private static boolean handlingCrash = false;
    private static boolean handlingByzantine = false;
    private static int byzantineCount = 0;
    private static int lastExecutedSeqNumber = 0;
    private static String runningReplica = "";


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
                if (handlingCrash || handlingByzantine) {
                    continue;
                }
                if(!isSequenceNumberOkayToExecute(messageReceived.split(" ")[0])) {
                    continue;
                }
                acknowledgeReceipt(receivePacket, rmSocket);
                String[] splitMessage = messageReceived.split(" : ");
                String metaData = splitMessage[0];
                switch (metaData) {
                    case "crash":
                        if (isCrashed()) {
                            handlingCrash = true;
                            List<String> dataList = getOtherRMsData();
                            startReplica("replica2");
                            setData(dataList);
                            handlingCrash = false;
                        }
                    case "byzantine":
                        // do something: start a new replica with the Db from other RMs
                        byzantineCount ++;
                        if (byzantineCount < 3) {
                            continue;
                        }
                        handlingByzantine = true;
                        stopReplica();
                        List<String> dataList = getOtherRMsData();
                        startReplica("replica1");
                        setData(dataList);
                        byzantineCount = 0;
                        handlingByzantine = false;
                    default:
                        lastExecutedSeqNumber ++;
                        String responseFromReplica = forwardToReplica(splitMessage[1]);
                        forwardToFrontEnd(responseFromReplica, metaData);
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

        int seqNumber = Integer.parseInt(value);
        if (seqNumber == lastExecutedSeqNumber + 1) {
            return true;
        }

        return false;
    }

    private static void setData(List<String> dataList) {
        String correctData;
        if (dataList.get(0).equalsIgnoreCase(dataList.get(1)) || dataList.get(0).equalsIgnoreCase(dataList.get(2))) {
            correctData = (dataList.get(0));
        } else if (dataList.get(1).equalsIgnoreCase(dataList.get(2))) {
            correctData = (dataList.get(1));
        } else {
            correctData = (dataList.get(0));
        }

        String[] correctDataForHospitals =  correctData.split("/");

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
                handlingCrash = false;
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
            System.out.println("ERROR as I could not connect to server object of " + city);
        }

        String result = callOperation(replicaInterface, operation, Arrays.copyOfRange(splitRequestMessage, 2, splitRequestMessage.length));
        System.out.println(result);
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
            processBuilder.directory(new File("C:\\Users\\potat\\Desktop\\assignments\\comp6231\\project\\DHMS_PROJECT\\src\\main\\resources\\"));
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
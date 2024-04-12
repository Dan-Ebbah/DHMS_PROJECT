package replicaManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ReplicaManager {
    private static final String sequencer_IP = "192.168.2.17";
    private static final int sequencer_Port = 2222;
    private static final int BUFFER_SIZE = 1024;
    //should start the servers too

    public static void main(String[] args) {
        //should start and be ready to receive messages
        try {
            DatagramSocket rmSocket = new DatagramSocket(null);
            rmSocket.bind(new InetSocketAddress(sequencer_IP, sequencer_Port));
            System.out.println("Replica Manager started...");

            while (true) {
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                rmSocket.receive(receivePacket);
                String messageReceived = new String(receivePacket.getData(), 0, receivePacket.getLength());
                acknowledgeReceipt(receivePacket, rmSocket);
                String[] splitMessage = messageReceived.split(":");

                forwardToReplica(splitMessage[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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


}

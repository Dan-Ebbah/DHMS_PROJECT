package sequencer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Sequencer {
    private static int sequencer_ID = 0;
    private static final String sequencer_IP = "192.168.2.17";
    private static final int Port = 2222;
    private static final String multicast_IP = "230.1.1.10";
    private static final int Buffer_SIZE = 1000;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.bind(new InetSocketAddress(InetAddress.getByName(sequencer_IP), Port));
            System.out.println("Sequencer------>Started");

            while (true) {
                byte[] buffer = new byte[Buffer_SIZE];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                new Thread(() -> processRequest(socket, request)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processRequest(DatagramSocket socket, DatagramPacket request) {
        try {
            String sentence = new String(request.getData(), 0, request.getLength());
            String[] parts = sentence.split(";");

            int sequencerId1 = Integer.parseInt(parts[0]);
            String ip = request.getAddress().getHostAddress();
            String message = constructMessage(ip, parts);

            sendMessage(socket,sequencerId1, message, parts[2].equalsIgnoreCase("00"));

            byte[] seqId = Integer.toString(sequencer_ID).getBytes();
            DatagramPacket response = new DatagramPacket(seqId, seqId.length, request.getAddress(), request.getPort());
            socket.send(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String constructMessage(String ip, String[] parts) {
        StringBuilder sb = new StringBuilder(ip);
        for (int i = 2; i < parts.length; i++) {
            sb.append(";").append(parts[i]);
        }
        return sb.toString();
    }

    private static void sendMessage(DatagramSocket socket, int sequencerId1,String message, boolean isRequest) {
        try {
            if (sequencerId1 == 0 && isRequest) {
                sequencerId1 = ++sequencer_ID;
            }
            String finalMessage = sequencerId1 + ";" + message;
            byte[] data = finalMessage.getBytes();
            InetAddress multicastAddress = InetAddress.getByName(multicast_IP);
            DatagramPacket packet = new DatagramPacket(data, data.length, multicastAddress, Port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

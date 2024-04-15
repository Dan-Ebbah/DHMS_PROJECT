package sequencer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Sequencer {
    private static final AtomicInteger SEQUENCER_ID = new AtomicInteger(0);
    private static final String SEQUENCER_IP = "192.168.2.11";
    private static final int SEQUENCER_RECEIVE_PORT = 2222;
    private static final int[] RM_PORTS = {4444, 4444, 4444, 4444};
    private static final String[] RM_HOSTS = {"192.168.43.7", "192.168.43.254", "192.168.43.159", "192.168.43.251"};
    private static final int BUFFER_SIZE = 1024;
    private static final int NUM_REPLICA_MANAGERS = RM_HOSTS.length;

    public static void main(String[] args) {
        try (DatagramSocket sequencerSocket = new DatagramSocket(null)) {
            sequencerSocket.bind(new InetSocketAddress(SEQUENCER_IP, SEQUENCER_RECEIVE_PORT));
            System.out.println("Sequencer started...");

            while (true) {
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                sequencerSocket.receive(packet);
                new Thread(() -> processRequest(packet)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processRequest(DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received -> " + request);
            String message = SEQUENCER_ID.incrementAndGet() + " " + request;
            System.out.println("Sending this message -> " + message);

            List<DatagramSocket> sockets = Arrays.asList(new DatagramSocket(),
                    new DatagramSocket(), new DatagramSocket(), new DatagramSocket());

            // Send request to each replica manager using unicast
            for (int i = 1; i <= NUM_REPLICA_MANAGERS; i++) {
                int finalI = i;
                new Thread(() -> {
                    try {
                        sendMessage(new DatagramSocket(), message, finalI);
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(DatagramSocket socket, String message, int rmNumber) {
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(RM_HOSTS[rmNumber - 1]), RM_PORTS[rmNumber - 1]);
            socket.send(packet);

            boolean receivedAck;
            receivedAck = receiveAck(socket);

            if (!receivedAck) {
                System.out.println("No ACK yet, so retrying...");
                sendMessage(socket, message, rmNumber);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean receiveAck(DatagramSocket socket) {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
        try {
            socket.setSoTimeout(3000);
            socket.receive(ackPacket);
            String messageReceived = new String(ackPacket.getData(), 0, ackPacket.getLength());
            System.out.println("Acknowledgement Received => " + messageReceived + " from ip = " + ackPacket.getAddress().getHostAddress());
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
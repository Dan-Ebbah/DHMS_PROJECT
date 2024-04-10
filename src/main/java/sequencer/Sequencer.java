package sequencer;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Sequencer {
    private static final AtomicInteger SEQUENCER_ID = new AtomicInteger(0);
    private static final String SEQUENCER_IP = "192.168.2.17";
    private static final int SEQUENCER_RECEIVE_PORT = 2222;
    private static final int[] RM_PORTS = {5000, 5001, 5002, 5003}; // Ports of the replica managers
    private static final String[] RM_HOSTS = {"", "", "", ""};
    private static final int BUFFER_SIZE = 1024;
    private static final long ACK_TIMEOUT = 2000; // Timeout for acknowledgment in milliseconds
    private static final int NUM_REPLICA_MANAGERS = 4;

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
            String message = SEQUENCER_ID.incrementAndGet() + " " + request;

            List<DatagramSocket> sockets = Arrays.asList(new DatagramSocket(),
                    new DatagramSocket(), new DatagramSocket(), new DatagramSocket());

            // Send request to each replica manager using unicast
            for (int i = 1; i <= NUM_REPLICA_MANAGERS; i++) {
                sendMessage(sockets.get(i - 1), message, i);
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(DatagramSocket socket, String message, int rmNumber) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName(RM_HOSTS[rmNumber]), RM_PORTS[rmNumber]);
            socket.send(packet);

            boolean receivedAck = false;
            new Thread(() -> receiveAck(socket)).start();
            sleep(ACK_TIMEOUT);

            if (!receivedAck) {
                sendMessage(socket, message, rmNumber);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void receiveAck(DatagramSocket socket) {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(ackPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

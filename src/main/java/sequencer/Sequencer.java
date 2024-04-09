package sequencer;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Sequencer {
    private static final AtomicInteger sequencer_ID = new AtomicInteger(0);
    private static final String sequencer_IP = "192.168.2.17";
    private static final int sequencer_Port = 2222;
    private static final int[] replicaPorts = {5000, 5001, 5002, 5003}; // Ports of the replica managers
    private static final int BUFFER_SIZE = 1024;
    private static final long ACK_TIMEOUT = 2000; // Timeout for acknowledgment in milliseconds
    private static final int NUM_REPLICA_MANAGERS = 4;

    private static final Map<Integer, Map<Integer, Boolean>> ackMap = new ConcurrentHashMap<>(); // Map to track acknowledgments for each replica manager

    public static void main(String[] args) {
        try {
            DatagramSocket sequencerSocket = new DatagramSocket(null);
            sequencerSocket.bind(new InetSocketAddress(sequencer_IP, sequencer_Port));
            System.out.println("Sequencer started...");

            while (true) {
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                sequencerSocket.receive(receivePacket);
                new Thread(() -> processRequest(sequencerSocket, receivePacket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processRequest(DatagramSocket socket, DatagramPacket request) {
        try {
            String sentence = new String(request.getData(), 0, request.getLength());
            String[] parts = sentence.split(" ");

            int sequencerId = Integer.parseInt(parts[0]);
            String message = constructMessage(parts);

            if (sequencerId == 0) {
                sequencerId = sequencer_ID.incrementAndGet();
            }

            // Initialize acknowledgment map for this sequencer ID
            ackMap.put(sequencerId, new ConcurrentHashMap<>());

            // Send request to each replica manager using unicast
            for (int i = 0; i < NUM_REPLICA_MANAGERS; i++) {
                sendMessage(socket, sequencerId, message, replicaPorts[i]);
            }

            // Wait for acknowledgment from each replica manager
            waitForAcknowledgment(sequencerId);

            // If acknowledgment not received from any replica manager, resend the request
            while (!allReplicaAcknowledged(sequencerId)) {
                for (int i = 0; i < NUM_REPLICA_MANAGERS; i++) {
                    if (!ackMap.get(sequencerId).get(i)) {
                        sendMessage(socket, sequencerId, message, replicaPorts[i]);
                    }
                }
                waitForAcknowledgment(sequencerId);
            }

            // Send acknowledgment to the client
            byte[] seqId = Integer.toString(sequencerId).getBytes();
            DatagramPacket response = new DatagramPacket(seqId, seqId.length, request.getAddress(), request.getPort());
            socket.send(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void waitForAcknowledgment(int sequencerId) {
        long startTime = System.currentTimeMillis();
        while (!allReplicaAcknowledged(sequencerId) && (System.currentTimeMillis() - startTime) < ACK_TIMEOUT) {
            // Wait for acknowledgment or timeout
        }
    }

    private static boolean allReplicaAcknowledged(int sequencerId) {
        if (!ackMap.containsKey(sequencerId)) {
            return false;
        }
        for (boolean acknowledged : ackMap.get(sequencerId).values()) {
            if (!acknowledged) {
                return false;
            }
        }
        return true;
    }

    private static String constructMessage(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            sb.append(parts[i]).append(" ");
        }
        return sb.toString().trim();
    }

    private static void sendMessage(DatagramSocket socket, int sequencerId, String message, int replicaManagerPort) {
        try {
            String finalMessage = sequencerId + " " + message;
            byte[] data = finalMessage.getBytes();
            InetAddress replicaAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, replicaAddress, replicaManagerPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method should be called by Replica Managers to acknowledge receiving the request
    public static void acknowledge(int sequencerId, int replicaManagerIndex) {
        if (ackMap.containsKey(sequencerId)) {
            ackMap.get(sequencerId).put(replicaManagerIndex, true);
        }
    }
}

package frontend;

import static frontend.ValidationHelper.isValidAdminID;
import static frontend.ValidationHelper.isValidAppointmentID;
import static frontend.ValidationHelper.isValidAppointmentType;
import static frontend.ValidationHelper.isValidUserID;
import static java.lang.Thread.sleep;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@WebService(endpointInterface="frontend.FrontEndInterface")
@SOAPBinding(style= SOAPBinding.Style.RPC)
public class FrontEndImpl implements FrontEndInterface {

    private static final int FE_RECEIVE_PORT_FOR_RM = 19000;
    private static final int FE_SEND_PORT_FOR_SEQUENCER = 19001;
    private static final int FE_SEND_PORT_FOR_RM = 19002;
    private static final String SEQUENCER_IP = "192.168.43.159";
    private static final int SEQUENCER_PORT = 2222;
    private static final String[] RM_HOSTS = new String[] {"192.168.43.7", "192.168.43.254", "192.168.43.159", "192.168.43.244"};
    private static final int[] RM_PORTS = new int[] {4444,4444,4444,4444};
    private static final String FAILURE = "FAILURE";

    private final DatagramSocket socketToSendToSequencer;
    private final DatagramSocket socketToReceiveFromRMs;
    private final DatagramSocket socketToSendToRMs;

    private int requestId = 0;
    private String[] responseFromRMs;
    private String udpRequest;
    private AtomicLong requestToSequencerTimeStamp;
    private AtomicLong timeTakenForFastestResponse;
    private AtomicBoolean waitingForFirstResponse;
    private boolean notifiedOfSoftwareFailure;

    public FrontEndImpl() {
        try {
            responseFromRMs = new String[] {"-", "-", "-", "-"};
            udpRequest = "";
            requestToSequencerTimeStamp = new AtomicLong(0);
            timeTakenForFastestResponse = new AtomicLong(-1);
            waitingForFirstResponse = new AtomicBoolean(false);
            notifiedOfSoftwareFailure = false;
            socketToSendToSequencer = new DatagramSocket(FE_SEND_PORT_FOR_SEQUENCER);
            socketToReceiveFromRMs = new DatagramSocket(FE_RECEIVE_PORT_FOR_RM);
            socketToSendToRMs = new DatagramSocket(FE_SEND_PORT_FOR_RM);
            new Thread(this::receiveResponseFromRMs).start();
            new Thread(this::notifyRMsInCaseOfCrash).start();
            new Thread(this::retryIfNoResponse).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String bookAppointment(String patientID, String appointmentType, String appointmentID) {
        if (!isValidUserID(patientID)
                || !isValidAppointmentID(appointmentID)
                || !isValidAppointmentType(appointmentType)) {
            return FAILURE;
        }

        String city = patientID.substring(0, 3);
        String operationName = "bookAppointment";
        udpRequest = createUdpRequest(city, operationName, patientID, appointmentType, appointmentID);
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        if (!isValidUserID(patientID)) {
            return FAILURE;
        }

        String city = patientID.substring(0, 3);
        String operationName = "getAppointmentSchedule";
        udpRequest = createUdpRequest(city, operationName, patientID, patientID);
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        if (!isValidUserID(patientID) || !isValidAppointmentID(appointmentID)) {
            return FAILURE;
        }

        String city = patientID.substring(0, 3);
        String operationName = "cancelAppointment";
        udpRequest = createUdpRequest(city, operationName, patientID, appointmentID);
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String addAppointment(String adminID, String appointmentID, String appointmentType, int capacity) {
        if (!isValidAdminID(adminID)
                || !isValidAppointmentID(appointmentID, adminID)
                || !isValidAppointmentType(appointmentType)) {
            return FAILURE;
        }

        System.out.println("received add request");
        String city = adminID.substring(0, 3);
        String operationName = "addAppointment";
        udpRequest = createUdpRequest(city, operationName, appointmentID, appointmentType, String.valueOf(capacity));
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String removeAppointment(String adminID, String appointmentID, String appointmentType) {
        if (!isValidAdminID(adminID)
                || !isValidAppointmentID(appointmentID, adminID)
                || !isValidAppointmentType(appointmentType)) {
            return FAILURE;
        }

        String city = adminID.substring(0, 3);
        String operationName = "removeAppointment";
        udpRequest = createUdpRequest(city, operationName, appointmentID, appointmentType);
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String listAppointmentAvailability(String adminID, String appointmentType) {
        if (!isValidAdminID(adminID) || !isValidAppointmentType(appointmentType)) {
            return FAILURE;
        }

        String city = adminID.substring(0, 3);
        String operationName = "listAppointmentAvailability";
        udpRequest = createUdpRequest(city, operationName, appointmentType);
        callSequencer();
        return getResponseToReturnToClient();
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        if (!isValidUserID(patientID)
                || !isValidAppointmentID(oldAppointmentID)
                || !isValidAppointmentType(oldAppointmentType)
                || !isValidAppointmentID(newAppointmentID)
                || !isValidAppointmentType(newAppointmentType)) {
            return FAILURE;
        }

        String city = patientID.substring(0, 3);
        String operationName = "swapAppointment";
        udpRequest = createUdpRequest(city, operationName, patientID,
                oldAppointmentID, oldAppointmentType, newAppointmentID, newAppointmentType);
        callSequencer();
        return getResponseToReturnToClient();
    }

    private String getResponseToReturnToClient() {
        long startTime = System.currentTimeMillis();
        while (true) {
            if (hasReceivedEnoughValidResponses()) {
                String validResponse = getValidResponse();
                System.out.println("Returning to client: " + validResponse);
                return validResponse;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime > 7000) {
                return tryReturnAnyNonEmptyResponse();
            }
        }
    }

    private boolean hasReceivedEnoughValidResponses() {
        return !"-".equals(getValidResponse());
    }

    private String getValidResponse() {
        for (int i = 0; i < responseFromRMs.length; i ++) {
            for (int j = i + 1; j < responseFromRMs.length; j ++) {
                if (!"-".equals(responseFromRMs[j]) && responseFromRMs[j].equals(responseFromRMs[i])) {
                    return responseFromRMs[i];
                }
            }
        }
        return "-";
    }

    private String tryReturnAnyNonEmptyResponse() {
        for (int i = 0; i < responseFromRMs.length; i ++) {
            if (!"-".equals(responseFromRMs[i])) {
                return responseFromRMs[i];
            }
        }

        return "No valid response to send";
    }

    private String createUdpRequest(String... requestElements) {
        StringBuilder udpRequestStringBuilder = new StringBuilder();
        try {
            udpRequestStringBuilder.append(InetAddress.getLocalHost().getHostAddress());
            udpRequestStringBuilder.append(" " + FE_RECEIVE_PORT_FOR_RM + " ");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        requestId ++;
        udpRequestStringBuilder.append(requestId);
        udpRequestStringBuilder.append(" :");
        for (String requestElement: requestElements) {
            udpRequestStringBuilder.append(" ").append(requestElement);
        }
        return udpRequestStringBuilder.toString();
    }

    private void callSequencer() {
        try {
            DatagramPacket datagram = new DatagramPacket(udpRequest.getBytes(StandardCharsets.UTF_8),
                    udpRequest.getBytes(StandardCharsets.UTF_8).length,
                    InetAddress.getByName(SEQUENCER_IP),
                    SEQUENCER_PORT);
            resetState();
            socketToSendToSequencer.send(datagram);
            System.out.println("Request '" + udpRequest + "' sent to sequencer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void retryIfNoResponse() {
        while (true) {
            if (waitingForFirstResponse.get() && System.currentTimeMillis() - requestToSequencerTimeStamp.get() > 5000) {
                callSequencer();
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void receiveResponseFromRMs() {
        byte[] buffer = new byte[1000];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        while (true) {
            try {
                socketToReceiveFromRMs.receive(response);
                String responseDataAsString = new String(response.getData()).substring(0, response.getLength());
                System.out.println("responseDataAsString: " + responseDataAsString);
                String[] responseAsArray = responseDataAsString.split(" ", -1);
                int rmNumber = Integer.parseInt(responseAsArray[0]);
                if (String.valueOf(requestId).equals(responseAsArray[1])) {
                    if (waitingForFirstResponse.get()) {
                        timeTakenForFastestResponse.set(System.currentTimeMillis() - requestToSequencerTimeStamp.get());
                        waitingForFirstResponse.set(false);
                    }
                    responseFromRMs[rmNumber - 1] = responseAsArray[2];
                    if (!notifiedOfSoftwareFailure) {
                        notifyRMIfSoftwareFailure();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyRMsInCaseOfCrash() {
        long timeStampOfLastCheckedRequest = -1;
        while (true) {
            if (timeTakenForFastestResponse.get() != -1
                    && timeStampOfLastCheckedRequest != requestToSequencerTimeStamp.get()
                    && System.currentTimeMillis() - requestToSequencerTimeStamp.get() >= 2000 + timeTakenForFastestResponse.get()) {
                for (int i = 0; i < RM_HOSTS.length; i++) {
                    if ("-".equals(responseFromRMs[i])) {
                        System.out.println("inform about suspected crash at " + RM_HOSTS[i]);
                        for (int j = 0; j < 4; j++) {
                            try {
                                String crashMessage = "crash " + (i + 1);
                                DatagramPacket datagram = new DatagramPacket(
                                        crashMessage.getBytes(StandardCharsets.UTF_8),
                                        crashMessage.getBytes(StandardCharsets.UTF_8).length,
                                        InetAddress.getByName(RM_HOSTS[j]),
                                        RM_PORTS[j]);
                                socketToSendToRMs.send(datagram);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                timeStampOfLastCheckedRequest = requestToSequencerTimeStamp.get();
            }
        }
    }

    public void notifyRMIfSoftwareFailure() {
        String majorityResponse = "-";
        for (int i = 0; i < RM_HOSTS.length; i ++) {
            for (int j = i + 1; j < 4; j ++) {
                if (responseFromRMs[i].equals(responseFromRMs[j]) && !"-".equals(responseFromRMs[i])) {
                    majorityResponse = responseFromRMs[i];
                }
            }
        }

        if ("-".equals(majorityResponse)) {
            return;
        }

        for (int i = 0; i < RM_HOSTS.length; i ++) {
            if (!"-".equals(responseFromRMs[i]) && !majorityResponse.equals(responseFromRMs[i])) {
                System.out.println("inform about software failure at " + RM_HOSTS[i]);
                try {
                    String faultMessage = "byzantine";
                    DatagramPacket datagram = new DatagramPacket(
                            faultMessage.getBytes(StandardCharsets.UTF_8),
                            faultMessage.getBytes(StandardCharsets.UTF_8).length,
                            InetAddress.getByName(RM_HOSTS[i]),
                            RM_PORTS[i]);
                    socketToSendToRMs.send(datagram);
                    notifiedOfSoftwareFailure = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void resetState() {
        responseFromRMs = new String[] {"-", "-", "-", "-"};
        timeTakenForFastestResponse.set(-1);
        requestToSequencerTimeStamp.set(System.currentTimeMillis());
        waitingForFirstResponse.set(true);
        notifiedOfSoftwareFailure = false;
    }
}


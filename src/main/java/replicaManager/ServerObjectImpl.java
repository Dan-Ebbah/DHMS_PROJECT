package replicaManager;

public class ServerObjectImpl implements ReplicaInterface {
    private String _serverName;
    public ServerObjectImpl(String serverName) {
        _serverName = serverName;
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {
        return null;
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {
        return null;
    }

    @Override
    public String bookAppointment(String patientID, String appointmentType, String appointmentID) {
        return null;
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID) {
        return null;
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        return null;
    }

    @Override
    public String listAppointmentAvailability(String appointmentType) {
        return null;
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentType, String oldAppointmentID, String newAppointmentType, String newAppointmentID) {
        return null;
    }

    public String getServerName() {
        return _serverName;
    }
}
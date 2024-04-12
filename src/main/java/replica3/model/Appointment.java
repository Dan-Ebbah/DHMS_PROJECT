package replica3.model;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Appointment {


    private String appointmentID;

    private AppointmentType appointmentType;

    private String patientID;
    private List<String> patientIDs;

    private int capacity;

    public Appointment(String appointmentID, AppointmentType appointmentType, int capacity) {
        this.appointmentID = appointmentID;
        this.appointmentType = appointmentType;
        this.capacity = capacity;
    }

    public Appointment(String appointmentID, AppointmentType appointmentType) {
        this.appointmentID = appointmentID;
        this.appointmentType = appointmentType;
        capacity = 0;
    }

    public HospitalType getAppointmentHospital() {
        return HospitalType.findHospital(appointmentID.substring(0,3));
    }


    public String getAppointmentID() {
        return appointmentID;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public void incrementCapacity() {
        capacity++;
    }

    public void decrementCapacity() {
        capacity--;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getPatientID() {
        return patientID;
    }

    public List<String> getPatientIDs() {
        return patientIDs;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }
    public void addPatientID(String patientID) {
        patientIDs.add(patientID);
    }

    public void removePatientID(String patientID) {
        patientIDs.remove(patientID);
    }

    public LocalDate getDayOfAppointment() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy");
        return LocalDate.parse(appointmentID.substring(4), dateTimeFormatter);
    }

    @Override
    public String toString() {
        return String.format("Appointment(#%s)", getAppointmentID());
    }

    public void book(String patientID) {
        capacity --;
        addPatientID(patientID);
    }

    public void cancel(String patientID) {
        capacity ++;
        removePatientID(patientID);
    }
}

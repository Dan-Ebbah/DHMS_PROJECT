package replica3.database;





import replica3.model.Appointment;
import replica3.model.AppointmentType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HashMapImpl {
    public static final String APPOINTMENT_DOES_NOT_EXIST = "Appointment does not seem to exist. Check Appointment ID again";
    public static final String APPOINTMENT_HAS_BEEN_SUCCESSFULLY_TEMPLATE = "Appointment has been successfully ";


    private ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> appointments;

    public HashMapImpl(ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> appointments) {
        this.appointments = appointments;
    }

    public HashMapImpl() {
        initializeDB();
    }

    private void initializeDB() {
        appointments = new ConcurrentHashMap<>();
        appointments.put(AppointmentType.Physician, new java.util.HashMap<>());
        appointments.put(AppointmentType.Surgeon, new java.util.HashMap<>());
    }

    public void insert(Appointment appointment) {
        if (appointments.containsKey(appointment.getAppointmentType())) {
            java.util.HashMap<String, Appointment> values = getValueByAppointmentType(appointment.getAppointmentType());
            values.put(appointment.getAppointmentID(), appointment);
        } else {
            java.util.HashMap<String, Appointment> subHashMap = new java.util.HashMap<>();
            subHashMap.put(appointment.getAppointmentID(), appointment);
            appointments.put(appointment.getAppointmentType(), subHashMap);
        }
    }

    public java.util.HashMap<String, Appointment> getByAppointmentType(AppointmentType appointmentType) {
        return appointments.get(appointmentType);
    }

    public String printDatabase() {
        StringBuilder result = new StringBuilder();
        for (AppointmentType appointmentType : appointments.keySet()) {
            result.append(String.format("\n%s: ", appointmentType.toString().toUpperCase()));
            java.util.HashMap<String, Appointment> subHashMap = appointments.get(appointmentType);
            for (Appointment appointment : subHashMap.values()) {
                result.append("\n\t\t").append(appointment.toString());
            }
        }
        return result.toString();
    }


    public String remove(Appointment appointment) {
        java.util.HashMap<String, Appointment> stringAppointmentHashMap = getValueByAppointmentType(appointment.getAppointmentType());
        stringAppointmentHashMap.remove(appointment.getAppointmentID());
        return APPOINTMENT_HAS_BEEN_SUCCESSFULLY_TEMPLATE.concat("Removed");
    }

    private java.util.HashMap<String, Appointment> getValueByAppointmentType(AppointmentType appointmentType) {
        return appointments.get(appointmentType);
    }

    private Appointment getAppointment(Appointment appointment) {
        java.util.HashMap<String, Appointment> subHash = getValueByAppointmentType(appointment.getAppointmentType());
        return subHash  == null ? null : subHash.get(appointment.getAppointmentID());
    }


    public String book(String patientID, Appointment appointment) {
        Appointment appointment1 = findByAppointmentID(appointment.getAppointmentID());

        if (appointment1 == null) {
            return APPOINTMENT_DOES_NOT_EXIST;
        }

        if (appointment1.getCapacity() <= 0) {
            return "No more slot for booking";
        }
        appointment1.book(patientID);
        updateAppointment(appointment1);

        return APPOINTMENT_HAS_BEEN_SUCCESSFULLY_TEMPLATE.concat("Added");
    }

    protected void setAppointments(ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> appointments) {
        this.appointments = appointments;
    }


    private void updateAppointment(Appointment appointment) {
        java.util.HashMap<String, Appointment> stringAppointmentHashMap = getValueByAppointmentType(appointment.getAppointmentType());

        stringAppointmentHashMap.replace(appointment.getAppointmentID(), appointment);
    }

    public Appointment findByAppointmentID(String appointmentID) {
        Optional<Appointment> optionalAppointment = appointments.values().stream()
                .map(x -> x.get(appointmentID))
                .filter(Objects::nonNull)
                .findFirst();

        return optionalAppointment.orElse(null);
    }


    public List<Appointment> getByPatientId(String patientID) {
        return appointments.values().stream()
                .flatMap(x -> x.values().stream())
                .filter(x -> x.getPatientIDs().contains(patientID))
                .collect(Collectors.toList());
    }

    public String cancel(String patientID, String appointmentID) {
        Appointment appointment = findByAppointmentID(appointmentID);

        if (appointment == null) {
            return APPOINTMENT_DOES_NOT_EXIST;
        }

        if (appointment.getPatientID() == null || !appointment.getPatientID().equalsIgnoreCase(patientID)) {
            return "You do not have access to cancel this appointment.";
        }

        appointment.cancel(patientID);
        updateAppointment(appointment);

        return APPOINTMENT_HAS_BEEN_SUCCESSFULLY_TEMPLATE.concat("cancelled");
    }

    public String getAvailability(AppointmentType appointmentType) {
        StringBuilder stringBuilder = new StringBuilder();
        java.util.HashMap<String, Appointment> appointmentLists = appointments.get(appointmentType);
        if(appointmentLists == null) {
            return stringBuilder.toString();
        }
        Stream<Appointment> stream = appointmentLists.values().stream();
        stream.forEach(x -> stringBuilder
                .append(x.getAppointmentID())
                .append(":")
                .append(x.getCapacity())
                .append(", "));
        return stringBuilder.toString();
    }
}

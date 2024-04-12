package replica3.database;


import replica3.model.Appointment;
import replica3.model.AppointmentType;

import java.util.concurrent.ConcurrentHashMap;

public class QuebecHashMap extends HashMapImpl {
    public QuebecHashMap(ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> appointments) {
        super(appointments);
    }

    public QuebecHashMap() {
        initializeDB();
    }

    public void initializeDB() {
        ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> hashMap = new ConcurrentHashMap<>();
        java.util.HashMap<String, Appointment> value = new java.util.HashMap<>();
        value.put("QUEA101124", new Appointment("QUEA101124", AppointmentType.Dental, 3));
        hashMap.put(AppointmentType.Dental, value);

        super.setAppointments(hashMap);
    }
}

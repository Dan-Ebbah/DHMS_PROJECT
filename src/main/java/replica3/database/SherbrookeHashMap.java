package replica3.database;

import replica3.model.Appointment;
import replica3.model.AppointmentType;

import java.util.concurrent.ConcurrentHashMap;

public class SherbrookeHashMap extends HashMapImpl {

    public SherbrookeHashMap() {
        initializeDB();
    }

    public void initializeDB() {
        ConcurrentHashMap<AppointmentType, java.util.HashMap<String, Appointment>> hashMap = new ConcurrentHashMap<>();
        super.setAppointments(hashMap);
    }
}

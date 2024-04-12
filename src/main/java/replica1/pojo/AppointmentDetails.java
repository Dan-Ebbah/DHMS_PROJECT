package replica1.pojo;

import java.io.Serializable;
import java.util.List;

public class AppointmentDetails implements Serializable {

    private List<String> patientIDList;
    int capacity;

    public AppointmentDetails() {}

    public AppointmentDetails(List<String> patientIDList, int capacity) {
        this.patientIDList = patientIDList;
        this.capacity = capacity;
    }

    public List<String> getPatientIDList() {
        return patientIDList;
    }

    public int getCapacity() {
        return capacity;
    }

    public void addPatientID(String patientID) {
        patientIDList.add(patientID);
        capacity --;
    }

    public void removePatientID(String patientID) {
        patientIDList.remove(patientID);
        capacity ++;
    }
}

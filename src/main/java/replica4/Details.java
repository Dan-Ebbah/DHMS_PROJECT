package replica4;
import java.io.Serializable;
import java.util.List;

public class Details implements Serializable {

    private List<String> patient_ID_List;
    int capacity;


    public Details(List <String> patient_ID_List, int capacity)
    {
        this.patient_ID_List = patient_ID_List;
        this.capacity =capacity;

    }
    public int get_capacity(){return capacity;}
    public void addPatientToTheList(String patientID)
    {
        patient_ID_List.add(patientID);
        capacity--;
    }
    public void removeID(String patientID)
    {
        patient_ID_List.remove(patientID);
        capacity++;
    }

    public List<String> getIDList()
    {return patient_ID_List;}


}

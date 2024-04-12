package replica2;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import java.util.ArrayList;


@WebService
@SOAPBinding(style= Style.RPC)
public interface AppointmentManager extends ReplicaInterface{
    @WebMethod
    boolean bookAppointment1(String patientID, String type, String appointmentID);
    @WebMethod
    byte[] getAppointmentSchedule1(String patientID);
    @WebMethod
    boolean cancelAppointment1(String patientID, String appointmentID);
    @WebMethod
    boolean addAppointment1(String patientID, String appointmentID, String appointmentType, int capacity);
    @WebMethod
    boolean removeAppointment1(String patientID, String appointmentID, String appointmentType);
    @WebMethod
    byte[] listAppointmentAvailability1(String patientID, String appointmentType);
    @WebMethod
    boolean swapAppointment1(String clientID, String oldID, String oldType, String newID, String newType);
}

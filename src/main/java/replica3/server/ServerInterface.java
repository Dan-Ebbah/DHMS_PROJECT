package replica3.server;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface ServerInterface {
    @WebMethod
    String addAppointment(String appointmentID, String appointmentType, int capacity);
    @WebMethod
    String removeAppointment( String appointmentID,  String appointmentType);
    @WebMethod
    String bookAppointment( String patientID,  String appointmentType,  String appointmentID);
    @WebMethod
    String cancelAppointment( String patientID,  String appointmentID);
    @WebMethod
    String getAppointmentSchedule( String patientID);
    @WebMethod
    String listAppointmentAvailability( String appointmentType);
    @WebMethod
    String swapAppointment( String patientID,  String oldAppointmentType,  String oldAppointmentID,  String newAppointmentType,  String newAppointmentID);
    @WebMethod
    public String getInfo();
    @WebMethod
    public void setInfo(String info);
}

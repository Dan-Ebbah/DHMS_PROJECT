package replica1;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style= SOAPBinding.Style.RPC)
public interface Hospital extends ReplicaInterface {

    // the below methods are only used for inter-server communication to assist bookAppointment and getAppointmentSchedule calls among servers
    @WebMethod
    int countOfAppointmentsInSameWeek (String appointmentID, String patientID);
    @WebMethod
    boolean appointmentWithSameTypeOnSameDayExists (String appointmentType, String appointmentID, String patientID);
    @WebMethod
    String getAppointmentsOfPatient (String patientID);
}

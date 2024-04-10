package com.webservice.hi;

import javax.jws.WebMethod;

public class clientToFE {
    FrontEndInterface FE;
    clientToFE(FrontEndInterface fe){
        FE = fe;
    }
    String bookAppointment(String patientID, String type, String appointmentID){
        return FE.bookAppointment(patientID, appointmentID,type);
    }
    String getAppointmentSchedule(String patientID){
        return FE.getAppointmentSchedule(patientID);
    }
    String cancelAppointment(String patientID, String appointmentID){
        return FE.cancelAppointment(patientID,appointmentID);
    }
    String addAppointment(String patientID, String appointmentID, String appointmentType, int capacity){
        return FE.addAppointment(patientID,appointmentID,appointmentType,capacity);
    }
    String removeAppointment(String patientID, String appointmentID, String appointmentType){
        return FE.removeAppointment(patientID,appointmentID,appointmentType);
    }
    String listAppointmentAvailability(String patientID, String appointmentType){
        return FE.listAppointmentAvailability(patientID,appointmentType);
    }
    String swapAppointment(String clientID, String oldID, String oldType, String newID, String newType){
        return FE.swapAppointment(clientID,oldID,oldType,newID,newType);
    }
}

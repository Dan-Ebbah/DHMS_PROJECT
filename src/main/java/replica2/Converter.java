package replica2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class Converter {
    public static String TypeToString(Appointment.AppointmentType type){
        return type.toString();
    }

    public static Appointment.AppointmentType StringToType(String str){
        return Appointment.AppointmentType.valueOf(str);
    }

    public static ArrayList<Appointment> ByteArrayToArrayList(byte[] bytes) throws IOException {
        ArrayList<Appointment> appointments = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream in = new DataInputStream(bais);
        while (in.available() > 0) {
            int capacity = Integer.parseInt(in.readUTF());
            String ID = in.readUTF();
            int clients = Integer.parseInt(in.readUTF());
            ArrayList<String> users = new ArrayList<>();
            for(int i=0;i<clients;i++){
                users.add(in.readUTF());
            }
            Appointment.AppointmentType type = Appointment.AppointmentType.valueOf(in.readUTF());
            Date date = new Date(Long.parseLong(in.readUTF()));
            appointments.add(new Appointment(capacity,ID,users,type,date));
        }
        return appointments;
    }
    public static byte[] ArrayListToByteArray(ArrayList<Appointment> appointments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (Appointment a : appointments) {
            for(String s: a.toStringArray()){
                out.writeUTF(s);
            }
        }
        return baos.toByteArray();
    }
}

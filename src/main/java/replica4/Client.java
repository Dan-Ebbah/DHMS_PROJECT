package replica4;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
public class Client {

    static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy-hh-mm-ss");
    static Date D = new Date();
    static String strDate = sdf.format(D);
    static String[] args;

    public static String adminID;


    public static void main(String[] args)throws MalformedURLException {
        Client.args = args;
        String n = "\n***************WELCOME TO DISTRIBUTED HEALTH CARE SYSTEM****************";
        System.out.println(n);
        while (true) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter ID:");
            adminID = sc.nextLine();
            if (adminID.charAt(3) == 'A') {

                System.out.println("\nEnter input 1 (or) 2 (or) 3 (or) 4 (or) 5 (or) 6 (or) 7 :\n1:Addappointment\n2:Removeappointment\n3:listappointmentavailbilty\n4:BookAppointment\n5:CancelAppointment\n6:GetAppointmentSchedule\n7:swapAppointment\n");
                String input = sc.nextLine();
                switch (input) {
                    case "1":
                        Addappointment(sc);
                        break;
                    case "2":
                        Removeappointment(sc);
                        break;
                    case "3":
                        listappointmentavailbilty(sc);
                        break;
                    case "4":
                        BookAppointment(sc);
                        break;
                    case "5":
                        CancelAppointment(sc);
                        break;
                    case "6":
                        GetAppointmentSchedule(sc);
                        break;
                    case "7":
                        swapAppointment(sc);
                        break;
                    default:
                        System.out.println("-------INVALID INPUT-------");
                        String status = "---Invalid Input---";
                        String add = "Console";
                        admin_log(add, status);
                        server_log(add, status);
                        break;
                }


            } else if (adminID.charAt(3) == 'P') {

                System.out.println("\nEnter input 1 (or) 2 (or) 3 :\n1:BookAppointment\n2:CancelAppointment\n3:GetAppointmentSchedule\n4:swapAppointment\n");
                String input = sc.nextLine();
                switch (input) {
                    case "1":
                        BookAppointment(sc);
                        break;
                    case "2":
                        CancelAppointment(sc);
                        break;
                    case "3":
                        GetAppointmentSchedule(sc);
                        break;
                    case "4":
                        swapAppointment(sc);
                        break;
                    default:
                        System.out.println("-------INVALID INPUT-------");
                        String status = "---Invalid Input---";
                        String add = "Console";
                        admin_log(add, status);
                        server_log(add, status);
                        break;
                }


            } else {
                System.out.println("-------INVALID INPUT-------");
                String status = "---Invalid Input---";
                String add = "Console";
                admin_log(add, status);
                server_log(add, status);
            }
        }
    }


    public static void Addappointment(Scanner sc) throws NullPointerException, MalformedURLException {


        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            System.out.println("enter appointment type-->");
            String type = sc.nextLine();


            System.out.println("enter appointment id -->");
            String id = sc.nextLine();

            System.out.println("enter capacity-->");
            int cap = sc.nextInt();
            String city = adminID.substring(0, 3);

            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":
                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);

                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            try {
                S.addAppointment(id, type, cap);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "add_appointment";
            admin_log(add, status);
            server_log(add, status);
        }
    }


    public static void Removeappointment(Scanner sc) throws MalformedURLException {


        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {

            System.out.println("enter appointment type-->");
            String type = sc.nextLine();
            System.out.println("enter appointment id-->");
            String id = sc.nextLine();

            String city = adminID.substring(0, 3);


            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":
                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            try {
                S.removeAppointment(id, type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {

            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "remove_appointment";
            admin_log(add, status);
            server_log(add, status);


        }


    }

    public static void listappointmentavailbilty(Scanner sc) throws MalformedURLException {


        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            String city = adminID.substring(0, 3);
            System.out.println("enter appointment type-->");
            String type = sc.nextLine();


            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":
                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            try {
                assert S != null;
                String list = S.listAppointmentAvailability(type);
                String status = "successfully completed";
                String add = "List_of_Appointments";
                admin_log1(add, status, list);
                server_log1(add, status, list);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {

            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "list_appointment_availabilty";
            admin_log(add, status);
            server_log(add, status);
        }

    }


    public static void BookAppointment(Scanner sc) throws MalformedURLException {
        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            String city = adminID.substring(0, 3);

            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":
                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);

                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            System.out.println("enter appointment type:");
            String appointment_type = sc.nextLine();

            System.out.println("enter appointment Id:");
            String appointment_Id = sc.nextLine();

            String patientId = "";
            if (adminID.charAt(3) == 'A') {
                System.out.println("enter patient ID:");
                patientId = sc.nextLine();
            }

            try {

                if (adminID.charAt(3) == 'A') {
                    String result = S.bookAppointment(patientId, appointment_type, appointment_Id);
                    String status = result.equals("fail") ? "failure" : "successful";
                    String add = result.equals("fail") ? "Appointment_Booking_Failed" : "Appointment_Booked";
                    admin_log1(add, status, appointment_Id);
                    server_log1(add, status, appointment_Id);
                } else if (adminID.charAt(3) == 'P') {
                    String result = S.bookAppointment(adminID, appointment_type, appointment_Id);
                    String status = result.equals("fail") ? "failure" : "successful";
                    String add = result.equals("fail") ? "Appointment_Booking_Failed" : "Appointment_Booked";
                    patient_log1(add, status, appointment_Id);
                    server_log1(add, status, appointment_Id);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "book_appointment_";
            admin_log(add, status);
            server_log(add, status);
        }

    }


    public static void CancelAppointment(Scanner sc) throws MalformedURLException {


        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            String city = adminID.substring(0, 3);
            System.out.println("enter appointment Id to be cancel-->");
            String cancel = sc.nextLine();
            String patientId = "";
            if (adminID.charAt(3) == 'A') {
                System.out.println("enter patient ID:");
                patientId = sc.nextLine();
            }



            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":
                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);

                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            try {


                if (adminID.charAt(3) == 'A') {

                    String result = S.cancelAppointment(patientId, cancel);
                    String status = result.equals("successful") ? "successful" : "failure";
                    String add = "Appointment_Canceled";
                    server_log1(add, status, cancel);
                    admin_log1(add, status, cancel);

                } else if (adminID.charAt(3) == 'P') {

                    String result = S.cancelAppointment(adminID, cancel);
                    String status = result.equals("successful") ? "successful" : "failure";
                    String add = "Appointment_Canceled";
                    patient_log1(add, status, cancel);
                    server_log1(add, status, cancel);
                }


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "cancel_appointment_";
            admin_log(add, status);
            server_log(add, status);
        }


    }


    public static void GetAppointmentSchedule(Scanner sc) throws MalformedURLException {
        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            String city = adminID.substring(0, 3);

            switch (city) {
                case "MTL": {

                    Interface S;
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    String patientId = "";
                    String mtlappointment = "";
                    if (adminID.charAt(3) == 'A') {
                        System.out.println("enter patient ID:");
                        patientId = sc.nextLine();
                        mtlappointment = S.getAppointmentSchedule(patientId);
                    } else {
                        mtlappointment = S.getAppointmentSchedule(adminID);
                    }
                    if (!mtlappointment.isEmpty()) {

                        String status = "successful";
                        String add = "Appointment_Schedule_Montreal";


                        if (adminID.charAt(3) == 'A') {
                            admin_log1(add, status, mtlappointment);
                            server_log1(add, status, mtlappointment);
                        } else if (adminID.charAt(3) == 'P') {
                            patient_log1(add, status, mtlappointment);
                            server_log1(add, status, mtlappointment);
                        }

                    }
                    break;
                }
                case "QUE": {


                    Interface S;
                    URL url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    String patientId = "";
                    String qhappointment = "";
                    if (adminID.charAt(3) == 'A') {
                        System.out.println("enter patient ID:");
                        patientId = sc.nextLine();
                        qhappointment = S.getAppointmentSchedule(patientId);
                    } else {
                        qhappointment = S.getAppointmentSchedule(adminID);
                    }

                    if (!qhappointment.isEmpty()) {

                        String status = "successful";
                        String add = "Appointment_Schedule_Quebec";


                        if (adminID.charAt(3) == 'A') {
                            admin_log1(add, status, qhappointment);
                        } else if (adminID.charAt(3) == 'P') {
                            patient_log1(add, status, qhappointment);
                        }
                        server_log1(add, status, qhappointment);

                    }
                    break;
                }
                case "SHE": {

                    Interface S;
                    URL url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    String patientId = "";
                    String sbhappointment = "";
                    if (adminID.charAt(3) == 'A') {
                        System.out.println("enter patient ID:");
                        patientId = sc.nextLine();
                        sbhappointment = S.getAppointmentSchedule(patientId);
                    } else {
                        sbhappointment = S.getAppointmentSchedule(adminID);
                    }


                    if (!sbhappointment.isEmpty()) {

                        String status = "successful";
                        String add = "Appointment_Schedule_Sherbrooke";

                        if (adminID.charAt(3) == 'A') {
                            admin_log1(add, status, sbhappointment);
                        } else if (adminID.charAt(3) == 'P') {
                            patient_log1(add, status, sbhappointment);
                        }
                        server_log1(add, status, sbhappointment);

                    }
                    break;
                }
            }
        } else {
            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "get_appointment_";
            admin_log(add, status);
            server_log(add, status);
        }
    }


    public static void admin_log(String add, String status) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("admin_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();

        }


    }

    public static void server_log(String add, String status) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("server_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    public static void admin_log1(String add, String status, String ID) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("admin_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---ID--->").append(ID).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();
        }


    }


    public static void server_log1(String add, String status, String ID) {

        try (BufferedWriter o = new BufferedWriter(new FileWriter("server_file.txt", true))) {

            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---ID--->").append(ID).append("---STATUS--->").append(status).append("\n");
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static void patient_log1(String add, String status, String ID) {
//
//        try (BufferedWriter o = new BufferedWriter(new FileWriter("patient_file.txt", true))) {
//
//            o.append("---TIME--->").append(strDate).append("---RESPONSE--->").append(add).append("---ID--->").append(ID).append("---STATUS--->").append(status).append("\n");
//        } catch (IOException e) {
//
//            e.printStackTrace();
//        }


    }


    public static void swapAppointment(Scanner sc) throws MalformedURLException {

        if ((adminID.startsWith("MTL") || adminID.startsWith("SHE") || adminID.startsWith("QUE")) && adminID.length() == 8) {
            String city = adminID.substring(0, 3);
            System.out.println("enter old appointment ID-->");
            String o_Id = sc.nextLine();
            System.out.println("enter old appointment type-->");
            String o_type = sc.nextLine();
            System.out.println("enter new appointment ID-->");
            String n_Id = sc.nextLine();
            System.out.println("enter new appointment type-->");
            String n_type = sc.nextLine();
            String patientId = "";
            if (adminID.charAt(3) == 'A') {
                System.out.println("enter patient ID:");
                patientId = sc.nextLine();
            }


            Interface S;
            switch (city) {
                case "MTL":
                    URL url = new URL("http://localhost:8081/montreal_server?wsdl");
                    QName qName = new QName("http://servers.webservice.example.com/", "montreal_serverService");
                    Service service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "QUE":

                    url = new URL("http://localhost:8081/Quebec_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Quebec_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                case "SHE":
                    url = new URL("http://localhost:8081/Sherbrooke_server?wsdl");
                    qName = new QName("http://servers.webservice.example.com/", "Sherbrooke_serverService");
                    service = Service.create(url, qName);
                    S = service.getPort(Interface.class);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid city code");
            }

            try {
                if (adminID.charAt(3) == 'A') {

                    String appointment = S.swapAppointment(patientId, o_type, o_Id, n_type, n_Id);
                    if(!appointment.equals("fail")) {

                        String status = "successful";
                        String add = "Appointments_are_swapped";
                        admin_log1(add, status, patientId);
                        server_log1(add, status, patientId);
                    }
                    else {
                        String status = "failure";
                        String add = "Appointments_are_not_swapped";
                        admin_log1(add, status, patientId);
                        server_log1(add, status, patientId);
                    }
                } else if (adminID.charAt(3) == 'P') {
                    String appointment = S.swapAppointment(adminID, o_type, o_Id, n_type, n_Id);
                    if(!appointment.equals("fail")) {
                        String status = "successful";
                        String add = "Appointments_are_swapped";
                        patient_log1(add, status, adminID);
                        server_log1(add, status, adminID);
                    }
                    else {
                        String status = "failure";
                        String add = "Appointments_are_not_swapped";
                        patient_log1(add, status, adminID);
                        server_log1(add, status, adminID);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {

            System.out.println("---Invalid AdminID---");
            String status = "---Invalid AdminID---";
            String add = "Swap_Appointment";
            admin_log(add, status);
            server_log(add, status);
        }

    }
}
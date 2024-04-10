package frontend;

public class ValidationHelper {

    public static boolean isValidUserID(String userId) {
        if (!(userId.startsWith("MTLA") || userId.startsWith("QUEA") || userId.startsWith("SHEA")
                || userId.startsWith("MTLP") || userId.startsWith("QUEP") || userId.startsWith("SHEP"))) {
            return false;
        }

        return userId.length() == 8 && userId.substring(4).matches("\\d+");
    }

    public static boolean isValidAppointmentID(String appointmentID) {
        if (!(appointmentID.startsWith("MTLM") || appointmentID.startsWith("QUEM") || appointmentID.startsWith("SHEM")
                || appointmentID.startsWith("MTLA") || appointmentID.startsWith("QUEA") || appointmentID.startsWith("SHEA")
                || appointmentID.startsWith("MTLE") || appointmentID.startsWith("QUEE") || appointmentID.startsWith("SHEE"))) {
            return false;
        }

        return appointmentID.length() == 10 && appointmentID.substring(4).matches("\\d+");
    }

    public static boolean isValidAppointmentID(String appointmentID, String adminID) {
        if (!(appointmentID.startsWith("MTLM") || appointmentID.startsWith("QUEM") || appointmentID.startsWith("SHEM")
                || appointmentID.startsWith("MTLA") || appointmentID.startsWith("QUEA") || appointmentID.startsWith("SHEA")
                || appointmentID.startsWith("MTLE") || appointmentID.startsWith("QUEE") || appointmentID.startsWith("SHEE"))) {
            return false;
        }

        if (!appointmentID.substring(0, 3).equals(adminID.substring(0, 3))) {
            return false;
        }

        return appointmentID.length() == 10 && appointmentID.substring(4).matches("\\d+");
    }

    public static boolean isValidAppointmentType(String appointmentType) {
        if (appointmentType == null) {
            return false;
        }

        return appointmentType.equals("Physician")
                || appointmentType.equals("Surgeon")
                || appointmentType.equals("Dental");
    }
}

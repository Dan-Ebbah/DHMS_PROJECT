package replica1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("ddMMyy");

    public static boolean isSameDate(String appointmentID1, String appointmentID2) {
        return appointmentID1.substring(4).equals(appointmentID2.substring(4));
    }

    public static boolean isSameWeek(String appointmentID1, String appointmentID2) {
        try {
            Date parsedDate1 = FORMATTER.parse(appointmentID1.substring(4));
            Date parsedDate2 = FORMATTER.parse(appointmentID2.substring(4));
            long diff = Math.abs(parsedDate1.getTime() - parsedDate2.getTime());
            return diff < 1000 * 60 * 60 * 24 * 7;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isLater(String currentID, String appointmentID) {
        if (currentID.substring(4).equals(appointmentID.substring(4))) {
            if ((currentID.charAt(3) == 'A' && appointmentID.charAt(3) == 'M')
                    || (currentID.charAt(3) == 'E' && appointmentID.charAt(3) == 'M')
                    || (currentID.charAt(3) == 'E' && appointmentID.charAt(3) == 'A')) {
                return true;
            }
        }

        try {
            Date parsedDateOfCurrentID = FORMATTER.parse(currentID.substring(4));
            Date parsedDateOfAppointmentID = FORMATTER.parse(appointmentID.substring(4));

            if (parsedDateOfCurrentID.getTime() - parsedDateOfAppointmentID.getTime() > 0L) {
                return true;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}

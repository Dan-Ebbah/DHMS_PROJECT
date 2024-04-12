package replica2;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

public class Logs {
    public static int num=0;
    public static void log(String logPath, String text){
        if(num>0){
            num--;
            return;
        }
        //write to the log file and prepend date to the information
        Date date = new Date();
        String output = (date.toString()+":\t" + text + "\n");
        System.out.println(output);
        try {
            Writer writer;
            writer = new FileWriter(logPath, true);
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

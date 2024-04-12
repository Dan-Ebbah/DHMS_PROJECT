package replica3.util;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerUtil {
    public static Logger getLogger(String className, String fileName) {
        Logger logger = Logger.getLogger(className);
        try {
            fileName = fileName.concat(".log");
            FileHandler fileHandler = new FileHandler(fileName, true);
            fileHandler.setFormatter(new MyFormatter());

            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return logger;
    }

    public static class MyFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder stringBuilder = new StringBuilder("[");
            stringBuilder
                    .append(new Date(record.getMillis()))
                    .append("] ")
                    .append(record.getMessage())
                    .append("\n");
            return stringBuilder.toString();
        }
    }
}

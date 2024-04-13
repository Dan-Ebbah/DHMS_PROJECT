package replica1.pojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class LogRecord {

    LocalDateTime requestDateTime;
    String requestType;
    String id;
    String status;
    String response;

    public LogRecord() {}

    public LogRecord(LocalDateTime requestDateTime, String requestType, String id, String status, String response) {

        this.requestDateTime = requestDateTime;
        this.requestType = requestType;
        this.id = id;
        this.status = status;
        this.response = response;
    }

    public void addToLogsFile() {

        try {
            Files.write(Paths.get("D:", "java_intellji", "DHMS_PROJECT", "src", "main", "java", "replica4", "log.txt"),
                    this.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        String s = "\n" + requestDateTime + ": " +
                "Type='" + requestType + '\'';

        if (id != null) {
            s = s + ", Id='" + id + '\'';
        }

        s = s + ", Status='" + status + '\'' +
                ", Response='" + response + '\'';

        return s;
    }
}

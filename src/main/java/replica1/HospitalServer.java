package replica1;

import javax.xml.ws.Endpoint;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class HospitalServer {

    public static void main(String[] args) {
        try {
            Endpoint mtlHospital = Endpoint.publish("http://localhost:8080/mtlHospital", new MTLHospital());
            Endpoint queHospital = Endpoint.publish("http://localhost:8080/queHospital", new QUEHospital());
            Endpoint sheHospital = Endpoint.publish("http://localhost:8080/sheHospital", new SHEHospital());

            File f = new File("C:/Users/shanm/IdeaProjects/DHMS_PROJECT/src/main/java/replica1/log.txt");
            if (!f.exists()) {
                f.createNewFile();
            } else {
                Files.write(Paths.get("C:", "Users", "shanm", "IdeaProjects", "DHMS_PROJECT", "src", "main", "java", "replica1", "log.txt"),
                        "\n-----------------------------------\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }

            System.out.println("MTL Hospital service is published: " + mtlHospital.isPublished());
            System.out.println("QUE Hospital service is published: " + queHospital.isPublished());
            System.out.println("SHE Hospital service is published: " + sheHospital.isPublished());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

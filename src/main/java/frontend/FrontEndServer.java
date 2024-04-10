package frontend;

import javax.xml.ws.Endpoint;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FrontEndServer {

    public static void main(String[] args) {
        Endpoint frontEndServiceEndpoint = Endpoint.publish("http://192.168.43.7:8080/frontend", new FrontEndImpl());

        System.out.println("FrontEnd service is published: " + frontEndServiceEndpoint.isPublished());
    }
}

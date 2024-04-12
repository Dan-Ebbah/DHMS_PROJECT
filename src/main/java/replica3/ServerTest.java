package replica3;


import javax.xml.ws.Endpoint;

public class ServerTest {
    public static void main(String[] args) {
        ServerObjectImpl server = new ServerObjectImpl();
        Endpoint publish = Endpoint.publish("http://localhost:8082/server/test", server);
        System.out.println(publish.isPublished());
    }
}

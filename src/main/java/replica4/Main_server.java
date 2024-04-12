
package replica4;

import replica4.servers.Quebec_server;
import replica4.servers.Sherbrooke_server;
import replica4.servers.montreal_server;

import javax.xml.ws.Endpoint;

public class Main_server {
    public static void main(String[] args) {
       try
       {

           Endpoint endpoint = Endpoint.publish("http://localhost:8081/montreal_server", new montreal_server());
           System.out.println("montreal service is published: " + endpoint.isPublished());

           // Create servant
           endpoint = Endpoint.publish("http://localhost:8081/Quebec_server", new Quebec_server());
           System.out.println("quebec service is published: " + endpoint.isPublished());

           endpoint = Endpoint.publish("http://localhost:8081/Sherbrooke_server", new Sherbrooke_server());
           System.out.println("sherbrooke service is published: " + endpoint.isPublished());


           // Wait for invocations from clients
           System.out.println("Server ready and waiting ...");


        } catch (Exception re) {
            System.out.println("Exception in main: " + re);
        }
    }




}

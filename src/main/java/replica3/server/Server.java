package replica3.server;

import replica3.database.MontrealHashMap;
import replica3.database.QuebecHashMap;
import replica3.database.SherbrookeHashMap;

import javax.xml.ws.Endpoint;

public class Server {
    public static void main(String[] args) {
        try {

            MontrealServerImpl montrealServer = new MontrealServerImpl(new MontrealHashMap());
            QuebecServerImpl quebecServer = new QuebecServerImpl(new QuebecHashMap());
            SherbrookeServerImpl sherbrookeServer = new SherbrookeServerImpl(new SherbrookeHashMap());
            ServerImpl[] servers = { montrealServer, quebecServer, sherbrookeServer};

            for (ServerImpl server : servers) {
                String url = "http://localhost:8082/server/" + server.getServerName().toLowerCase();
                System.out.println(url + " is running");
                Endpoint publish = Endpoint.publish(url, server);
            }

            System.out.println("All Servers are running ...");


        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }
}

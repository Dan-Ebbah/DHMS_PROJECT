package replicaManager;


import javax.xml.ws.Endpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Replica {
    private List<ServerObjectImpl> _serverObjects;

    public Replica() {
        ArrayList<ServerObjectImpl> serverObjects = new ArrayList<>();
        publishAndStartServerObjects();
    }

    public Replica(List<ServerObjectImpl> serverObjects) {
        _serverObjects = serverObjects;
        publishAndStartServerObjects();
    }

    public Replica(ConcurrentHashMap hashMap) {
        //  spins new server objects with this hasMap
    }

    public void publishAndStartServerObjects() {
        try {
            for (ServerObjectImpl server : _serverObjects) {
                String url = "http://localhost:8082/server/" + server.getServerName().toLowerCase();
                Endpoint publish = Endpoint.publish(url, server);
            }

            System.out.println("All Server Objects are running ...");


        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }

    public String getAllDataFromReplica()
    {
        StringBuilder result = new StringBuilder();
        for (ServerObjectImpl serverObject: _serverObjects) {
            String serverName = serverObject.getServerName();

            result.append(serverName);
            result.append("/");

            String serverObjectInfo = serverObject.getInfo();
            result.append(serverObjectInfo);
            result.append("\\"); //seprates hospitals
        }
        return result.toString();
    }
}

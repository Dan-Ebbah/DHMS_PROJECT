package replicaManager;

import replica3.MontrealServerObjectImpl;
import replica3.QuebecServerObjectImpl;
import replica3.ServerObjectImpl;
import replica3.SherbrookeServerObjectImpl;
import replica3.database.MontrealHashMap;
import replica3.database.QuebecHashMap;
import replica3.database.SherbrookeHashMap;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Replica {
    private List<replica3.ServerObjectImpl> _serverObjects;
    private final static QName QUEBEC_SERVER_IMPLSERVICE_QNAME = new QName("http://server/", "QuebecServerImplService");

    private final static QName SHERBROOKE_SERVER_IMPLSERVICE_QNAME = new QName("http://server/", "SherbrookeServerImplService");
    private final static QName MONTREAL_SERVER_IMPLSERVICE_QNAME = new QName("http://server/", "MontrealServerImplService");

    public Replica() {
        ArrayList<replica3.ServerObjectImpl> serverObjects = new ArrayList<>();
        try {
            serverObjects.add(new MontrealServerObjectImpl(new MontrealHashMap()));
            serverObjects.add(new QuebecServerObjectImpl(new QuebecHashMap()));
            serverObjects.add(new SherbrookeServerObjectImpl(new SherbrookeHashMap()));
        } catch (SocketException e) {
            System.out.println("Error in creating one of the server objects. Error = " + e.getMessage());
        }

        _serverObjects = serverObjects;
        publishAndStartServerObjects();
    }

    public Replica(List<replica3.ServerObjectImpl> serverObjects) {
        _serverObjects = serverObjects;
        publishAndStartServerObjects();
    }

    public Replica(ConcurrentHashMap hashMap) {
        //  spins new server objects with this hasMap
    }


    public void publishAndStartServerObjects() {
        try {
            for (replica3.ServerObjectImpl server : _serverObjects) {
                String url = "http://localhost:8082/server/" + server.getServerName().toLowerCase();
                System.out.println(server.getServerName() + " : " + url);
                Endpoint publish = Endpoint.publish(url, server);
            }

            System.out.println("All Server Objects are running ...");


        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }

    public QName[] getServerQnames() {
        return new QName[]{SHERBROOKE_SERVER_IMPLSERVICE_QNAME, MONTREAL_SERVER_IMPLSERVICE_QNAME, QUEBEC_SERVER_IMPLSERVICE_QNAME};
    }

    public Map<String, QName> getServerWebDetails() {
        HashMap<String, QName> serverDetailsMap = new HashMap<>();
        for (replica3.ServerObjectImpl serverObject: _serverObjects) {
            String serverName = serverObject.getServerName().toLowerCase();
            switch (serverName) {
                case "montreal":
                    serverDetailsMap.put("montreal", MONTREAL_SERVER_IMPLSERVICE_QNAME);
                case "sherbrooke":
                    serverDetailsMap.put("sherbrooke", SHERBROOKE_SERVER_IMPLSERVICE_QNAME);
                case "quebec":
                    serverDetailsMap.put("quebec", QUEBEC_SERVER_IMPLSERVICE_QNAME);
            }
        }
        return serverDetailsMap;
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

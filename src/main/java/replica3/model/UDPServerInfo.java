package replica3.model;

public class UDPServerInfo {
    private String serverAddress;
    private int port;

    public UDPServerInfo(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return port;
    }
}

package replica3.server;

import replica3.database.HashMapImpl;
import replica3.model.UDPServerInfo;
import replica3.util.LoggerUtil;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.SocketException;
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class SherbrookeServerImpl extends ServerImpl{
    public SherbrookeServerImpl() {
    }

    public SherbrookeServerImpl(HashMapImpl database) throws SocketException {
        super(database, 5053, LoggerUtil.getLogger(SherbrookeServerImpl.class.getName(), "Sherbrooke"));
    }

    @Override
    public String getServerName() {
        return "Sherbrooke";
    }

    @Override
    protected UDPServerInfo[] getOtherServersInfo() {
        UDPServerInfo sherbrookeServerAddress = new UDPServerInfo("QuebecServerAddress", 5051);
        UDPServerInfo quebecServerAddress = new UDPServerInfo("MontrealServerAddress", 5052);

        return new UDPServerInfo[]{sherbrookeServerAddress, quebecServerAddress};
    }
}

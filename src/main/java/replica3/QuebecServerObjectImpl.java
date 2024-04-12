package replica3;

import replica3.database.HashMapImpl;
import replica3.model.UDPServerInfo;
import replica3.util.LoggerUtil;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.SocketException;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class QuebecServerObjectImpl extends ServerObjectImpl {
    public QuebecServerObjectImpl() {
        super();
    }

    public QuebecServerObjectImpl(HashMapImpl database) throws SocketException {
        super(database, 5051, LoggerUtil.getLogger(QuebecServerObjectImpl.class.getName(), "QUE"));
    }

    @Override
    public String getServerName() {
        return "Quebec";
    }

    @Override
    protected UDPServerInfo[] getOtherServersInfo() {
        UDPServerInfo quebecServerAddress = new UDPServerInfo("MontrealServerAddress", 5052);
        UDPServerInfo sherbrookeServerAddress = new UDPServerInfo("SherbrookeServerAddress", 5053);

        return new UDPServerInfo[]{quebecServerAddress, sherbrookeServerAddress};
    }
}

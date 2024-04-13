package replica3.server;

import replica3.database.HashMapImpl;
import replica3.util.LoggerUtil;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.SocketException;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class MontrealServerImpl extends ServerImpl{
    public MontrealServerImpl() {
    }

    public MontrealServerImpl(HashMapImpl database) throws SocketException {
        super(database, 5052, LoggerUtil.getLogger(MontrealServerImpl.class.getName(), "Montreal"));
    }

    @Override
    public String getServerName() {
        return "Montreal";
    }
}

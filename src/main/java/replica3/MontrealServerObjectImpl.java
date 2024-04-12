package replica3;

import replica3.database.HashMapImpl;
import replica3.util.LoggerUtil;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.SocketException;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class MontrealServerObjectImpl extends ServerObjectImpl {
    public MontrealServerObjectImpl() {
    }

    public MontrealServerObjectImpl(HashMapImpl database) throws SocketException {
        super(database, 5052, LoggerUtil.getLogger(MontrealServerObjectImpl.class.getName(), "MTL"));
    }

    @Override
    public String getServerName() {
        return "Montreal";
    }
}

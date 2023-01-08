package atomic_broadcast;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import java.net.DatagramSocket;

public class Network {

    private static final Log log = LogFactory.getLog(Network.class.getName());

    public static int findFreePort() {
        int port = 0;
        try {
            DatagramSocket udp = new DatagramSocket(0);
            port = udp.getLocalPort();
            udp.close();
        } catch (Exception e) {
            log.error().appendLast("could not bind to a port");
        }
        return port;
    }
}

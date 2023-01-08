package atomic_broadcast.network;

import atomic_broadcast.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkTest {

    @Test
    public void testFindFreePort(){
        int freePort = Network.findFreePort();
        assertTrue(freePort > 0);
    }
}

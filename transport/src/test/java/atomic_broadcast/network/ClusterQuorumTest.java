package atomic_broadcast.network;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static atomic_broadcast.utils.ConsensusUtils.quorumThreshold;

public class ClusterQuorumTest {

    @Test
    public void testMajorityNeededForMemberCount() {
        long threshold = quorumThreshold(6);
        Assertions.assertEquals(4, threshold);

        threshold = quorumThreshold(5);
        Assertions.assertEquals(3, threshold);

        threshold = quorumThreshold(4);
        Assertions.assertEquals(3, threshold);

        threshold = quorumThreshold(3);
        Assertions.assertEquals(2, threshold);

        threshold = quorumThreshold(2);
        Assertions.assertEquals(2, threshold);

        threshold = quorumThreshold(1);
        Assertions.assertEquals(1, threshold);
    }
}

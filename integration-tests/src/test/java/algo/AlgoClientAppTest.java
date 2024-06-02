package algo;

import container.AlgoContainerMain;
import org.junit.jupiter.api.Disabled;

public class AlgoClientAppTest {


    @Disabled("disabled until test can assert waiting for aeron connection")
    public void clientAppStartUpTest() {
        AlgoContainerMain.start(new NoOpAlgoFactory());
    }
}

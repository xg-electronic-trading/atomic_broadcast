package algo;

import com.messages.sbe.StrategyEnum;

public class NoOpAlgoFactory implements AlgoFactory {

    @Override
    public AlgoCode createAlgo(StrategyEnum strategy) {
        return new NoOpAlgo();
    }

}

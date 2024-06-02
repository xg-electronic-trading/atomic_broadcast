package algo;

import com.messages.sbe.StrategyEnum;

public interface AlgoFactory {

    AlgoCode createAlgo(StrategyEnum strategy);
}



package container;

import orderstate.StateCache;
import subscriptions.MarketDataPoller;

public class AlgoMainLoop implements Container {


    private final MarketDataPoller marketData;
    private final StateCache osCache;

    public AlgoMainLoop(
            MarketDataPoller marketData,
            StateCache osCache) {
        this.marketData = marketData;
        this.osCache = osCache;
    }


    /*
     * design event handler (e.g. algo container) such that:
     * 1) poll event bus, receive event and translate to state object with EventType
     * 2) poll market data + signals, add to list of order ids to trigger
     * 3) trigger algo callback
     * 4) send commands
     * 5) track command response
     *
     */

    @Override
    public void executeCalcCycle() {
        // 1. poll events

        // 2. poll market data + market data derived signals
        marketData.poll();

        // 3. poll clock service

        // 4. trigger algos
        runAlgosWithContext();

    }

    private void runAlgosWithContext() {

    }
}

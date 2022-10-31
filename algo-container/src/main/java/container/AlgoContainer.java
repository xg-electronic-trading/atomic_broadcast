package container;

import orderstate.ByteBufferOrderStateCache;
import subscriptions.MarketDataPoller;

public class AlgoContainer implements Container {

    private final MarketDataPoller marketData;
    private final ByteBufferOrderStateCache osCache;

    public AlgoContainer(MarketDataPoller marketData, ByteBufferOrderStateCache osCache) {
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
        // 1. poll market data
        marketData.poll();

        // 2. poll clock service

        // 3. trigger algos

    }
}

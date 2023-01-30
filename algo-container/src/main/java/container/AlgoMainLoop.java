package container;

import atomic_broadcast.utils.Pollable;
import orderstate.StateCache;
import subscriptions.MarketDataPoller;

public class AlgoMainLoop implements Pollable {

    private final Pollable events;
    private final Pollable marketData;

    public AlgoMainLoop(
            Pollable events,
            Pollable marketData) {
        this.events = events;
        this.marketData = marketData;
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
    public void poll() {
        // 1. poll events
        events.poll();

        // 2. poll market data + market data derived signals
        marketData.poll();

        // 3. poll clock service

        // 4. trigger algos
        runAlgosWithContext();
    }

    private void runAlgosWithContext() {

    }
}

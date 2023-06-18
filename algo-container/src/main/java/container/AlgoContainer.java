package container;

import atomic_broadcast.utils.Pollable;
import subscriptions.MarketDataPoller;

public class AlgoContainer implements Pollable {

    private final Pollable events;
    private final MarketDataPoller marketData;
    private final Pollable publisher;

    public AlgoContainer(
            Pollable publisher,
            Pollable events,
            MarketDataPoller marketData) {
        this.events = events;
        this.marketData = marketData;
        this.publisher = publisher;
    }


    /**
     * This class is designed such that it contains everything to run on a single thread.
     * A single instance of this class should have:
     *
     * 1) its own state cache (in memory or offheap)
     * 2) its own event listener (either via its own subscription to a
     *    multicast topic or by reading events from its own ringbuffer)
     * 3) its own market data client
     * 4) its own clock service
     * 3) its own command processor (i.e. its own publication to a global
     *    sequencer, complete with its own cmd validator)
     *
     * The above allows running a container on the main thread or multiple worker
     * threads within a java process without having to worry about any cross thread
     * communicaton.
     *
     * A single invocation of the poll method of this class will:
     *
     * 1) poll event bus, receive event and translate to state object with EventType
     * 2) poll market data + signals, add to list of order ids to trigger
     * 3) trigger algo callback
     * 4) send commands
     * 5) track command response
     *
     */

    @Override
    public void poll() {
        //1. poll publisher
        publisher.poll();

        // 2. poll events
        events.poll();

        // 3. poll market data + market data derived signals
        marketData.poll();

        // 4. poll signals provider


        // 5. poll clock service

    }
}

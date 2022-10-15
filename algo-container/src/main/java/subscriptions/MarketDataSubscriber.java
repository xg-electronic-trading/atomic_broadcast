package subscriptions;

public interface MarketDataSubscriber {

    /**
     * subscribe to market data by symbol.
     * Symbol can be ric, bbid or some other unique identifier according
     * to the market data provider.
     */

    boolean addSubscription(long orderId, CharSequence symbol);
}

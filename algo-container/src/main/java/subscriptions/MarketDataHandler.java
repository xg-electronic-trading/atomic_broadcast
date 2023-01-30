package subscriptions;

public interface MarketDataHandler {
    public void onMarketDataTick(MarketDataSnapshot marketData);
}

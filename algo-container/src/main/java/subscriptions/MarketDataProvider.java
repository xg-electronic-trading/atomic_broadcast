package subscriptions;

public interface MarketDataProvider {

    MarketDataSnapshot snapMarketData(CharSequence symbol);
}

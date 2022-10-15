package subscriptions;

public interface MarketDataSnapshot {

    long mdSeqNo();

    long bestBid();

    long bestOffer();

}

package subscriptions;

import org.agrona.collections.LongHashSet;
import org.agrona.collections.Object2ObjectHashMap;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public class MarketDataService implements MarketDataProvider, MarketDataSubscriber, MarketDataPoller {

    private final int INITIAL_SYM_CAPACITY = 5000;
    private final int INITIAL_ORDER_CAPACITY = 50_000;

    private final Map<CharSequence, AbstractSet<Long>> symToIds = new Object2ObjectHashMap<>(INITIAL_ORDER_CAPACITY, 0.55F, true);
    private final Supplier<AbstractSet<Long>> longSetSupplier = () -> new LongHashSet(300, 0.55f, true);
    public MarketDataService() {
    }


    @Override
    public MarketDataSnapshot snapMarketData(CharSequence symbol) {
        return null;
    }

    @Override
    public boolean addSubscription(long orderId, CharSequence symbol) {
        if (symToIds.containsKey(symbol)) {
            AbstractSet<Long> idSet = symToIds.get(symbol);
            idSet.add(orderId);
        } else {
            AbstractSet<Long> idSet = longSetSupplier.get();
            idSet.add(orderId);
            symToIds.put(symbol, idSet);
        }
        return true;
    }

    @Override
    public boolean poll() {
        Iterator<Map.Entry<CharSequence, AbstractSet<Long>>> itr = symToIds.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry<CharSequence, AbstractSet<Long>> entry = itr.next();

            /**
             * check market data seq no. has changed
             */

            boolean hasMdTicked = checkMdSeqNoChange(entry.getKey());

            if (hasMdTicked) {
                AbstractSet<Long> idSet = entry.getValue();
                Iterator<Long> idItr = idSet.iterator();
                while (idItr.hasNext()) {
                    long orderId = idItr.next();
                    //orderTriggerSetter.getOrderTrigger(orderId).markOrderToRun();
                }

                return true;
            }
        }

        return false;
    }

    private boolean checkMdSeqNoChange(CharSequence symbol) {
        return true;
    }
}

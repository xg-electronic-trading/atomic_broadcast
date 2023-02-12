package orderstate;

import org.agrona.collections.Long2ObjectHashMap;
import pool.ObjectPool;

public class MapOrderStateCache implements StateCache {

    private final Long2ObjectHashMap<MutableOrderState> map = new Long2ObjectHashMap<>(20_000_000, 0.55f, true);
    private final ObjectPool<MutableOrderState> pool;

    public MapOrderStateCache(ObjectPool<MutableOrderState> pool) {
        this.pool = pool;
    }

    @Override
    public MutableOrderState orderState(long id) {
        MutableOrderState os;
        if (map.containsKey(id)) {
            os = map.get(id);
        } else {
            os = pool.construct();
            os.orderId = id;
        }
        return os;
    }

    public void commitState(MutableOrderState state) {
        map.put(state.orderId, state);
    }

    @Override
    public int maxOrders() {
        return map.capacity();
    }

    @Override
    public void close() {
        map.clear();
    }
}

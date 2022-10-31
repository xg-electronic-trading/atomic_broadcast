package orderstate;

import org.agrona.collections.Long2ObjectHashMap;
import pool.ObjectPool;

public class MapOrderStateCache implements StateCache {

    private final Long2ObjectHashMap<MutableOrderState> map = new Long2ObjectHashMap<>(4_000_000, 0.55f, true);
    private final ObjectPool<MutableOrderState> pool;

    public MapOrderStateCache(ObjectPool<MutableOrderState> pool) {
        this.pool = pool;
    }

    @Override
    public MutableOrderState orderState(long id) {
        if(map.containsKey(id)) {
            return map.get(id);
        } else {
            MutableOrderState state = pool.construct();
            state.orderId = id;
            return state;
        }
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

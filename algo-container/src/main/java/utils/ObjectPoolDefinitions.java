package utils;

import container.MutableOrderState;

public class ObjectPoolDefinitions {

    /**
     * There should be only one state object required per strategy processing on a single thread,
     * however we have given the pool a size of 10 to cater for extra objects being checked out when
     * traversing a order hierarchy
     */
    public final ObjectPool<MutableOrderState> orderStateObjectPool = new ObjectPool<>(10, MutableOrderState::new);
}

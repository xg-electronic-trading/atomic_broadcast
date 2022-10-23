package pool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * ArrayList backed object pool.
 * ObjectPool is instantiated with pooled object of type T.
 * A supplier of type T is provided to create new instances of T to be stored in the pool.
 */

public class ObjectPool<T extends Pooled> {

    private final Supplier<T> newInstance;
    private final List<T> pool;
    private final int initialSize;

    public ObjectPool(int initialSze, Supplier<T> newInstance) {
        this.newInstance = newInstance;
        this.initialSize = initialSze;
        this.pool = new ArrayList<>(initialSze);
        initialisePool();
    }

    private void initialisePool() {
        int i = 0;
        while(i < initialSize) {
            pool.add(newInstance.get());
            i++;
        }
    }

    public int size() {
        return pool.size();
    }

    public T construct() {
        T obj;
        if(pool.size() > 0) {
            obj = pool.remove(pool.size() -1);
        } else {
            obj = newInstance.get();
        }

        obj.construct(this);

        return obj;
    }

    public void destruct(T obj) {
        obj.reset();
        pool.add(obj);
    }

}

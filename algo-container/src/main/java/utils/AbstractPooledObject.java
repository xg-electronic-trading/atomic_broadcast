package utils;

public abstract class AbstractPooledObject implements Pooled {

    private ObjectPool pool;

    @Override
    public final void construct(ObjectPool pool) {
        this.pool = pool;
    }

    @Override
    public final void destruct() {
        pool.destruct(this);
    }





}

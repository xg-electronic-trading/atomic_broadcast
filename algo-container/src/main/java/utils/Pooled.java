package utils;

public interface Pooled {

    void construct(ObjectPool pool);

    void destruct();

    void reset();

}

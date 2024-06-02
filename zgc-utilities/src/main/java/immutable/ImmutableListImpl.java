package immutable;

import reusable.Resettable;

import java.util.List;

public class ImmutableListImpl<E> implements ImmutableList<E>, Resettable {

    private List<E> list;

    public ImmutableListImpl() {
        reset();
    }

    public ImmutableList<E> set(List<E> list) {
        this.list = list;
        return this;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(E object) {
        return list.contains(object);
    }

    @Override
    public void reset() {
        list = null;
    }
}

package immutable;

public interface ImmutableList<E> {

    int size();

    E get(int index);

    boolean isEmpty();

    boolean contains(E object);

}

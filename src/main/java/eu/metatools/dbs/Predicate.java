package eu.metatools.dbs;

public interface Predicate<T> {
    boolean test(T item);
}

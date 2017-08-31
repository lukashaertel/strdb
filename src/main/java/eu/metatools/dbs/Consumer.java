package eu.metatools.dbs;

public interface Consumer<T> {
     void apply(T item);
}

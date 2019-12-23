package org.vatplanner.importer.postgis.status;

@FunctionalInterface
public interface ExceptionalTriConsumer<T, U, V> {

    void accept(T value1, U value2, V value3) throws Exception;
}

package org.vatplanner.importer.postgis.status;

@FunctionalInterface
public interface ExceptionalBiConsumer<T, U> {

    void accept(T value1, U value2) throws Exception;
}

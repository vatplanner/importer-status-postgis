package org.vatplanner.importer.postgis.status;

@FunctionalInterface
public interface ExceptionalConsumer<T> {

    void accept(T value) throws Exception;
}

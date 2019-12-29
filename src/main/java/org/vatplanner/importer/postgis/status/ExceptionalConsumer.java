package org.vatplanner.importer.postgis.status;

@FunctionalInterface
public interface ExceptionalConsumer<T, EX extends Exception> {

    void accept(T value) throws EX;
}

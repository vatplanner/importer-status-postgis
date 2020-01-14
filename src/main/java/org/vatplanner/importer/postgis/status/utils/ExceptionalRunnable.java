package org.vatplanner.importer.postgis.status.utils;

@FunctionalInterface
public interface ExceptionalRunnable<EX extends Exception> {

    void run() throws EX;
}

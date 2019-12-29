package org.vatplanner.importer.postgis.status;

@FunctionalInterface
public interface ExceptionalRunnable<EX extends Exception> {

    void run() throws EX;
}

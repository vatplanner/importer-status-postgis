package org.vatplanner.importer.postgis.status;

import java.sql.Connection;

/**
 * A {@link Runnable} database action allowing exceptions. Java default
 * functional interfaces do not permit exceptions to be thrown, so we require
 * our own interface; also we want to provide a database {@link Connection} to
 * the runnable.
 */
@FunctionalInterface
public interface DatabaseAction {

    void run(Connection db) throws Exception;
}

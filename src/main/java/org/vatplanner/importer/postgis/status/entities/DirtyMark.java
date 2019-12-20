package org.vatplanner.importer.postgis.status.entities;

/**
 * Implemented by relational entities supporting "dirty" state tracking. An
 * entity should mark itself as dirty whenever state is potentially changed
 * locally, so it is no longer guaranteed that it matches originally retrieved
 * state from on the database. Entities not fetched from database should be
 * marked dirty right from the start of their existence. Successfully persisting
 * data to or loading from database results in clean entities.
 */
public interface DirtyMark {

    /**
     * Marks the relational entity dirty.
     */
    public void markDirty();

    /**
     * Checks if the relational entity is currently dirty.
     *
     * @return true if dirty, false if clean
     */
    public boolean isDirty();
}
